"""Checks the store listing against Google Play's limits.

    python tools/store/check.py
"""

import sys
from pathlib import Path

from PIL import Image

META = Path(__file__).resolve().parents[2] / "fastlane/metadata/android"

TEXT_LIMITS = {"title.txt": 30, "short_description.txt": 80, "full_description.txt": 4000, "changelogs/default.txt": 500}
# Folder: (min, max count). Each image: 320 to 3840 px a side, no longer side over twice the shorter one.
IMAGES = {"phoneScreenshots": (2, 8), "sevenInchScreenshots": (0, 8), "tenInchScreenshots": (0, 8)}


def main() -> int:
    errors = []
    locales = sorted(p.name for p in META.iterdir() if p.is_dir())
    for locale in locales:
        d = META / locale
        for name, limit in TEXT_LIMITS.items():
            f = d / name
            if not f.exists():
                errors.append(f"{locale}: missing {name}")
                continue
            n = len(f.read_text(encoding="utf-8").strip())
            if n > limit:
                errors.append(f"{locale}: {name} is {n} characters (limit {limit})")
        images = d / "images"
        for folder, (low, high) in IMAGES.items():
            shots = sorted((images / folder).glob("*.*")) if (images / folder).exists() else []
            if not low <= len(shots) <= high:
                errors.append(f"{locale}: {len(shots)} {folder} (needs {low} to {high})")
            for shot in shots:
                with Image.open(shot) as im:
                    w, h = im.size
                    if im.mode not in ("RGB", "L") or not 320 <= min(w, h) <= max(w, h) <= 3840 or max(w, h) > 2 * min(w, h):
                        errors.append(f"{locale}: {folder}/{shot.name} is {w}x{h} {im.mode}")
        feature = images / "featureGraphic.png"
        if feature.exists():
            with Image.open(feature) as im:
                if im.size != (1024, 500) or im.mode != "RGB":
                    errors.append(f"{locale}: featureGraphic is {im.size} {im.mode}, needs 1024x500 RGB")
        else:
            errors.append(f"{locale}: missing featureGraphic.png")
    icon = META / "en-US/images/icon.png"
    if not icon.exists():
        errors.append("en-US: missing icon.png")
    print("\n".join(errors) or f"OK: {len(locales)} locales")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
