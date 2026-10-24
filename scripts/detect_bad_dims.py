#!/usr/bin/env python3
"""Détecteur dimensionnel FR v2 — critères de la recette identifiée (0.9.75).

Formats VALIDES d'une carte upgrade/unité FR (mesurés iron-squadron 300dpi) :
  - Std Fra upgrade : ratio ~0.694 (713x1027) ; pleine 0.698 (726x1040)
  - Paysage anneau : ~1.431 (1039x726)
SUSPECT si :
  A) ratio hors [0.68,0.71] ET hors [1.40,1.46]  (mauvais rapport)
  B) 365x525  -> contenu FAUX connu (basse rés, mauvais contenu) = à remplacer
Surtout NE PAS flag les 713/714x1027 (std correct) juste parce qu'ils diffèrent
du dominant du dossier. Le ratio est le critère, pas la largeur de groupe.
"""
import cv2, os, glob, json, sys, collections
BASE = sys.argv[1] if len(sys.argv)>1 else '/root/sw-army-builder/app/src/main/assets/cards/swl'
rows=[]
for d in sorted(glob.glob(BASE+'/*/fr')):
    fac=os.path.basename(os.path.dirname(d))
    for f in glob.glob(d+'/*.webp'):
        img=cv2.imread(f, cv2.IMREAD_GRAYSCALE)
        if img is None: continue
        h,w=img.shape; r=w/h
        flags=[]
        if (w,h)==(365,525):
            flags.append('365x525 CONTENU FAUX')
        elif not ((0.675<=r<=0.710) or (1.40<=r<=1.46)):
            flags.append(f'RATIO {r:.3f} hors-format')
        if flags:
            rows.append({'file':f'{fac}/fr/{os.path.basename(f)}','w':w,'h':h,'ratio':round(r,3),'flags':flags})
json.dump(rows, open('/tmp/bad_dims_v2.json','w'), ensure_ascii=False, indent=1)
far=[]
for r in rows: far.append(r['file'])
print(f"SUSPECTS v2 (ratio/hors-format) : {len(rows)}")
for r in rows:
    print(f"  {r['file']:55s} {r['w']}x{r['h']} r={r['ratio']}  {'; '.join(r['flags'])}")