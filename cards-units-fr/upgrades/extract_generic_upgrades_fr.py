#!/usr/bin/env python3
"""Extract full FR generic upgrade cards from rendered pages.
Card boundaries detected from gutters:
  cols (page x): col0 [216,910], col1 [942,1636], col2 [1667,2335]
  rows (page y): row0 [0,1130], row1 [1130,2170], row2 [2170,3209]
Extract full card (badge to title band), no side fragments.
"""
import cv2, numpy as np, os, glob, json

SRC = "/tmp/gen_pages"
OUT = "/tmp/gen_cards"
os.makedirs(OUT, exist_ok=True)

COLS = [(216,910),(942,1636),(1667,2335)]
ROWS = [(0,1130),(1130,2170),(2170,3209)]

results = {}
for page in sorted(glob.glob(os.path.join(SRC, "pg-*.png"))):
    pagenum = int(os.path.basename(page).split("-")[1].split(".")[0])
    img = cv2.imread(page)
    for ri, (y0, y1) in enumerate(ROWS):
        for ci, (x0, x1) in enumerate(COLS):
            card = img[y0:y1, x0:x1]
            name = f"p{pagenum:02d}_r{ri}_c{ci}.png"
            cv2.imwrite(os.path.join(OUT, name), card)
            results[name] = {"page": pagenum, "row": ri, "col": ci,
                             "size": [int(card.shape[1]), int(card.shape[0])]}

print("extracted:", len(results))
json.dump(results, open("/tmp/gen_cards_manifest.json","w"), indent=1)
from collections import Counter
sizes = Counter((v["size"][0], v["size"][1]) for v in results.values())
print("sizes:", sizes.most_common(6))
