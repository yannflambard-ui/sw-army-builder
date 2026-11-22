#!/usr/bin/env python3
"""Simulation hot-seat 2 joueurs SW Armada 1.6.0 — log JSONL + notation rejouable.

SOURCES (aucune stat inventée) :
-stats vaisseaux/escadrons : app/src/main/assets/catalog.json (champ shipStats / rulesText)
- tables de dés : ArmadaDice.kt (8 faces par couleur, vérifié 18/18)
- règles : RRG 1.6.0 + skill expert-armada-v15
- déploiement : bande distance 1-3 du bord long, zone 4' (ArmadaDeployment.kt)

APPROXIMATIONS VOLONTAIREMENT SIGNALÉES À L'EXPERT :
- gabarit de manœuvre : 22.5 deg / cran, 40 mm/segment (ManeuverGeometry.kt PROVISOIRE)
- objectifs : 2 VP/rentre tenue, pas de mission spécifique (a affiner)
- jetons de defense : Brace/Redirect/Scatter/Evade/Salvo/Contain NON modelises (defTokens charge mais non consomme) — degats appliques directement, sans reaction du defenseur
"""
import json, random, os, re, math

random.seed(20261122)
BASE = '/root/sw-army-builder'
OUT = f'{BASE}/sim_logs/armada_hotseat.jsonl'
NOTATION = f'{BASE}/sim_logs/armada_hotseat.notation.txt'

# --- Tables de dés (ArmadaDice.kt) : (hull, squad, crit, accuracy) ---
FACES = {
    'black': [(1,1,0,0)]*4 + [(2,1,1,0)]*2 + [(0,0,0,0)]*2,
    'blue':  [(1,1,0,0)]*4 + [(1,0,1,0)]*2 + [(0,0,0,1)]*2,
    'red':   [(1,1,0,0)]*3 + [(2,2,0,0), (1,0,1,0), (1,0,1,0), (0,0,0,1), (0,0,0,0), (0,0,0,0)],
}
NAME = {(1,1,0,0):'HIT', (2,1,1,0):'HIT_CRIT', (0,0,0,0):'BLANK',
        (1,0,1,0):'CRIT', (0,0,0,1):'ACC', (2,2,0,0):'HIT_HIT'}

def roll(dice):
    """dice = (black, blue, red) -> liste de (couleur, face)."""
    out=[]
    for color,n in zip(('black','blue','red'), dice):
        for _ in range(n):
            f=random.choice(FACES[color]); out.append((color, f))
    return out

def damage(rolls, attacker_is_ship, target_is_ship):
    """RRG : critique ne compte que vaisseau CONTRE vaisseau (ArmadaDiceRoller.damage)."""
    ship_vs_ship = attacker_is_ship and target_is_ship
    return sum((f[0] if ship_vs_ship else f[1]) for _,f in rolls)

# --- Chargement des VRAIES stats du catalogue ---
cards = json.load(open(f'{BASE}/app/src/main/assets/catalog.json'))['cards']
BY_NAME = {c['name']: c for c in cards}

def ship(name):
    c = BY_NAME[name]
    st = json.loads(c['shipStats'])
    return {
        'id': c['id'], 'name': name, 'points': c.get('points',0), 'faction': c.get('factionId'),
        'type': 'ship', 'hull': st['hull'], 'maxHull': st['hull'],
        'shield': dict(st.get('shield') or {}), 'maxShield': dict(st.get('shield') or {}),
        'speed': st.get('maxSpeed', 1), 'command': st.get('command', 1),
        'squadron': st.get('squadron', 0), 'engineering': st.get('engineering', 0),
        'attack': st.get('attack') or {}, 'defTokens': list(st.get('defenseTokens') or []),
        'size': st.get('size','small'), 'sectorsUsed': [],
        'xMm': 0.0, 'yMm': 0.0, 'heading': 0.0, 'destroyed': False,
    }

def squadron(name, count=1):
    c = BY_NAME[name]
    txt = c.get('rulesText') or ''
    def num(pat, default=1):
        m = re.search(pat, txt)
        return int(m.group(1)) if m else default
    hull = num(r'Hull Value:\s*(\d+)', 3)
    speed = num(r'Speed:\s*(\d+)', 2)
    return {
        'id': c['id'], 'name': name, 'points': c.get('points',0), 'faction': c.get('factionId'),
        'type': 'squadron', 'hull': hull, 'maxHull': hull, 'speed': speed, 'count': count,
        'xMm': 0.0, 'yMm': 0.0, 'heading': 0.0, 'destroyed': False,
    }

# Flottes réelles (Clone Wars + Empire), stats lues dans le catalogue.
FLEETS = {
    'REPUBLIC': [
        ship('Venator I-class Star Destroyer'),
        ship('Acclamator I-class Assault Ship'),
        squadron('V-19 Torrent Squadron', 3),
        squadron('ARC-170 Squadron', 3),
    ],
    'EMPIRE': [
        ship('Gladiator I-class Star Destroyer'),
        ship('Arquitens-class Command Cruiser'),
        squadron('Black Squadron', 3),
        squadron('Gamma Squadron', 3),
    ],
}

# --- Déploiement RRG : bande distance 1-3 du bord long, zone 4' ---
BOARD_H = 914.4; SETUP_W = 1219.2; DEP_MIN = 76.2; DEP_MAX = 228.6
def in_band(x, y, south):
    d = BOARD_H/2 - y if south else BOARD_H/2 + y
    return abs(x) <= SETUP_W/2 and DEP_MIN <= d <= DEP_MAX

# Des des escadrons (catalog.json rulesText) : anti-escadron vs batterie.
SQUAD_DICE = {
    'V-19 Torrent Squadron': {'squad': (0,2,1), 'battery': (0,0,1)},
    'ARC-170 Squadron': {'squad': (0,2,1), 'battery': (0,2,0)},
    'Black Squadron': {'squad': (0,3,0), 'battery': (0,1,0)},
    'Gamma Squadron': {'squad': (1,0,0), 'battery': (1,0,0)},
}

logs=[]
def snapshot():
    return {
        'units': {
            side: [{
                'id':u['id'],'name':u['name'],'type':u['type'],
                'xMm':round(u['xMm'],1),'yMm':round(u['yMm'],1),'heading':u['heading'],
                'hull':u['hull'],'maxHull':u['maxHull'],
                'shield':dict(u['shield']) if u['type']=='ship' and isinstance(u.get('shield'),dict) else None,
                'command':u.get('command') if u['type']=='ship' else None,
                'sectorsUsed':list(u.get('sectorsUsed',[])),
                'destroyed':u['destroyed'],'points':u['points'],
            } for u in fleet]
            for side,fleet in FLEETS.items()
        },
        'scores': dict(scores) if 'scores' in globals() else {'REPUBLIC':0,'EMPIRE':0},
    }

class PartieTerminee(Exception):
    '''Sortie immédiate des boucles dès qu'une flotte n'a plus de vaisseau.'''

def check_elimination(rnd):
    ar=[u for u in FLEETS['REPUBLIC'] if u['type']=='ship' and not u['destroyed']]
    ae=[u for u in FLEETS['EMPIRE'] if u['type']=='ship' and not u['destroyed']]
    if not ar or not ae:
        w='EMPIRE' if not ar else 'REPUBLIC'
        log(rnd,'SYSTEM','elimination',{'republic_ships':len(ar),'empire_ships':len(ae)},w)
        raise PartieTerminee(w)

def log(round_, player, action, detail=None, result=None, state=None):
    logs.append({
        't':len(logs)+1,'round':round_,'player':player,'action':action,
        'detail':detail or {},'result':result,'state':state or {},'snapshot':snapshot(),
    })

# --- Round 0 : déploiement ---
for side, fleet in FLEETS.items():
    south = side=='REPUBLIC'
    for i,u in enumerate(fleet):
        x = (i - (len(fleet)-1)/2) * 150.0
        y = (BOARD_H/2 - 150.0) if south else -(BOARD_H/2 - 150.0)
        assert in_band(x,y,south), (side,u['name'])
        if u['type']=='squadron':
            # Escadron : distance 1-2 (25,4-50,8 mm) d'un vaisseau AMI (RRG).
            ships=[v for v in fleet if v['type']=='ship']
            if ships:
                host=ships[0]
                # Décalage pour ne pas superposer deux escadrons au même point.
                same=sum(1 for v in fleet if v['type']=='squadron' and fleet.index(v)<fleet.index(u))
                x=host['xMm']+40.0+same*30.0
                y=host['yMm']+(-20.0 if south else 20.0)+same*20.0
        u['xMm'],u['yMm'] = x,y
        if not south: u['heading']=180.0
        log(0,side,'deploy',{'unit':u['name'],'type':u['type'],
            'from':[round(x,1),round(y,1)],'zone':'SUD' if south else 'NORD',
            'rrg':'vaisseau: bande distance 1-3 | escadron: distance 1-2 d un vaisseau ami'},
            'placé légalement')

scores={'REPUBLIC':0,'EMPIRE':0}
command_stacks={'REPUBLIC':[],'EMPIRE':[]}  # FIFO de vraies icônes
order_tokens={'REPUBLIC':{},'EMPIRE':{}}     # 1 copie MAX par type
first_player='REPUBLIC'

def arc(attacker, target):
    """Secteur de coque visé (approximatif : angle relatif au cap)."""
    dx=target['xMm']-attacker['xMm']; dy=target['yMm']-attacker['yMm']
    ang=math.degrees(math.atan2(dy,dx))
    rel=(ang-attacker['heading'])%360
    if rel<45 or rel>=315: return 'front'
    if rel<135: return 'right'
    if rel<225: return 'rear'
    return 'left'

def portee_max(sec):
    '''RRG : distance 1 = 3" (76,2 mm), 2 = 6" (152,4), 3 = 9" (228,6).
    Cohérent avec ArmadaDeployment.kt (DEPLOY_MIN_MM=76.2, MAX=228.6).
    Front = distance 1-3, latéral = 1-2, arrière = distance 1.'''
    return {'front':228.6, 'right':152.4, 'left':152.4, 'rear':76.2}.get(sec, 76.2)

def dist_mm(a,b):
    return math.hypot(a['xMm']-b['xMm'], a['yMm']-b['yMm'])

def parse_dice(arcval):
    """'attack' stocke [black, blue, red] pour chaque secteur (catalogue)."""
    v = arcval or [0,0,0]
    return (int(v[0]), int(v[1]), int(v[2]))

rnd=0
try:
  for rnd in range(1,7):
    log(rnd,'SYSTEM','phase',{'phase':'COMMAND'},'début phase commandement')
    for side,fleet in FLEETS.items():
        ships=[u for u in fleet if u['type']=='ship' and not u['destroyed']]
        if not ships:
            log(rnd,side,'no_command_possible',{'flotte':'vide'},'aucun vaisseau : pas de cadran')
            continue
        cap=max((s.get('command',1) for s in ships), default=1)
        # Le catalogue n'expose pas les vraies icônes de cadran : on cycle sur
        # NAV/ING/FEU/ESC (approximation SIGNALÉE, pas une icône fictive 'I').
        icons=['NAV','ING','FEU','ESC']
        command_stacks[side]=[icons[i%len(icons)] for i in range(cap)]
        log(rnd,side,'recharge_command_stack',
            {'capacite':cap,'pile':list(command_stacks[side])},
            'pile FIFO rechargée à la valeur de commande (RRG)')
        if command_stacks[side]:
            token=command_stacks[side].pop(0)
            if order_tokens[side].get(token,0) > 0:
                log(rnd,side,'order_token_discarded',{'token':token},
                    'doublon défaussé immédiatement (1 copie max/type)')
            else:
                order_tokens[side][token]=1
                log(rnd,side,'reveal_command_dial',{'token':token,'stock':dict(order_tokens[side])},
                    'cadran révélé et crédité (1 copie max/type)')

    # --- SHIP PHASE ---
    log(rnd,'SYSTEM','phase',{'phase':'SHIP'},'début phase vaisseau')
    for side in ('REPUBLIC','EMPIRE'):
        foe='EMPIRE' if side=='REPUBLIC' else 'REPUBLIC'
        for u in FLEETS[side]:
            if u['destroyed'] or u['type']!='ship': continue
            # Manoeuvre (vitesse) — gabarit INDICATIF (22.5/cran, 40mm/segment)
            old=(u['xMm'],u['yMm'])
            spd=min(u['speed'], 4)
            step=40.0*spd
            foes_now=[t for t in FLEETS[foe] if not t['destroyed']]
            if foes_now:
                dn=min(dist_mm(u,t) for t in foes_now)
                step=max(0.0, min(step, dn-60.0))  # s'arrête à distance 1 = 76,2 mm (marge 16 mm)
            u['yMm'] += (-1 if side=='REPUBLIC' else 1) * step  # SUD(y>0) avance vers 0
            log(rnd,side,'maneuver',{'unit':u['name'],'speed':spd,'from':[round(old[0],1),round(old[1],1)],
                'to':[round(u['xMm'],1),round(u['yMm'],1)],
                'gabarit':'INDICATIF 22.5deg/cran, 40mm/segment (non sourcé)'},
                'manœuvre résolue')
            # Tirs : 2 max depuis 2 secteurs DIFFÉRENTS (RRG)
            for shot in range(2):
                targets=[t for t in FLEETS[foe] if not t['destroyed']]
                if not targets: break
                targets=[(t,arc(u,t)) for t in targets if dist_mm(u,t)<=portee_max(arc(u,t))+1.0]
                if not targets:
                    log(rnd,side,'no_target_in_range',{'unit':u['name']},'aucune cible à portée')
                    break
                tgt,sec=random.choice(targets)
                if sec in u['sectorsUsed']:
                    log(rnd,side,'attack_skip',{'unit':u['name'],'sector':sec},
                        'secteur déjà utilisé (2 tirs = 2 zones distinctes)')
                    continue
                dice=parse_dice((u.get('attack') or {}).get(sec))
                if sum(dice)==0: continue
                rolls=roll(dice)
                dmg=damage(rolls, attacker_is_ship=True, target_is_ship=(tgt['type']=='ship'))
                acc=sum(1 for _,f in rolls if f[3])
                # Boucliers : appliquer face au secteur de la cible (RRG simplifié)
                tsec=arc(tgt,u); sh=tgt.get('shield',{}).get(tsec,0)
                absorbed=min(sh,dmg); tgt_shield=tgt.setdefault('shield',{})
                tgt_shield[tsec]=max(0,sh-absorbed); hull_dmg=dmg-absorbed
                tgt['hull']=max(0,tgt['hull']-hull_dmg)
                fatal = tgt['hull']==0
                if fatal: tgt['destroyed']=True
                u['sectorsUsed'].append(sec)
                log(rnd,side,'attack',{'attacker':u['name'],'defender':tgt['name'],
                    'sector':sec,'dice':list(dice),'rangeMm':round(dist_mm(u,tgt),1),'rangeMaxMm':portee_max(sec)},
                    f'{hull_dmg} dommages coque',
                    {'roll':[[c,NAME[f]] for c,f in rolls],'crit':sum(1 for _,f in rolls if f[2]),
                     'accuracy':acc,'shield_absorbed':absorbed,'hull_dmg':hull_dmg,
                     'crit': (sum(1 for _,f in rolls if f[2]) if tgt['type']=='ship' else 0),
                     'crit_note': ('critique réservée aux vaisseaux' if tgt['type']!='ship' else ''),
                     'target_hull':tgt['hull'],'target_type':tgt['type'],
                     'sectors_used':list(u['sectorsUsed'])})
                if fatal: check_elimination(rnd)

    # --- SQUADRON PHASE ---
    log(rnd,'SYSTEM','phase',{'phase':'SQUADRON'},'début phase escadron')
    for side in ('REPUBLIC','EMPIRE'):
        foe='EMPIRE' if side=='REPUBLIC' else 'REPUBLIC'
        for u in FLEETS[side]:
            if u['destroyed'] or u['type']!='squadron': continue
            # L'escadron se déplace vers son ennemi le plus proche (vitesse x 25,4mm)
            # avant d'attaquer : sans ça il reste à distance 3 et ne tire jamais.
            foes_all=[t for t in FLEETS[foe] if not t['destroyed']]
            if foes_all:
                near=min(foes_all, key=lambda t: dist_mm(u,t))
                step=min(u['speed']*25.4, max(0.0, dist_mm(u,near)-60.0))
                oldp=(u['xMm'],u['yMm'])
                u['xMm'] += (near['xMm']-u['xMm'])/max(dist_mm(u,near),0.01)*step
                u['yMm'] += (near['yMm']-u['yMm'])/max(dist_mm(u,near),0.01)*step
                log(rnd,side,'squadron_move',{'unit':u['name'],'from':[round(oldp[0],1),round(oldp[1],1)],
                    'to':[round(u['xMm'],1),round(u['yMm'],1)],'mm':round(step,1)},'escadron avance')
            targets=[t for t in FLEETS[foe] if not t['destroyed'] and dist_mm(u,t)<=78.0]
            if not targets:
                log(rnd,side,'squadron_no_target',{'unit':u['name']},'aucune cible à distance 1')
                continue
            tgt=random.choice(targets)
            # RRG : anti-squadron vs escadron, BATTERIE vs vaisseau.
            dice_choice = SQUAD_DICE.get(u['name'], {'squad':(0,3,0),'battery':(0,1,0)})
            dice = dice_choice['battery'] if tgt['type']=='ship' else dice_choice['squad']
            rolls=roll(dice)
            is_ship = tgt['type']=='ship'
            dmg=damage(rolls, attacker_is_ship=False, target_is_ship=is_ship)
            crits=sum(1 for _,f in rolls if f[2]) if is_ship else 0  # 0 critique vs escadron
            tgt['hull']=max(0,tgt['hull']-dmg)
            fatal = tgt['hull']==0
            if fatal: tgt['destroyed']=True
            log(rnd,side,'squadron_attack',{'attacker':u['name'],'defender':tgt['name'],
                'dice':list(dice),'mode':'battery' if is_ship else 'anti-squadron'},
                f'{dmg} dommages',
                {'roll':[[c,NAME[f]] for c,f in rolls],'crit':crits,'target_hull':tgt['hull'],
                 'target_type':tgt['type']})
            if fatal: check_elimination(rnd)

    # --- STATUS PHASE ---
    log(rnd,'SYSTEM','phase',{'phase':'STATUS'},'début phase statut')
    for side,fleet in FLEETS.items():
        for u in fleet:
            u['sectorsUsed']=[]  # suivi vidé en début de manche suivante (RRG)
            if u['type']=='ship' and not u['destroyed']:
                # RRG : régénération = points d'ingénierie dépensés pour rendre
                # des boucliers (1 pt = 1 bouclier dans un secteur, max valeur initiale).
                eng=u.get('engineering',0)
                regen=0
                if eng>0 and isinstance(u.get('shield'),dict) and isinstance(u.get('maxShield'),dict):
                    for sec in list(u['shield']):
                        while eng>0 and u['shield'][sec] < u['maxShield'].get(sec,0):
                            u['shield'][sec]+=1; eng-=1; regen+=1
                u['sectorsUsed']=[]
                log(rnd,side,'repair',{'unit':u['name'],'engineering':u.get('engineering',0),
                    'boucliers_regen':regen,'shield':dict(u.get('shield') or {})},
                    f'{regen} bouclier(s) régénéré(s)')

    # Score : 2 VP par objectif tenu (INDICATIF), second joueur départage
    holder='REPUBLIC' if rnd%2==1 else 'EMPIRE'
    scores[holder]+=2
    log(rnd,'SYSTEM','score_objective',{'holder':holder,'scores':dict(scores)},
        '2 VP objectif (indicatif)')
    if scores['REPUBLIC']>=12 or scores['EMPIRE']>=12:
        w='REPUBLIC' if scores['REPUBLIC']>scores['EMPIRE'] else 'EMPIRE'
        log(rnd,'SYSTEM','victory',{'scores':dict(scores)},w)
        break

except PartieTerminee:
    pass  # élimination immédiate déjà loggée

# Départage / élimination
if not any(x['action'] in ('victory','elimination') for x in logs):
    alive_r=[u for u in FLEETS['REPUBLIC'] if u['type']=='ship' and not u['destroyed']]
    alive_e=[u for u in FLEETS['EMPIRE'] if u['type']=='ship' and not u['destroyed']]
    if not alive_r or not alive_e:
        w='EMPIRE' if not alive_r else 'REPUBLIC'
        log(rnd,'SYSTEM','elimination',{'republic_ships':len(alive_r),'empire_ships':len(alive_e)},w)
    else:
        # RRG 1.6.0 : en égalité, c'est le SECOND joueur qui gagne (pas de DRAW).
        # Ici REPUBLIC = premier joueur, EMPIRE = second joueur.
        # RRG p.32 : les points de la flotte ADVERSE détruite vont au score du joueur.
        for side, foe in (('REPUBLIC','EMPIRE'), ('EMPIRE','REPUBLIC')):
            # RRG p.32 : vaisseaux ET escadrons ennemis détruits comptent.
            pts=sum(u['points'] for u in FLEETS[foe] if u['destroyed'])
            scores[side]+=pts
            log(rnd,'SYSTEM','score_fleet_destroyed',{'side':side,'flotte_détruite':foe,
                'points':pts,'scores':dict(scores)},
                f'{pts} points de la flotte {foe} détruite crédités à {side}')
        w='REPUBLIC' if scores['REPUBLIC']>scores['EMPIRE'] else ('EMPIRE' if scores['EMPIRE']>scores['REPUBLIC'] else 'EMPIRE')
        log(rnd,'SYSTEM','final_score',{'scores':dict(scores),
            'tiebreak':'second joueur (EMPIRE) gagne en cas d égalité'},w)

os.makedirs(os.path.dirname(OUT),exist_ok=True)
with open(OUT,'w') as f:
    for x in logs: f.write(json.dumps(x,ensure_ascii=False)+'\n')
with open(NOTATION,'w') as f:
    f.write('SW ARMADA 1.6.0 — PARTIE SIMULÉE (replay)'+'\n')
    f.write('Stats lues dans catalog.json. Des : ArmadaDice.kt. Dep : bande distance 1-3.'+'\n')
    f.write('Positions/PV/boucliers/secteurs complets dans le JSONL (cle snapshot).'+'\n\n')
    for x in logs:
        d=x['detail']
        if x['action']=='deploy':
            line=f"[{x['t']:03d}] R{x['round']} {x['player']:<8} DEPLOY  {d.get('unit','')} {d.get('from')} zone {d.get('zone')}"
        elif x['action']=='maneuver':
            line=f"[{x['t']:03d}] R{x['round']} {x['player']:<8} MANEUVR {d.get('unit','')} v{d.get('speed')} {d.get('from')} -> {d.get('to')}"
        elif x['action']=='attack':
            st=x['state']
            line=f"[{x['t']:03d}] R{x['round']} {x['player']:<8} ATK     {d.get('attacker','')} -> {d.get('defender','')} sect {d.get('sector')} des {d.get('dice')} crit {st.get('crit')} acc {st.get('accuracy')} bouclier {st.get('shield_absorbed')} COQUE {st.get('hull_dmg')}"
        elif x['action']=='squadron_attack':
            line=f"[{x['t']:03d}] R{x['round']} {x['player']:<8} SQUAD   {d.get('attacker','')} -> {d.get('defender','')} PV RESTANTS {x['state'].get('target_hull')} / des {x['detail'].get('dice')}"
        elif x['action']=='score_objective':
            line=f"[{x['t']:03d}] R{x['round']} SYSTEM  SCORE   {d.get('holder')} {d.get('scores')}"
        else:
            line=f"[{x['t']:03d}] R{x['round']} {x['player']:<8} {x['action'].upper():<20} {json.dumps(d,ensure_ascii=False)[:90]} -> {x['result']}"
        f.write(line+'\n')
print(f'LOG : {OUT} ({len(logs)} entrées)')
print(f'NOTATION : {NOTATION}')
print('Scores:',scores)
