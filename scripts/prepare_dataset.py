"""
Dataset preparation script for the Virtual Try-On FYP.

Supported datasets
------------------
VITON-HD  (upper-body: shirts, blouses, tops)
  → Download from: https://github.com/shadow2496/VITON-HD
  → Google Drive link in the repo README
  → Folder structure expected:
       viton_hd/
         test/
           cloth/          ← white-background garment JPGs
           cloth-mask/     ← binary PNG masks (white=garment, black=background)

DressCode  (upper-body + lower-body + dresses)
  → Download from: https://github.com/aimagelab/dress-code  (requires form request)
  → Folder structure expected:
       dress_code/
         upper_body/
           images/         ← garment images
           masks/          ← binary masks
         lower_body/
           images/
           masks/

Output
------
Converted transparent-background PNGs are written to:
    <android_assets_dir>/garments/<category>/

A garments_catalog.json is also generated for the database seeder.

Usage
-----
  python prepare_dataset.py --dataset viton_hd --input ./viton_hd --output ../app/src/main/assets
  python prepare_dataset.py --dataset dress_code --input ./dress_code --output ../app/src/main/assets
  python prepare_dataset.py --dataset both --viton ./viton_hd --dresscode ./dress_code --output ../app/src/main/assets
"""

import argparse
import json
import os
import random
import shutil
from pathlib import Path

try:
    from PIL import Image
    import numpy as np
except ImportError:
    raise SystemExit("Install dependencies first:  pip install Pillow numpy")


# --------------------------------------------------------------------------- #
# Configuration
# --------------------------------------------------------------------------- #

# How many garments to extract per category (keep assets folder small)
MAX_PER_CATEGORY = 40

# Resize all garments to this size before saving (keeps APK size manageable)
TARGET_SIZE = (512, 682)   # width × height, ~3:4 ratio

SIZE_LABELS = ["XS", "S", "M", "L", "XL"]
COLORS = ["blue", "black", "white", "red", "green", "grey", "navy",
          "beige", "pink", "yellow", "purple", "orange", "brown"]

BODY_TYPE_MAPPING = {
    "SHIRT":   ["SLIM,REGULAR", "REGULAR,LARGE", "SLIM,REGULAR,LARGE"],
    "PANTS":   ["SLIM,REGULAR", "REGULAR,LARGE", "SLIM,REGULAR,LARGE"],
    "GLASSES": ["SLIM,REGULAR,LARGE"],
    "SHOES":   ["SLIM,REGULAR,LARGE"],
}


# --------------------------------------------------------------------------- #
# Core conversion
# --------------------------------------------------------------------------- #

def apply_mask_to_garment(cloth_path: Path, mask_path: Path, out_path: Path) -> bool:
    """Convert white-background garment + binary mask → transparent PNG."""
    try:
        cloth = Image.open(cloth_path).convert("RGBA")
        mask  = Image.open(mask_path).convert("L")

        # Resize mask to match cloth if dimensions differ
        if mask.size != cloth.size:
            mask = mask.resize(cloth.size, Image.LANCZOS)

        cloth.putalpha(mask)
        cloth = cloth.resize(TARGET_SIZE, Image.LANCZOS)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        cloth.save(out_path, "PNG", optimize=True)
        return True
    except Exception as e:
        print(f"  ✗ Failed {cloth_path.name}: {e}")
        return False


def remove_white_background(cloth_path: Path, out_path: Path,
                            tolerance: int = 30) -> bool:
    """
    Fallback: remove near-white background without a mask.
    Works well on VITON-HD which has clean white backgrounds.
    """
    try:
        img  = Image.open(cloth_path).convert("RGBA")
        data = np.array(img, dtype=np.int32)

        r, g, b, a = data[:,:,0], data[:,:,1], data[:,:,2], data[:,:,3]
        white_mask = (r > 255 - tolerance) & (g > 255 - tolerance) & (b > 255 - tolerance)
        data[:,:,3] = np.where(white_mask, 0, 255)

        result = Image.fromarray(data.astype(np.uint8), "RGBA")
        result = result.resize(TARGET_SIZE, Image.LANCZOS)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        result.save(out_path, "PNG", optimize=True)
        return True
    except Exception as e:
        print(f"  ✗ Failed {cloth_path.name}: {e}")
        return False


# --------------------------------------------------------------------------- #
# Metadata generation
# --------------------------------------------------------------------------- #

def guess_color(filename: str) -> str:
    name = filename.lower()
    for c in COLORS:
        if c in name:
            return c
    return random.choice(COLORS)


def make_garment_entry(asset_path: str, garment_type: str,
                       index: int) -> dict:
    color    = guess_color(asset_path)
    size     = SIZE_LABELS[index % len(SIZE_LABELS)]
    body_idx = index % len(BODY_TYPE_MAPPING[garment_type])

    return {
        "name":              f"{garment_type.capitalize()} {index + 1:03d}",
        "type":              garment_type,
        "gender":            "UNISEX",
        "color":             color,
        "imageAssetPath":    asset_path,
        "widthCm":           _width_for_type(garment_type, size),
        "heightCm":          _height_for_type(garment_type, size),
        "sizeLabel":         size,
        "suitableBodyTypes": BODY_TYPE_MAPPING[garment_type][body_idx],
        "suitableGenders":   "MALE,FEMALE,UNISEX",
    }


def _width_for_type(gtype: str, size: str) -> float:
    base = {"XS": 42, "S": 46, "M": 50, "L": 54, "XL": 58}[size]
    if gtype == "PANTS":   return base * 0.9
    if gtype == "GLASSES": return 14.0
    if gtype == "SHOES":   return 26.0
    return float(base)


def _height_for_type(gtype: str, size: str) -> float:
    base = {"XS": 65, "S": 68, "M": 71, "L": 74, "XL": 77}[size]
    if gtype == "PANTS":   return base * 1.2
    if gtype == "GLASSES": return 5.0
    if gtype == "SHOES":   return 10.0
    return float(base)


# --------------------------------------------------------------------------- #
# Dataset-specific processors
# --------------------------------------------------------------------------- #

def process_viton_hd(viton_root: Path, assets_dir: Path) -> list[dict]:
    """Process VITON-HD → upper-body (SHIRT) garments."""
    cloth_dir = viton_root / "test" / "cloth"
    mask_dir  = viton_root / "test" / "cloth-mask"

    if not cloth_dir.exists():
        # Try train split as fallback
        cloth_dir = viton_root / "train" / "cloth"
        mask_dir  = viton_root / "train" / "cloth-mask"

    if not cloth_dir.exists():
        print(f"  ✗ cloth/ not found under {viton_root}")
        return []

    images = sorted(cloth_dir.glob("*.jpg"))[:MAX_PER_CATEGORY]
    print(f"  Processing {len(images)} VITON-HD shirts …")

    catalog = []
    success  = 0
    for i, cloth_path in enumerate(images):
        stem     = cloth_path.stem
        out_name = f"shirt_{i:03d}.png"
        out_path = assets_dir / "garments" / "shirts" / out_name
        asset    = f"garments/shirts/{out_name}"

        # Masks can be .png or .jpg depending on the dataset version
        mask_path = None
        if mask_dir.exists():
            for ext in (".png", ".jpg", ".jpeg"):
                candidate = mask_dir / f"{stem}{ext}"
                if candidate.exists():
                    mask_path = candidate
                    break

        if mask_path:
            ok = apply_mask_to_garment(cloth_path, mask_path, out_path)
        else:
            ok = remove_white_background(cloth_path, out_path)

        if ok:
            catalog.append(make_garment_entry(asset, "SHIRT", success))
            success += 1

    print(f"  ✓ {success} shirts converted")
    return catalog


def process_dresscode(dresscode_root: Path, assets_dir: Path) -> list[dict]:
    """Process DressCode → upper-body (SHIRT) + lower-body (PANTS) garments."""
    catalog = []

    for category, gtype in [("upper_body", "SHIRT"), ("lower_body", "PANTS")]:
        img_dir  = dresscode_root / category / "images"
        mask_dir = dresscode_root / category / "masks"
        if not img_dir.exists():
            print(f"  ✗ {img_dir} not found, skipping")
            continue

        images  = sorted(img_dir.glob("*.jpg"))[:MAX_PER_CATEGORY]
        print(f"  Processing {len(images)} DressCode {category} …")
        success = 0

        for i, img_path in enumerate(images):
            out_name  = f"{gtype.lower()}_{i:03d}.png"
            subfolder = "shirts" if gtype == "SHIRT" else "pants"
            out_path  = assets_dir / "garments" / subfolder / out_name
            asset     = f"garments/{subfolder}/{out_name}"

            mask_path = mask_dir / img_path.name.replace(".jpg", ".png")

            if mask_path.exists():
                ok = apply_mask_to_garment(img_path, mask_path, out_path)
            else:
                ok = remove_white_background(img_path, out_path)

            if ok:
                catalog.append(make_garment_entry(asset, gtype, success))
                success += 1

        print(f"  ✓ {success} {category} items converted")

    return catalog


# --------------------------------------------------------------------------- #
# Entry point
# --------------------------------------------------------------------------- #

def main():
    parser = argparse.ArgumentParser(description="Prepare garment dataset for Virtual Try-On FYP")
    parser.add_argument("--dataset", choices=["viton_hd", "dress_code", "both"],
                        default="viton_hd")
    parser.add_argument("--input",    help="Dataset root (for viton_hd or dress_code)")
    parser.add_argument("--viton",    help="VITON-HD root (when --dataset=both)")
    parser.add_argument("--dresscode",help="DressCode root (when --dataset=both)")
    parser.add_argument("--output",   default="../app/src/main/assets",
                        help="Android assets directory (default: ../app/src/main/assets)")
    args = parser.parse_args()

    assets_dir = Path(args.output).resolve()
    print(f"\nOutput → {assets_dir}\n")

    catalog = []

    if args.dataset == "viton_hd":
        root = Path(args.input or ".").resolve()
        print(f"[VITON-HD] reading from {root}")
        catalog += process_viton_hd(root, assets_dir)

    elif args.dataset == "dress_code":
        root = Path(args.input or ".").resolve()
        print(f"[DressCode] reading from {root}")
        catalog += process_dresscode(root, assets_dir)

    elif args.dataset == "both":
        if args.viton:
            print(f"[VITON-HD] reading from {args.viton}")
            catalog += process_viton_hd(Path(args.viton), assets_dir)
        if args.dresscode:
            print(f"[DressCode] reading from {args.dresscode}")
            catalog += process_dresscode(Path(args.dresscode), assets_dir)

    # Write catalog JSON (consumed by Android DatabaseSeeder)
    catalog_path = assets_dir / "garments_catalog.json"
    catalog_path.parent.mkdir(parents=True, exist_ok=True)
    with open(catalog_path, "w") as f:
        json.dump(catalog, f, indent=2)

    print(f"\n✓ Catalog written: {catalog_path}  ({len(catalog)} garments)")
    print("  Now rebuild the Android project — DatabaseSeeder will populate Room on first launch.")


if __name__ == "__main__":
    main()
