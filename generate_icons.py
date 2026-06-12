#!/usr/bin/env python3
"""
Generate Android launcher icons for Are We Okay app.
Creates square and round icons in all required mipmap densities.
"""
from PIL import Image, ImageDraw, ImageFilter
import math
import os

# Source image path - the uploaded icon
SOURCE = r"C:\Users\erens\.gemini\antigravity-ide\brain\87d9cbc7-a17c-4ecb-9bb9-060be53fc508\user_upload_1.png"

# Android mipmap sizes: (folder_name, size_px)
MIPMAP_SIZES = [
    ("mipmap-mdpi",    48),
    ("mipmap-hdpi",    72),
    ("mipmap-xhdpi",   96),
    ("mipmap-xxhdpi",  144),
    ("mipmap-xxxhdpi", 192),
]

BASE_DIR = r"d:\android-projeler\are-we-okay-app\android\app\src\main\res"


def add_rounded_corners(img, radius_fraction=0.225):
    """Apply rounded corners to an RGBA image."""
    size = img.size
    radius = int(size[0] * radius_fraction)
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([0, 0, size[0]-1, size[1]-1], radius=radius, fill=255)
    result = img.copy()
    result.putalpha(mask)
    return result


def make_circle(img):
    """Crop image to a circle."""
    size = img.size
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([0, 0, size[0]-1, size[1]-1], fill=255)
    result = img.copy()
    result.putalpha(mask)
    return result


def generate_icon(source_img, size, shape="square"):
    """Resize and shape the source icon."""
    # Resize to target size with high quality
    icon = source_img.resize((size, size), Image.LANCZOS)
    if shape == "round":
        icon = make_circle(icon)
    else:
        icon = add_rounded_corners(icon, radius_fraction=0.225)
    return icon


def main():
    print(f"Loading source icon from: {SOURCE}")
    
    # Check if source exists, otherwise look for uploaded image
    if not os.path.exists(SOURCE):
        # Try alternative paths
        alt_paths = [
            r"C:\Users\erens\.gemini\antigravity-ide\brain\87d9cbc7-a17c-4ecb-9bb9-060be53fc508\user_upload_1.png",
            r"C:\Users\erens\.gemini\antigravity-ide\brain\87d9cbc7-a17c-4ecb-9bb9-060be53fc508\user_upload.png",
        ]
        source = None
        for p in alt_paths:
            if os.path.exists(p):
                source = p
                break
        if not source:
            # List available files
            brain_dir = r"C:\Users\erens\.gemini\antigravity-ide\brain\87d9cbc7-a17c-4ecb-9bb9-060be53fc508"
            print(f"Source not found. Files in brain dir:")
            for f in os.listdir(brain_dir):
                print(f"  {f}")
            return
    else:
        source = SOURCE

    src = Image.open(source).convert("RGBA")
    print(f"Source image size: {src.size}")

    for folder, size in MIPMAP_SIZES:
        out_dir = os.path.join(BASE_DIR, folder)
        os.makedirs(out_dir, exist_ok=True)

        # Square icon (ic_launcher.webp)
        square = generate_icon(src, size, shape="square")
        sq_path = os.path.join(out_dir, "ic_launcher.webp")
        square.save(sq_path, "WEBP", quality=95)
        print(f"  Saved {sq_path} ({size}x{size})")

        # Round icon (ic_launcher_round.webp)
        rnd = generate_icon(src, size, shape="round")
        rnd_path = os.path.join(out_dir, "ic_launcher_round.webp")
        rnd.save(rnd_path, "WEBP", quality=95)
        print(f"  Saved {rnd_path} ({size}x{size})")

    print("\nDone! All launcher icons generated.")


if __name__ == "__main__":
    main()
