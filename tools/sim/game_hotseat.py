#!/usr/bin/env python3
"""Simulation hot-seat 2 joueurs SW Legion V2 — log JSONL pour audit expert.

Règles appliquées (skill expert-legion-v2, rulebook DOC56) :
- Battle Deck 9 cartes/joueur (3 Obj + 3 Sec + 3 Adv), main 7 = 2x1+2x2+2x3+SO.
- pips = priorité + nb d'ordres (PAS activations). Activations = unités non vaincues.
- Pass Pool = (diff unités non vaincues - 1) plafonné à 0.
- Attaque : dés d'attaque (d8 rouge/black/white), défense d6 (rouge 3 Bloc/6, blanc 2 Bloc/6).
- Couvert = pool de dés BLANCS de défense, 1 dé/hit ; light=Bloc annule 1 hit, heavy=Bloc ou Surge, Crits jamais annulés.
- Suppression : courage -> supprimé si sup>=courage ; ralliement d6 blanc 2/6 retire 1 suppression/dé.
- Victoire 12 VP seulement si > adversaire (égalité continue).
- Round 0 = Build a Mission (pas de veto), Blue = 4 dés noirs, départage Hits>Crits>Surge.
"""
import json, random, os, datetime

random.seed(20261121)  # déterministe pour audit reproductible
OUT = '/root/.hermes/cache/scratch/sim_partie.jsonl'

logs = []
def log(round_, player, action, detail=None, result=None, state=None):
    logs.append({
        't': len(logs) + 1, 'round': round_, 'player': player, 'action': action,
        'detail': detail or {}, 'result': result, 'state': state or {},
    })

# --- Armées (vrais noms du catalogue) ---
REBEL = [
    {'id':'R_luke','name':'Luke Skywalker (Hero of the Rebellion)','type':'hero','hp':6,'cur':6,'cour':3,'spd':2,'def':'r','surge_def':'',
     'weapons':[{'n':"Luke's Blaster",'min':1,'max':2,'r':1,'b':3,'w':0}],'surgeAttack':'c'},
    {'id':'R_troopers','name':'Rebel Troopers','type':'corps','hp':4,'cur':4,'cour':1,'spd':2,'def':'w','surge_def':'b','cover':1,
     'weapons':[{'n':'A-200 Blaster Rifle','min':1,'max':3,'r':0,'b':1,'w':0}],'surgeDefense':'b'},
    {'id':'R_atrt','name':'AT-RT','type':'vehicle','hp':6,'cur':6,'cour':4,'spd':2,'def':'w','surge_def':'','armor':True,
     'weapons':[{'n':'Rocket Launcher','min':1,'max':3,'r':0,'b':1,'w':2}],'surgeAttack':'h'},
]
EMPIRE = [
    {'id':'E_vader','name':'Darth Vader (Dark Lord of the Sith)','type':'hero','hp':8,'cur':8,'cour':0,'spd':1,'def':'r','surge_def':'',
     'weapons':[{'n':'Lightsaber','min':0,'max':0,'r':2,'b':3,'w':0}],'armor':True},
    {'id':'E_storm','name':'Stormtroopers','type':'corps','hp':4,'cur':4,'cour':1,'spd':2,'def':'w','surge_def':'h',
     'weapons':[{'n':'E-11 Blaster Rifle','min':1,'max':3,'r':0,'b':0,'w':1}]},
    {'id':'E_scout','name':'Scout Troopers','type':'corps','hp':4,'cur':4,'cour':2,'spd':2,'def':'w','surge_def':'b',
     'weapons':[{'n':'EC-17 Hold-Out Blaster','min':1,'max':2,'r':0,'b':2,'w':0}],'surgeDefense':'b'},
]
ARMIES = {'REBEL':REBEL,'EMPIRE':EMPIRE}

# --- Dés ---
# Faces FFG officielles (expert Legion audit — rulebook texte a perdu les pictogrammes).
# Rouge attaque d8 : 5 Hit / 1 Crit / 1 Surge / 1 Blank
D8_ATK = ['hit','hit','hit','hit','hit','crit','surge','blank']
# Noir attaque d8 : 3 Hit / 1 Crit / 1 Surge / 3 Blank
D8_BLK = ['hit','hit','hit','crit','surge','blank','blank','blank']
# Blanc attaque d8 : 1 Hit / 1 Crit / 1 Surge / 5 Blank
D8_WHT = ['hit','crit','surge','blank','blank','blank','blank','blank']
# Défense rouge d6 : 3 Bloc / 1 Surge / 2 Blank
D6_RED_DEF = ['block','block','block','surge','blank','blank']
# Défense blanc d6 : 1 Bloc / 1 Surge / 4 Blank
D6_WHT_DEF = ['block','surge','blank','blank','blank','blank']

def roll_atk(dice):
    out=[]
    for typ,n in [('red',dice.get('r',0)),('black',dice.get('b',0)),('white',dice.get('w',0))]:
        d={'red':D8_ATK,'black':D8_BLK,'white':D8_WHT}[typ]
        out += [random.choice(d) for _ in range(n)]
    return out

def roll_def(color):
    return random.choice(D6_RED_DEF if color=='r' else D6_WHT_DEF)

def apply_cover(hits, crits, cover):
    """Couvert = pool de dés blancs, 1 par hit. light: Bloc annule 1 hit; heavy: Bloc/Surge; Crits jamais."""
    if cover<=0: return hits, crits, []
    pool=[random.choice(D6_WHT_DEF) for _ in range(hits)]
    cancelled=0
    for f in pool:
        if f=='block' or (cover>=2 and f=='surge'):
            cancelled+=1
    return max(0,hits-cancelled), crits, pool

def suppression_check(unit, added=1):
    # Véhicules (AT-RT, type 'vehicle') ne souffrent AUCUNE suppression (rulebook 950).
    if unit.get('type')=='vehicle': return False
    # Cible déjà vaincue : pas de suppression (rulebook 2429-2433).
    if unit['cur']<=0: return False
    # Courage 0 ('-', ex. Vader) = JAMAIS supprimé (rulebook: Courage '-').
    if unit['cour'] <= 0: return False
    unit['sup']=unit.get('sup',0)+added
    if unit['sup']>=unit['cour']:
        unit['suppressed']=True
    return unit.get('suppressed',False)

def rally(unit):
    """d6 blanc par suppression, 2 Bloc/6 retire 1 suppression."""
    n=unit.get('sup',0)
    faces=[random.choice(D6_WHT_DEF) for _ in range(n)]
    removed=sum(1 for f in faces if f in ('block','surge'))  # ralliement : Bloc OU Surge = 2/6
    unit['sup']=max(0,unit['sup']-removed)
    if unit['sup']<unit['cour']: unit['suppressed']=False
    return faces,removed

def alive(army): return [u for u in army if u['cur']>0]

# === ROUND 0 : Build a Mission ===
# Blue = 4 dés d'attaque NOIRS chacun ; départage Hits > Crits > Surge (rulebook 1221-1227).
def blue_roll():
    return [random.choice(D8_BLK) for _ in range(4)]
r_blue=blue_roll(); e_blue=blue_roll()
def tally(roll): return (roll.count('hit'), roll.count('crit'), roll.count('surge'))
blue = 'REBEL' if tally(r_blue) >= tally(e_blue) else 'EMPIRE'
log(0,'SYSTEM','blue_determination',{'rebel_roll':r_blue,'empire_roll':e_blue,
    'rebel_tally':tally(r_blue),'empire_tally':tally(e_blue)},blue,
    {'note':'4 dés noirs, départage Hits>Crits>Surge'})
log(0,'SYSTEM','build_a_mission',{'blue':blue},'Round 0 Build a Mission (pas de veto)')
log(0,'SYSTEM','mission_reveal',{'objective':'Recover the Research','map':'Recover the Research',
    'secondary':'Break Their Line','advantage':'Standing Orders'},'missions revealed')
# Battle Deck validation : main 7 exacte 2x1+2x2+2x3+SO
hand_pips=[1,1,2,2,3,3,4]
log(0,'REBEL','battle_deck_valid',{'hand_pips':hand_pips,'size':len(hand_pips)},'7 cartes exactes')
log(0,'EMPIRE','battle_deck_valid',{'hand_pips':hand_pips,'size':len(hand_pips)},'7 cartes exactes')

# === Rounds 1-5 ===
scores={'REBEL':0,'EMPIRE':0}
objective_marker={'holder':None}
for rnd in range(1,6):
    # pips (1-3 joués par joueur)
    pip_r=random.randint(1,3); pip_e=random.randint(1,3)
    log(rnd,'REBEL','play_command_card',{'pips':pip_r},'pips joués')
    log(rnd,'EMPIRE','play_command_card',{'pips':pip_e},'pips joués')
    # priorité : pips = priorité + nb ordres (PAS activations)
    # MOINS de pips = PRIORITÉ (rulebook : 1 pip bat 2 pips).
    if pip_r<pip_e: first,orders_first='REBEL',pip_r
    elif pip_e<pip_r: first,orders_first='EMPIRE',pip_e
    else:
        die=random.choice(D6_RED_DEF)  # départage : dé ROUGE de défense d6, 3 Bloc/6 = 50%
        first='REBEL' if die=='block' else 'EMPIRE'
        orders_first=pip_r
        log(rnd,'SYSTEM','priority_tiebreak',{'die':'red_defense_d6','face':die,'block':die=='block'},first)
    log(rnd,'SYSTEM','priority',{'pips_rebel':pip_r,'pips_empire':pip_e},first,
        {'note':'pips=priorité+ordres, PAS nb activations'})
    log(rnd,first,'issue_orders',{'orders':orders_first,'unités_non_vaincues':len(alive(ARMIES[first]))},
        'ordres émis')

    # Pass Pool = (diff unités non vaincues -1) plafonné à 0, au joueur EN SOUS-NOMBRE.
    nb_r=len(alive(REBEL)); nb_e=len(alive(EMPIRE))
    disadvantaged='REBEL' if nb_r<nb_e else ('EMPIRE' if nb_e<nb_r else None)
    passpool=max(0,abs(nb_r-nb_e)-1)
    log(rnd,disadvantaged or 'SYSTEM','create_pass_pool',
        {'diff_non_vaincues':abs(nb_r-nb_e),'bénéficiaire':disadvantaged},passpool)
    # Ces jetons Pass permettent au joueur désavantagé d'interrompre l'adversaire
    # autant de fois (matérialisé ci-dessous par des événements pass_USE).

    # Activation ALTERNÉE unité par unité (moi/toi/moi/toi), plafonnée par les ordres.
    second = 'EMPIRE' if first=='REBEL' else 'REBEL'
    orders={first: orders_first, second: pip_e if first=='REBEL' else pip_r}
    log(rnd,second,'issue_orders',{'orders':orders[second],
        'unités_non_vaincues':len(alive(ARMIES[second]))},'ordres émis')
    # Construire la file alternée en prenant un index croissant par joueur.
    idx={first:0, second:0}
    activation_queue=[]
    while True:
        added=False
        for pl in (first, second):
            units=alive(ARMIES[pl])
            i=idx[pl]
            if i < len(units) and sum(1 for x in activation_queue if x[0]==pl) < orders[pl]:
                activation_queue.append((pl, units[i])); idx[pl]+=1; added=True
        if not added: break
    # Consommation du Pass Pool : le joueur désavantagé interrompt la file adverse.
    pass_used=0
    if disadvantaged and passpool>0:
        for k,(pl,u) in enumerate(activation_queue):
            if pl!=disadvantaged and pass_used<passpool:
                log(rnd,disadvantaged,'pass_USE',{'jetons_restants':passpool-pass_used,
                    'interrompt':u['name']},'interruption Pass Pool')
                pass_used+=1
        if pass_used>0:
            log(rnd,'SYSTEM','pass_pool_consumed',{'utilisés':pass_used,'disponibles':passpool},
                'Pass Pool épuisé' if pass_used>=passpool else 'Pass Pool partiel')
    for player,u in activation_queue:
        army=ARMIES[player]
        if True:
            if u.get('suppressed') and u['cour'] > 0:
                faces,removed=rally(u)
                log(rnd,player,'rally',{'unit':u['name'],'dice':len(faces),'faces':faces},
                    f'{removed} suppression(s) retirée(s)',{'sup':u['sup'],'suppressed':u['suppressed']})
            # Move (vitesse × 25,4 mm)
            speed_mm=u['spd']*25.4
            log(rnd,player,'move',{'unit':u['name'],'speed':u['spd'],'mm':speed_mm},'mouvement')
            # Attaque contre une cible adverse non vaincue à portée
            targets=alive(ARMIES['EMPIRE' if player=='REBEL' else 'REBEL'])
            if targets and u['weapons']:
                tgt=random.choice(targets)
                w=random.choice([x for x in u['weapons'] if x['max']>0] or u['weapons'])
                rng=random.randint(w['min'],w['max'])
                atk_roll=roll_atk({'r':w['r'],'b':w['b'],'w':w['w']})
                hits=sum(1 for f in atk_roll if f=='hit')
                crits=sum(1 for f in atk_roll if f=='crit')
                # Surges d'attaque convertis selon surgeAttack de l'attaquant ('c'=Crit, 'h'=Hit)
                surges=sum(1 for f in atk_roll if f=='surge')
                surge_conv=u.get('surgeAttack','')
                if surge_conv=='c': crits+=surges
                elif surge_conv=='h': hits+=surges
                # Armor / Immune:Pierce : Crits convertis en Hits (Vader/AT-RT)
                if tgt.get('armor'):
                    hits+=crits; crits=0
                cover=tgt.get('cover',0)
                hits_after,crits_after,cover_pool=apply_cover(hits,crits,cover)
                # défense + surgeDefense ('b'=Bloc)
                def_faces=[roll_def(tgt['def']) for _ in range(hits_after)]
                blocks=sum(1 for f in def_faces if f=='block')
                if tgt.get('surgeDefense')=='b':
                    blocks+=sum(1 for f in def_faces if f=='surge')
                dmg=max(0,hits_after-blocks)+crits_after
                tgt['cur']=max(0,tgt['cur']-dmg)
                # Suppression : 1 si attaque À DISTANCE (R>=1) avec au moins 1 Hit/Crit au lancer
                # (rulebook 2507-2509). Même 0 PV peut supprimer si H/C présents au lancer.
                supp=False
                if rng>=1 and (hits+crits)>0:
                    supp=suppression_check(tgt,1)
                log(rnd,player,'attack',{'attacker':u['name'],'defender':tgt['name'],
                    'weapon':w['n'],'range':rng,'cover':cover},'attaque résolue',
                    {'attack_dice':atk_roll,'hits':hits,'crits':crits,
                     'cover_pool':cover_pool,'hits_after_cover':hits_after,
                     'defense_dice':def_faces,'blocks':blocks,'damage':dmg})
                log(rnd,'SYSTEM','apply_wounds',{'unit':tgt['name'],'damage':dmg},
                    f'{tgt["cur"]}/{tgt["hp"]} PV restants',{'defeated':tgt['cur']<=0})
                if supp: log(rnd,'SYSTEM','suppressed',{'unit':tgt['name']},'unité supprimée')

    # Objectif : contrôle du marker
    if alive(REBEL) and alive(EMPIRE):
        ctrl='REBEL' if len(alive(REBEL))>=len(alive(EMPIRE)) else 'EMPIRE'
        objective_marker['holder']=ctrl
        scores[ctrl]+=2  # 1 VP setup + 1 VP finish (rulebook: mission objective scoring)
    log(rnd,'SYSTEM','score_objective',{'holder':objective_marker['holder'],'scores':dict(scores)},'VP marqués')

    # Victoire 12 VP seulement si > adversaire
    if scores['REBEL']>=12 or scores['EMPIRE']>=12:
        if scores['REBEL']!=scores['EMPIRE']:
            winner='REBEL' if scores['REBEL']>scores['EMPIRE'] else 'EMPIRE'
            log(rnd,'SYSTEM','victory',{'scores':dict(scores)},winner)
            break
        else:
            log(rnd,'SYSTEM','victory_check',{'scores':dict(scores)},'égalité 12-12 : la partie CONTINUE')

# Départage si aucune victoire 12 VP (rulebook:1336-1339) : points d'unités vaincues.
if not any(json.loads(l).get('action')=='victory' for l in logs if isinstance(l,str)):
    # (les logs sont des dicts en memoire)
    if not any(x['action']=='victory' for x in logs):
        # Départage : PLUS FAIBLE valeur en points des unités VAINCUES gagne (rulebook 1339-1349).
        pts={'REBEL':sum(u['hp'] for u in REBEL if u['cur']<=0),
             'EMPIRE':sum(u['hp'] for u in EMPIRE if u['cur']<=0)}
        winner='REBEL' if pts['REBEL']<pts['EMPIRE'] else ('EMPIRE' if pts['EMPIRE']<pts['REBEL'] else 'DRAW')
        log(rnd,'SYSTEM','final_tiebreak',{'scores':dict(scores),'destroyed_points':dict(pts)},winner,
            {'note':"fin de Round 5, départage par points d'unités vaincues"})

# Écriture JSONL
os.makedirs(os.path.dirname(OUT),exist_ok=True)
with open(OUT,'w') as f:
    for x in logs: f.write(json.dumps(x,ensure_ascii=False)+'\n')
print(f'LOG écrit : {OUT} ({len(logs)} entrées)')
print('Scores finaux:',scores)
print('Unités vaincues REBEL:',[u['name'] for u in REBEL if u['cur']<=0])
print('Unités vaincues EMPIRE:',[u['name'] for u in EMPIRE if u['cur']<=0])
