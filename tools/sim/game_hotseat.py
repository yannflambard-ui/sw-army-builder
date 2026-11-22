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
from suppression_rules import is_panicked, is_suppressed, actions as suppression_actions, tokens_removed_on_panic, after_panic
import json, random, os, datetime

random.seed(20261121)  # déterministe pour audit reproductible
OUT = '/root/sw-army-builder/sim_logs/game_hotseat.jsonl'
NOTATION = '/root/sw-army-builder/sim_logs/game_hotseat.notation.txt'
SNAPSHOT = True  # état COMPLET (positions + PV + statuts) à chaque action

logs = []
def log(round_, player, action, detail=None, result=None, state=None):
    entry = {
        't': len(logs) + 1, 'round': round_, 'player': player, 'action': action,
        'detail': detail or {}, 'result': result, 'state': state or {},
    }
    if SNAPSHOT:
        entry['snapshot'] = snapshot()   # défini après ARMIES, disponible au 1er log (t=1 en Round 0)
    logs.append(entry)

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

# Stats REELLES depuis catalog.json (legionStats) : plus de valeurs codees en dur
# pour miniCount / des de defense / surges / keywords (audits precedents).
_CATALOG = json.load(open('/root/sw-army-builder/app/src/main/assets/catalog.json'))['cards']
_BY_NAME = {c['name']: c for c in _CATALOG if c.get('gameSystem') == 'LEGION_V2'}
for _army in (REBEL, EMPIRE):
    for _u in _army:
        _c = _BY_NAME.get(_u['name'])
        if not _c: continue
        try:
            _st = json.loads(_c['legionStats'])
        except Exception:
            continue
        _u['miniCount'] = _st.get('miniCount', _u.get('miniCount', 1))
        _u['health'] = _st.get('health', 1)
        _u['cour'] = _st.get('courage', _u.get('cour', 1))
        _u['spd'] = _st.get('speed', _u.get('spd', 1))
        _u['def'] = _st.get('defenseDie', _u.get('def', 'w'))
        _u['surgeAttack'] = _st.get('surgeAttack', '')
        _u['surgeDefense'] = _st.get('surgeDefense', '')
        _kws = [str(k).lower() for k in (_st.get('keywords') or [])]
        _u['armor'] = 'armor' in _kws   # Deflect != Armor : critiques JAMAIS convertis
        _u['deflect'] = 'deflect' in _kws

# --- Positions de déploiement (mm plateau 1828.8 x 914.4) ---
# REBEL en bas (y<0), EMPIRE en haut (y>0). Les unités avancent vers y=0.
for _side, _army in (('REBEL', REBEL), ('EMPIRE', EMPIRE)):
    _sign = -1 if _side == 'REBEL' else 1
    for _i, _u in enumerate(_army):
        _u['xMm'] = (-(len(_army) - 1) / 2 + _i) * 120.0
        _u['yMm'] = _sign * (50.0 + (_i % 2) * 20.0)
        _u.setdefault('sup', 0)
        _u.setdefault('suppressed', False)

def snapshot():
    """État COMPLETE de la partie à un instant t : chaque unité avec position,
    PV, statuts, courage, défense et effectif. Format 'replay d'échecs' : on peut
    reconstruire la partie en relisant les snapshots dans l'ordre."""
    out = {}
    for _side, _army in (('REBEL', REBEL), ('EMPIRE', EMPIRE)):
        out[_side] = [{
            'id': u['id'], 'name': u['name'], 'type': u.get('type', ''),
            'xMm': round(u.get('xMm', 0.0), 1), 'yMm': round(u.get('yMm', 0.0), 1),
            'cur': u['cur'], 'hp': u['hp'], 'sup': u.get('sup', 0),
            'suppressed': u.get('suppressed', False),
            'cour': u.get('cour', 0), 'def': u.get('def', ''),
            'miniCount': u.get('miniCount', 1),
        } for u in _army]
    return {'units': out, 'scores': dict(scores) if 'scores' in globals() else {'REBEL': 0, 'EMPIRE': 0}}

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
while tally(r_blue) == tally(e_blue):  # rulebook 1227 : relancer sur égalité exacte
    r_blue, e_blue = blue_roll(), blue_roll()
blue = 'REBEL' if tally(r_blue) > tally(e_blue) else 'EMPIRE'
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
        # rulebook 1415-1417 : c'est le joueur avec le MOINS d'ordres qui lance.
        roller = 'REBEL' if len(alive(REBEL)) >= len(alive(EMPIRE)) else 'EMPIRE'
        first = 'REBEL' if ((die=='block') == (roller=='REBEL')) else 'EMPIRE'
        orders_first=pip_r
        log(rnd,'SYSTEM','priority_tiebreak',{'die':'red_defense_d6','face':die,
            'block':die=='block','roller':roller,'rule':'moins d ordres lance (1415-1417)'},first)
    log(rnd,'SYSTEM','priority',{'pips_rebel':pip_r,'pips_empire':pip_e},first,
        {'note':'pips=priorité+ordres, PAS nb activations'})
    log(rnd,first,'issue_orders',{'orders':orders_first,'unités_non_vaincues':len(alive(ARMIES[first]))},
        'ordres émis')

    # Activation ALTERNÉE unité par unité (moi/toi/moi/toi), TOUTES unités non vaincues.
    second = 'EMPIRE' if first=='REBEL' else 'REBEL'
    orders={first: orders_first, second: pip_e if first=='REBEL' else pip_r}
    log(rnd,second,'issue_orders',{'orders':orders[second],
        'unités_non_vaincues':len(alive(ARMIES[second]))},'ordres émis')
    # Rulebook 1451-1463 : Issue Orders x2 -> ORDER POOL -> PASS POOL.
    log(rnd,'SYSTEM','create_order_pool',{'rebel':pip_r,'empire':pip_e,
        'total':pip_r+pip_e},'Order Pool créé après les deux Issue Orders')
    # Pass Pool = (diff unités non vaincues -1) plafonné à 0, au joueur EN SOUS-NOMBRE.
    nb_r=len(alive(REBEL)); nb_e=len(alive(EMPIRE))
    disadvantaged='REBEL' if nb_r<nb_e else ('EMPIRE' if nb_e<nb_r else None)
    passpool=max(0,abs(nb_r-nb_e)-1)
    log(rnd,disadvantaged or 'SYSTEM','create_pass_pool',
        {'diff_non_vaincues':abs(nb_r-nb_e),'bénéficiaire':disadvantaged},passpool)
    # Construire la file alternée en prenant un index croissant par joueur.
    idx={first:0, second:0}
    activation_queue=[]
    while True:
        added=False
        for pl in (first, second):
            units=alive(ARMIES[pl])
            i=idx[pl]
            # Rulebook 1453/1521-1523/2553 : TOUTES les unités non vaincues
            # s'activent. Les pips = priorité + nb d'ordres, PAS un plafond
            # d'activations (erreur d'audit précédente).
            if i < len(units):
                activation_queue.append((pl, units[i])); idx[pl]+=1; added=True
        if not added: break
    # Rulebook 1542-1546/1550-1552 : dépenser un jeton Pass = PASSER (fin de tour
    # de l'unité), PAS interrompre. On retire donc une activation de la file.
    pass_used=0
    if disadvantaged and passpool>0:
        kept=[]
        for pl,u in activation_queue:
            if pl!=disadvantaged and pass_used<passpool:
                log(rnd,disadvantaged,'pass_USE',{'jetons_restants':passpool-pass_used,
                    'unité_passée':u['name']},'jeton Pass dépensé : l unité adverse passe son tour')
                pass_used+=1
            else:
                kept.append((pl,u))
        activation_queue=kept
        if pass_used>0:
            log(rnd,'SYSTEM','pass_pool_consumed',{'utilisés':pass_used,'disponibles':passpool},
                'Pass Pool épuisé' if pass_used>=passpool else 'Pass Pool partiel')
    for player,u in activation_queue:
        army=ARMIES[player]
        # Une unité peut être vaincue APRES la construction de la file : elle
        # ne s'active PAS (rulebook : seules les unités non vaincues agissent).
        if u['cur'] <= 0:
            log(rnd,'SYSTEM','activation_skipped',{'unit':u['name'],'raison':'vaincue avant activation'},
                'unité vaincue : activation annulée')
            continue
        if True:
            # Rulebook 977/1550/1558-1560 : Supprimé = 1 action en moins ;
            # Paniqué = 0 action et retrait de Courage jetons.
            # Seuils depuis la source de vérité PARTAGÉE (rulebook 973-977).
            panicked = is_panicked(u['sup'], u['cour'])
            actions_left = suppression_actions(u['sup'], u['cour'])
            if panicked:
                before=u['sup']
                removed=tokens_removed_on_panic(before, u['cour'])
                u['sup']=after_panic(before, u['cour'])
                u['suppressed']=is_suppressed(u['sup'], u['cour'])
                log(rnd,player,'panicked',{'unit':u['name'],
                    'jetons_retirés':removed,'sup_initial':before,'sup_restant':u['sup'],
                    'courage':u['cour']},
                    f'Paniqué : 0 action, {removed} jeton(s) retiré(s)')
            if u.get('suppressed') and u['cour'] > 0:
                faces,removed=rally(u)
                log(rnd,player,'rally',{'unit':u['name'],'dice':len(faces),'faces':faces},
                    f'{removed} suppression(s) retirée(s)',{'sup':u['sup'],'suppressed':u['suppressed']})
            if actions_left <= 0:
                continue  # Paniqué : aucune action (jetons déjà retirés)
            # Supprimé = 1 action en moins : 1 move seulement, pas d'attaque.
            n_moves = 1 if actions_left < 2 else 2
            # Move (vitesse × 25,4 mm) — vrai déplacement vers l'adversaire.
            speed_mm=u['spd']*25.4
            fx, fy = u['xMm'], u['yMm']
            total_mm = float(n_moves) * (25.4 * u['spd'])
            # On s'arrête à distance 2 (50,8 mm) de l'ennemi le plus proche :
            # sinon les unités se croisent et sortent de portée (bug de simulation).
            foes_now=alive(ARMIES['EMPIRE' if player=='REBEL' else 'REBEL'])
            if foes_now:
                nearest=min(foes_now,key=lambda t:((u['xMm']-t['xMm'])**2+(u['yMm']-t['yMm'])**2)**0.5)
                dd=((u['xMm']-nearest['xMm'])**2+(u['yMm']-nearest['yMm'])**2)**0.5
                total_mm=max(0.0, min(total_mm, dd-50.8))
            u['yMm'] += (1 if player == 'REBEL' else -1) * total_mm
            log(rnd,player,'move',{'unit':u['name'],'speed':u['spd'],'mm':total_mm,
                'moves':n_moves,'from':[round(fx,1),round(fy,1)],
                'to':[round(u['xMm'],1),round(u['yMm'],1)]},
                f'{n_moves} moves · {total_mm:.1f} mm')
            # Attaque contre une cible adverse non vaincue à portée
            foes=alive(ARMIES['EMPIRE' if player=='REBEL' else 'REBEL'])
            if foes and u['weapons'] and actions_left >= 2:
                # Portée REELLE (rulebook 682-692) : distance au sol en pouces.
                # max=0 = corps à corps (distance < 1"). On ne tire que sur une
                # cible VALIDE, sinon on logge l'absence de cible à portée.
                legal=[]
                for t in foes:
                    dist=((u['xMm']-t['xMm'])**2 + (u['yMm']-t['yMm'])**2)**0.5 / 25.4
                    for wp in u['weapons']:
                        lo=max(0,wp['min']); hi=wp['max']
                        if (hi==0 and dist<1.0) or (hi>0 and lo<=dist<=hi):
                            legal.append((t,wp,dist)); break
                if not legal:
                    log(rnd,player,'no_target_in_range',{'unit':u['name'],
                        'ennemis':len(foes)},'aucune cible légale à portée')
                else:
                    tgt,w,rng=random.choice(legal)
                    atk_roll=roll_atk({'r':w['r'],'b':w['b'],'w':w['w']})
                    hits=sum(1 for f in atk_roll if f=='hit')
                    crits=sum(1 for f in atk_roll if f=='crit')
                    # Surges d'attaque convertis selon surgeAttack ('c'=Crit, 'h'=Hit)
                    surges=sum(1 for f in atk_roll if f=='surge')
                    surge_conv=u.get('surgeAttack','')
                    if surge_conv=='c': crits+=surges
                    elif surge_conv=='h': hits+=surges
                    # NO : Armor/Deflect n'annule PAS les critiques (rulebook 3187-3190 :
                    # « cancel up to X hits », Modify Attack Dice). Les critiques
                    # ne se transforment jamais en hits bloquables.
                    cover=tgt.get('cover',0)
                    # Rulebook 2259 : un défenseur Supprimé gagne +1 couvert.
                    if tgt.get('suppressed'): cover += 1
                    # Rulebook 3188-3190 : Armor X annule jusqu'à X Hits en
                    # Modify Attack Dice (AVANT le pool de défense). Crits intacts.
                    armor_x = 1 if tgt.get('armor') else 0
                    armor_removed=min(hits, armor_x)
                    hits -= armor_removed
                    hits_after,crits_after,cover_pool=apply_cover(hits,crits,cover)
                    def_faces=[roll_def(tgt['def']) for _ in range(hits_after)]
                    blocks=sum(1 for f in def_faces if f=='block')
                    # Deflect (Vader) : Surge = Bloc + 1 Wound à l'attaquant (3381-3389).
                    deflect_wounds=0
                    if tgt.get('deflect'):
                        for f in def_faces:
                            if f=='surge': blocks+=1; deflect_wounds+=1
                    elif tgt.get('surgeDefense')=='b':
                        blocks+=sum(1 for f in def_faces if f=='surge')
                    dmg=max(0,hits_after-blocks)+crits_after
                    tgt['cur']=max(0,tgt['cur']-dmg)
                    if deflect_wounds:
                        u['cur']=max(0,u['cur']-deflect_wounds)
                        log(rnd,player,'deflect',{'unit':tgt['name'],'surges':deflect_wounds},
                            f'{deflect_wounds} blessure(s) à l attaquant')
                    # Suppression : attaque À DISTANCE (R>=1) avec H/C au lancer.
                    supp=False
                    if rng>=1 and (hits+crits)>0:
                        supp=suppression_check(tgt,1)
                    log(rnd,player,'attack',{'attacker':u['name'],'defender':tgt['name'],
                        'weapon':w['n'],'range':rng,'cover':cover},'attaque résolue',
                        {'attack_dice':atk_roll,'hits':hits,'crits':crits,
                         'cover_pool':cover_pool,'hits_after_cover':hits_after,
                         'defense_dice':def_faces,'blocks':blocks,'damage':dmg,
                         'armor_removed':armor_removed,'cover':cover})
                    log(rnd,'SYSTEM','apply_wounds',{'unit':tgt['name'],'damage':dmg},
                        f'{tgt["cur"]}/{tgt["hp"]} PV restants',{'defeated':tgt['cur']<=0})
                    if supp: log(rnd,'SYSTEM','suppressed',{'unit':tgt['name']},'unité supprimée')


    # Fin de round : les tokens de suppression sont RETIRES (rulebook 1541-1547),
    # et aucune suppression ne survit à la défaite (2429-2433).
    for _army in (REBEL, EMPIRE):
        for _u in _army:
            # Rulebook 2612-2616 : remove 1 token, PAS tous.
            _u['sup']=max(0,_u['sup']-1)
            _u['suppressed'] = _u['sup']>0 and _u['cour']>0 and _u['sup']>=_u['cour']

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

# Fin de partie (rulebook 1332-1345) : victoire au plus de VP à l'issue du Round 5.
# Le départage par points d'unités vaincues ne s'applique QU'en cas d'égalité.
if not any(x['action']=='victory' for x in logs):
    if scores['REBEL'] != scores['EMPIRE']:
        w='REBEL' if scores['REBEL']>scores['EMPIRE'] else 'EMPIRE'
        log(5,'SYSTEM','victory',{'scores':dict(scores),
            'rule':'plus de VP en fin de Round 5 (1332-1345)'},w)
    else:
        # Départage : PLUS FAIBLE valeur en points des unités VAINCUES gagne.
        pts={'REBEL':sum(u['hp'] for u in REBEL if u['cur']<=0),
             'EMPIRE':sum(u['hp'] for u in EMPIRE if u['cur']<=0)}
        winner='REBEL' if pts['REBEL']<pts['EMPIRE'] else ('EMPIRE' if pts['EMPIRE']<pts['REBEL'] else 'DRAW')
        log(5,'SYSTEM','final_tiebreak',{'scores':dict(scores),'destroyed_points':dict(pts)},winner,
            {'note':"fin de Round 5, départage par points d'unités vaincues"})


# === Export : log JSONL complet (chaque entrée porte un snapshot) + notation ===
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT,'w') as f:
    for x in logs: f.write(json.dumps(x, ensure_ascii=False)+'\n')

def notation_line(x):
    t=x['t']
    d=x.get('detail') or {}
    if x['action']=='play_command_card': return f"{t:03d}. {x['player']} joue {d['pips']} pips"
    if x['action']=='issue_orders': return f"{t:03d}. {x['player']} donne {d['orders']} ordres"
    if x['action']=='move': return f"{t:03d}. {x['player']} {d['unit']} avance {d['mm']:.1f}mm"
    if x['action']=='attack': return f"{t:03d}. {x['player']} {d.get('attacker','?')} tire sur {d.get('defender','?')} -> {x['state']['damage']} degats"
    if x['action']=='suppressed': return f"{t:03d}. SUPPRIME {d['unit']}"
    if x['action'] in ('victory','final_tiebreak'): return f"{t:03d}. RESULTAT {x.get('result')} {d}"
    return f"{t:03d}. {x['action']} {x['player']} {str(d)[:400]} -> {x.get('result') or ''}"

with open(NOTATION,'w') as f:
    f.write('SW LEGION V2 · partie hot-seat REBEL vs EMPIRE · 5 rounds\n')
    f.write('Chaque coup est rejouable via le JSONL (cle snapshot).\n')
    for x in logs: f.write(notation_line(x)+'\n')

print(f"LOG : {OUT} ({len(logs)} entrées)")
print(f"NOTATION : {NOTATION}")
print(f"Scores finaux: {scores}")
print(f"Unités vaincues REBEL: {[u['name'] for u in REBEL if u['cur']<=0]}")
print(f"Unités vaincues EMPIRE: {[u['name'] for u in EMPIRE if u['cur']<=0]}")
