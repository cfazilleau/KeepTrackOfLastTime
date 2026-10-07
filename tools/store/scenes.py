"""The screens captured for the store, in order. Driven by capture.py."""

import time
from pathlib import Path

import demo
from capture import PACKAGE, Device, demo_status_bar, launch, seed

# Shown in the icon palette screenshot: the demo tiles' icons, then a few more.
PALETTE = ("sprout,cat,phone,scissors,car,bed,trash,coffee,refrigerator,pill,footprints,toothbrush,paw-print,"
           "dumbbell,shopping-cart,shirt,bath,gift,cake")


def fresh(dev: Device, locale: str, theme="light", palette=None, only=None):
    seed(dev, locale, theme, palette, only)
    dev.sh(f"pm grant {PACKAGE} android.permission.POST_NOTIFICATIONS")
    dev.sh("cmd statusbar collapse")
    launch(dev)
    dev.find(demo.names(locale)["plants"], timeout=20)  # The tiles are in, even on a slow emulator.
    time.sleep(0.5)


def run(dev: Device, locale: str, s: dict[str, str], out: Path, kind: str):
    names = demo.names(locale)

    def shot(n):
        demo_status_bar(dev)  # Again: hides the reminder's notification icon.
        time.sleep(0.3)
        dev.screencap(out / f"{n}.png")

    w, h = dev.size()

    if kind == "feature":
        # Three tiles alone, for the feature graphic: the wide one, then two side by side, away from the + button.
        fresh(dev, locale, only={"plants", "grandma", "haircut"})
        shot("feature")
        return

    # 1. Home: all tiles, light theme.
    fresh(dev, locale)
    shot(1)

    if kind == "tablet":
        # 2. Dark theme.
        fresh(dev, locale, theme="dark")
        shot(2)
        # 3. Editing a tile.
        fresh(dev, locale)
        dev.long_press(*dev.find(names["grandma"]))
        time.sleep(1.5)
        shot(3)
        return

    # 2. Tap a tile: it restarts, and offers to undo.
    dev.tap(*dev.find(names["plants"]))
    time.sleep(2.2)
    shot(2)

    # 3. Editing a tile.
    fresh(dev, locale)
    dev.long_press(*dev.find(names["grandma"]))
    time.sleep(1.5)
    shot(3)

    # 4. Its reminder, further down the same sheet.
    scroll_to(dev, s["label_reminder"], h * 0.36)
    shot(4)
    dev.sh("input keyevent BACK")

    # 5. One group.
    fresh(dev, locale)
    dev.tap_label(names["home"])
    time.sleep(1.5)
    shot(5)

    # 6. Dark theme, scrolled to the groups' sections.
    fresh(dev, locale, theme="dark")
    scroll_to(dev, names["home"], h * 0.255, last=True)
    shot(6)

    # 7. The icon palette.
    fresh(dev, locale, palette=PALETTE)
    for attempt in range(3):  # A tap right after launch can be lost.
        dev.tap_label(s["action_menu"])
        try:
            dev.tap_label(s["settings_icon_palette"], timeout=3)
            break
        except LookupError:
            if attempt == 2:
                raise
    dev.find(s["icon_palette_yours"])  # The screen has slid in.
    time.sleep(1)
    shot(7)

    # 8. Widgets on the home screen. They were placed once by hand (README): plants, cat, Grandma, vitamins.
    fresh(dev, locale)
    dev.sh("input keyevent HOME")
    # Swapping the database behind the app's back doesn't redraw the widgets: ask for it.
    dev.sh(f"am broadcast -a {PACKAGE}.action.WIDGET_TICK -n {PACKAGE}/.widget.WidgetTickReceiver")
    time.sleep(3)
    shot(8)


def scroll_to(dev: Device, label: str, y: float, last=False):
    """Scrolls slowly (no fling) until the node labelled [label] (the lowest one if [last]) is at height [y]."""
    w, _ = dev.size()
    for _ in range(5):
        found = [n["bounds"] for n in dev.nodes() if label in (n["text"], n["desc"])]
        if not found or (last and len(found) < 2):  # [last]: the first one is a chip at the top.
            dev.drag(w / 2, 1400, 600)
            continue
        l, t, r, b = max(found, key=lambda bounds: bounds[1]) if last else found[0]
        distance = (t + b) / 2 - y
        if abs(distance) < 20:
            break
        start = 1500 if distance > 0 else 500
        dev.drag(w / 2, start, start - distance)
        time.sleep(0.8)
