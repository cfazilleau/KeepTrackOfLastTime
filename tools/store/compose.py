"""Builds the store graphics from the raw screenshots of capture.py.

    python tools/store/compose.py [--locales en-US fr-FR]

Writes into fastlane/metadata/android/<locale>/images/:
  phoneScreenshots/<n>.jpg   1080x1920, a caption above each raw phone screenshot (8 scenes)
  tenInchScreenshots/<n>.jpg 2560x1440, the same for the tablet ones (when captured)
  featureGraphic.png         1024x500, the icon, the name, a tagline and three tiles (capture.py --kind feature)
  icon.png                   512x512 (en-US only: the default for every language)

The text comes from captions.json. Pages are HTML rendered by headless Microsoft Edge (or Chrome: set BROWSER),
so every script (Arabic, Devanagari, CJK) is shaped properly. Fonts are Noto, from Google Fonts.
"""

import argparse
import html
import json
import os
import shutil
import subprocess
import tempfile
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from PIL import Image

import demo

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
RAW = ROOT / "build/store/raw"
WORK = ROOT / "build/store/html"
META = ROOT / "fastlane/metadata/android"
CAPTIONS = json.loads((HERE / "captions.json").read_text(encoding="utf-8"))

BROWSER = os.environ.get("BROWSER") or next(
    (p for p in [
        r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        shutil.which("chromium") or "", shutil.which("google-chrome") or "",
    ] if p and Path(p).exists()), None)

LIGHT = {"ground": "#ECEEF3", "text": "#1C1F26", "muted": "#555B69",
         "shadow": "rgba(160,168,190,.6)", "highlight": "rgba(255,255,255,.95)"}
DARK = {"ground": "#1B1D22", "text": "#F1F2F5", "muted": "#A2A8B4",
        "shadow": "rgba(0,0,0,.6)", "highlight": "rgba(255,255,255,.05)"}

# Scene 6 (dark theme) gets a dark page.
DARK_SCENES = {6}

# The launcher icon (res/drawable/ic_launcher_foreground.xml), as SVG, on its background colour.
ICON_SVG = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72">
<rect x="0" y="0" width="108" height="108" fill="#2E6B4F"/>
<path d="M54,30 A24,24 0 1,1 33.2,42" stroke="#FFFFFF" stroke-width="5" stroke-linecap="round" fill="none"/>
<path d="M26,37 L40,36 L33,48 Z" fill="#FFFFFF"/>
<path d="M54,40 L54,54 L64,60" stroke="#FFFFFF" stroke-width="5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
</svg>"""

FONT = {
    "ar": ("Noto Sans Arabic", "Noto+Sans+Arabic"), "hi-IN": ("Noto Sans Devanagari", "Noto+Sans+Devanagari"),
    "ja-JP": ("Noto Sans JP", "Noto+Sans+JP"), "ko-KR": ("Noto Sans KR", "Noto+Sans+KR"),
    "zh-CN": ("Noto Sans SC", "Noto+Sans+SC"),
}

# Height of the tablet's taskbar, cut from tablet captures.
TASKBAR = 130

# The tiles of the "feature" capture (phone, 1080x1920): one wide, two small under it, shadows included.
TILES_CROP = (0, 410, 1080, 1370)


def page(locale: str, size: tuple[int, int], body: str, css: str, theme=LIGHT) -> str:
    family, query = FONT.get(locale, ("Noto Sans", "Noto+Sans"))
    lang = demo.ANDROID_LOCALE[locale].split("-")[0]
    rtl = ' dir="rtl"' if locale == "ar" else ""
    vars_ = "".join(f"--{k}:{v};" for k, v in theme.items())
    return f"""<!doctype html><html lang="{lang}"{rtl}><head><meta charset="utf-8">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Noto+Sans:wght@500;700;800&family={query}:wght@500;700;800&display=block">
<style>
:root{{{vars_}}}
html,body{{margin:0;width:{size[0]}px;height:{size[1]}px;overflow:hidden;background:var(--ground);color:var(--text);
  font-family:"Noto Sans","{family}",sans-serif;}}
:lang(ko){{word-break:keep-all;}} :lang(ja){{word-break:auto-phrase;}} /* No line breaks inside words. */
{css}
</style></head><body>{body}</body></html>"""


def screenshot_page(locale: str, raw: Path, caption: str, theme: dict, tablet: bool) -> str:
    if tablet:
        w, h, shot_w, top, cap_top, cap_size = 2560, 1440, 1900, 300, 88, 84
    else:
        w, h, shot_w, top, cap_top, cap_size = 1080, 1920, 840, 470, 120, 76
    css = f"""
.cap{{position:absolute;left:70px;right:70px;top:{cap_top}px;height:{top - cap_top - 50}px;display:flex;
  align-items:center;justify-content:center;text-align:center;font-weight:800;font-size:{cap_size}px;
  line-height:1.15;letter-spacing:-.01em;text-wrap:balance;}}
.shot{{position:absolute;left:{(w - shot_w) // 2}px;top:{top}px;width:{shot_w}px;border-radius:{56 if not tablet else 48}px;
  box-shadow:28px 28px 64px var(--shadow),-28px -28px 64px var(--highlight);}}
"""
    body = f'<div class="cap">{html.escape(caption)}</div><img class="shot" src="{raw.as_uri()}">'
    return page(locale, (w, h), body, css, theme)


def feature_page(locale: str, tiles: Path) -> str:
    # The tiles on the right (on the left in Arabic), the text on the other side.
    side, other = ("left", "right") if locale == "ar" else ("right", "left")
    w, h = Image.open(tiles).size
    height = 440
    tiles_width = w * height / h
    css = f"""
.text{{position:absolute;{other}:64px;top:0;bottom:0;width:{1024 - 64 - 24 - 32 - tiles_width:.0f}px;display:flex;flex-direction:column;justify-content:center;}}
.icon{{width:112px;height:112px;border-radius:30px;overflow:hidden;margin-bottom:28px;
  box-shadow:10px 10px 24px var(--shadow),-10px -10px 24px var(--highlight);}}
.icon svg{{display:block;width:100%;height:100%;}}
h1{{margin:0 0 10px;font-size:62px;font-weight:800;letter-spacing:-.02em;line-height:1.05;}}
p{{margin:0;font-size:30px;font-weight:500;color:var(--muted);line-height:1.25;text-wrap:balance;}}
.tiles{{position:absolute;{side}:24px;top:{(500 - height) / 2}px;height:{height}px;width:{tiles_width:.1f}px;
  /* Fades the crop's edges into the page: no seam where a neighbour's shadow was cut. */
  mask-image:linear-gradient(to bottom,transparent,#000 7%,#000 93%,transparent),
    linear-gradient(to right,transparent,#000 5%,#000 95%,transparent);
  mask-composite:intersect;}}
"""
    body = (f'<div class="text"><div class="icon">{ICON_SVG}</div><h1>Time Clicker</h1>'
            f'<p>{html.escape(CAPTIONS[locale]["tagline"])}</p></div><img class="tiles" src="{tiles.as_uri()}">')
    return page(locale, (1024, 500), body, css)


def icon_page() -> str:
    return page("en-US", (512, 512), f'<div style="width:512px;height:512px">{ICON_SVG}</div>',
                "svg{display:block;width:512px;height:512px}")


def render(html_text: str, name: str, size: tuple[int, int]) -> Path:
    """Renders a page to build/store/html/<name>.png."""
    WORK.mkdir(parents=True, exist_ok=True)
    src = WORK / f"{name}.html"
    src.write_text(html_text, encoding="utf-8")
    out = WORK / f"{name}.png"
    with tempfile.TemporaryDirectory() as profile:
        subprocess.run([
            BROWSER, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
            "--allow-file-access-from-files", f"--user-data-dir={profile}", "--virtual-time-budget=10000",
            f"--window-size={size[0]},{size[1]}", f"--screenshot={out}", src.as_uri(),
        ], check=True, capture_output=True, timeout=120)
    with Image.open(out) as im:
        assert im.size == size, f"{name}: rendered at {im.size}, expected {size}"
    return out


def save(png: Path, dest: Path):
    dest.parent.mkdir(parents=True, exist_ok=True)
    im = Image.open(png).convert("RGB")  # The store wants no alpha channel.
    if dest.suffix == ".jpg":
        im.save(dest, quality=90, optimize=True, progressive=True)
    else:
        im.save(dest, optimize=True)


def crop_tiles(locale: str) -> Path:
    """The three tiles of the "feature" capture, with their shadows, on the page colour."""
    path = WORK / f"tiles-{locale}.png"
    WORK.mkdir(parents=True, exist_ok=True)
    Image.open(RAW / "phone" / locale / "feature.png").convert("RGB").crop(TILES_CROP).save(path)
    return path


def crop_taskbar(raw: Path, locale: str, n: int) -> Path:
    """The tablet capture without the taskbar at the bottom (2560x1600 -> 2560x1500)."""
    path = WORK / f"tablet-{locale}-{n}-cropped.png"
    WORK.mkdir(parents=True, exist_ok=True)
    im = Image.open(raw)
    im.crop((0, 0, im.width, im.height - TASKBAR)).save(path)
    return path


def jobs_for(locale: str):
    images = META / locale / "images"
    captions = CAPTIONS[locale]["captions"]
    for kind, folder, size, tablet, scenes in [
        # Phone: the widgets (raw scene 8) come third.
        ("phone", "phoneScreenshots", (1080, 1920), False, [1, 2, 8, 3, 4, 5, 6, 7]),
        # Tablet scenes: home, dark theme, editing a tile.
        ("tablet", "tenInchScreenshots", (2560, 1440), True, [1, 2, 3]),
    ]:
        raw_dir = RAW / kind / locale
        if not raw_dir.exists():
            continue
        for out, n in enumerate(scenes, start=1):
            raw = raw_dir / f"{n}.png"
            if not raw.exists():
                continue
            if tablet:
                caption, theme = [captions[0], captions[5], captions[2]][n - 1], DARK if n == 2 else LIGHT
                raw = crop_taskbar(raw, locale, n)
            else:
                caption, theme = captions[n - 1], DARK if n in DARK_SCENES else LIGHT
            name = f"{kind}-{locale}-{out}"
            yield (screenshot_page(locale, raw, caption, theme, tablet), name, size, images / folder / f"{out}.jpg")
    if (RAW / "phone" / locale / "feature.png").exists():
        yield (feature_page(locale, crop_tiles(locale)), f"feature-{locale}", (1024, 500),
               images / "featureGraphic.png")
    if locale == "en-US":
        yield (icon_page(), "icon", (512, 512), images / "icon.png")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--locales", nargs="*", default=demo.LOCALES)
    args = parser.parse_args()
    if not BROWSER:
        raise SystemExit("No Edge or Chrome found: set BROWSER to a Chromium-based browser.")
    jobs = [job for locale in args.locales for job in jobs_for(locale)]

    def run(job):
        html_text, name, size, dest = job
        save(render(html_text, name, size), dest)
        return dest

    with ThreadPoolExecutor(max_workers=4) as pool:
        for dest in pool.map(run, jobs):
            print(dest.relative_to(ROOT), flush=True)


if __name__ == "__main__":
    main()
