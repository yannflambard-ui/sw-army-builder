#!/usr/bin/env python3
"""
Découpe l'illustration de chaque carte unité Legion en token ROND transparent.
Source : images de cartes légales déjà embarquées dans l'app (assets/cards/swl).
Sortie : app/src/main/assets/legion_tokens/<card_id>.webp (256x256, fond transparent).

Layouts :
 - paysage 1039x726 : art à DROITE (687,75)-(1039,525)
 - portrait 725x1040 : art en HAUT (30,0)-(698,240), crop central
 - petit 363x520 : art en haut

Le token est un disque avec un liseré ; l'art est redimensionné pour remplir le disque.
"""
import json, os, sys, glob
from PIL import Image, ImageDraw, ImageOps

CATALOG = "/root/sw-army-builder/app/src/main/assets/catalog.json"
CARDS_ROOT = "/root/sw-army-builder/app/src/main/assets/cards"
OUT = "/root/sw-army-builder/app/src/main/assets/legion_tokens"
SIZE = 256
RING = 6


def art_box(w, h):
    """Boîte de l'illustration selon l'orientation de la carte."""
    if w > h:  # paysage
        # art à droite
        return (int(w * 0.66), int(h * 0.10), w, int(h * 0.72))
    # portrait / petit : art en haut
    return (int(w * 0.05), 0, int(w * 0.96), int(h * 0.26))


def make_token(img, ring_color=(255, 200, 87, 255)):
    w, h = img.size
    x1, y1, x2, y2 = art_box(w, h)
    art = img.crop((x1, y1, x2, y2)).convert("RGBA")
    aw, ah = art.size
    # Crop carré central de l'art
    side = min(aw, ah)
    left = (aw - side) // 2
    top = (ah - side) // 2
    art = art.crop((left, top, left + side, top + side))
    art = art.resize((SIZE - RING * 2, SIZE - RING * 2), Image.LANCZOS)

    token = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    mask = Image.new("L", (SIZE, SIZE), 0)
    md = ImageDraw.Draw(mask)
    md.ellipse((0, 0, SIZE - 1, SIZE - 1), fill=255)
    # Fond disque + art
    token.paste((16, 24, 36, 255), (0, 0, SIZE, SIZE), mask)
    # Mask circulaire de la taille de l'art (244x244), pas de tout le token
    art_size = SIZE - RING * 2
    inner_mask = Image.new("L", (art_size, art_size), 0)
    ImageDraw.Draw(inner_mask).ellipse((0, 0, art_size - 1, art_size - 1), fill=255)
    token.paste(art, (RING, RING), inner_mask)
    # Liseré
    d = ImageDraw.Draw(token)
    d.ellipse((0, 0, SIZE - 1, SIZE - 1), outline=ring_color, width=RING)
    return token


def main():
    limit = int(sys.argv[1]) if len(sys.argv) > 1 else 0
    data = json.load(open(CATALOG, encoding="utf-8"))
    cards = data.get("cards", data) if isinstance(data, dict) else data
    units = [c for c in cards
             if c.get("kind") == "LEGION_UNIT" and c.get("imageAssetPath")]
    os.makedirs(OUT, exist_ok=True)
    done = 0
    fail = 0
    mapping = {}
    for c in units:
        path = os.path.join("/root/sw-army-builder/app/src/main/assets", c["imageAssetPath"])
        if not os.path.exists(path):
            fail += 1
            continue
        try:
            img = Image.open(path)
            tok = make_token(img)
            safe = c["id"].replace(":", "_").replace("/", "_")
            fname = safe + ".webp"
            tok.save(os.path.join(OUT, fname), "WEBP", quality=88)
            mapping[c["id"]] = fname
            done += 1
        except Exception as e:
            fail += 1
            print("FAIL", c["id"], e)
        if limit and done >= limit:
            break
    json.dump(mapping, open(os.path.join(OUT, "token_map.json"), "w"), ensure_ascii=False, indent=1)
    print(f"OK tokens={done} fail={fail} total_units={len(units)}")


if __name__ == "__main__":
    main()
