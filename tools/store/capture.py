"""Takes the raw store screenshots on a running emulator, in every language.

Needs a rooted emulator (a "Google APIs" image, not "Google Play") with the app installed:

    ./gradlew assembleRelease
    adb -s emulator-5558 root
    adb -s emulator-5558 install -r app/build/outputs/apk/release/app-release.apk
    python tools/store/capture.py --serial emulator-5558 [--locales en-US fr-FR] [--kind phone|tablet|feature]

Each locale gets demo tiles (demo.py) written straight into the app's database, then the script drives the UI
(through uiautomator) and saves PNGs to build/store/raw/<kind>/<locale>/<n>.png. compose.py frames them.
"""

import argparse
import os
import re
import sqlite3
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path

import demo

PACKAGE = "dev.cfaz.timeclicker"
DATA = f"/data/data/{PACKAGE}"
ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
SCHEMA_HASH = "c7f56db0fc44baf1b40e1a57ac76bf47"  # app/schemas/.../5.json
SCHEMA_VERSION = 5

# Store locale -> the app's resource folder, to find buttons by their translated labels.
RES_DIR = {
    "en-US": "values", "fr-FR": "values-fr", "es-ES": "values-es", "de-DE": "values-de", "it-IT": "values-it",
    "pt-BR": "values-pt", "nl-NL": "values-nl", "pl-PL": "values-pl", "ru-RU": "values-ru", "tr-TR": "values-tr",
    "id": "values-in", "ar": "values-ar", "hi-IN": "values-hi", "ja-JP": "values-ja", "ko-KR": "values-ko",
    "zh-CN": "values-zh-rCN",
}

os.environ["MSYS_NO_PATHCONV"] = "1"  # Git Bash: keep device paths as they are


class Device:
    def __init__(self, serial: str):
        self.serial = serial

    def adb(self, *args: str, binary=False, check=True):
        out = subprocess.run(["adb", "-s", self.serial, *args], capture_output=True, check=check)
        return out.stdout if binary else out.stdout.decode("utf-8", "replace")

    def sh(self, cmd: str, check=True) -> str:
        return self.adb("shell", cmd, check=check)

    def now_ms(self) -> int:
        return int(self.sh("date +%s").strip()) * 1000

    def screencap(self, path: Path):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(self.adb("exec-out", "screencap", "-p", binary=True))

    def tap(self, x: float, y: float):
        self.sh(f"input tap {int(x)} {int(y)}")

    def long_press(self, x: float, y: float, ms=900):
        self.sh(f"input swipe {int(x)} {int(y)} {int(x)} {int(y)} {ms}")

    def swipe(self, x1, y1, x2, y2, ms=300):
        self.sh(f"input swipe {int(x1)} {int(y1)} {int(x2)} {int(y2)} {ms}")

    def drag(self, x: float, y1: float, y2: float):
        """Scrolls by exactly y1 - y2: moves quickly (no long-press), then holds still before lifting (no fling)."""
        steps = max(2, int(abs(y2 - y1) // 40))
        moves = "; ".join(f"input motionevent MOVE {int(x)} {int(y1 + (y2 - y1) * i / steps)}" for i in range(1, steps + 1))
        self.sh(f"input motionevent DOWN {int(x)} {int(y1)}; {moves}; sleep 0.4; input motionevent UP {int(x)} {int(y2)}")

    def size(self) -> tuple[int, int]:
        w, h = re.search(r"(\d+)x(\d+)", self.sh("wm size").splitlines()[-1]).groups()
        return int(w), int(h)

    def nodes(self) -> list[dict]:
        """The visible UI, as a flat list of {text, desc, bounds: (l, t, r, b)}."""
        for _ in range(5):
            out = self.sh("uiautomator dump /sdcard/ui.xml >/dev/null && cat /sdcard/ui.xml", check=False)
            if "<hierarchy" in out:
                break
            time.sleep(0.5)
        root = ET.fromstring(out[out.index("<?xml"):] if "<?xml" in out else out[out.index("<hierarchy"):])
        nodes = []
        for n in root.iter("node"):
            l, t, r, b = map(int, re.findall(r"\d+", n.get("bounds")))
            nodes.append({"text": n.get("text", ""), "desc": n.get("content-desc", ""), "bounds": (l, t, r, b)})
        return nodes

    def find(self, label: str, exact=True, timeout=8.0) -> tuple[float, float]:
        """Centre of the first node whose text or content description is [label]."""
        end = time.time() + timeout
        while True:
            for n in self.nodes():
                for value in (n["text"], n["desc"]):
                    if value and (value == label if exact else label in value):
                        l, t, r, b = n["bounds"]
                        return (l + r) / 2, (t + b) / 2
            if time.time() > end:
                raise LookupError(f"No node labelled {label!r}")
            time.sleep(0.4)

    def tap_label(self, label: str, **kw):
        self.tap(*self.find(label, **kw))


def strings(locale: str) -> dict[str, str]:
    """The app's strings for a locale, falling back to English."""
    def load(folder):
        tree = ET.parse(RES / folder / "strings.xml")
        out = {}
        for s in tree.getroot().iter("string"):
            out[s.get("name")] = "".join(s.itertext()).replace("\\'", "'").replace('\\"', '"')
        return out
    values = load("values")
    values.update(load(RES_DIR[locale]))
    return values


def build_database(path: Path, locale: str, now_ms: int, only=None):
    names = demo.names(locale)
    db = sqlite3.connect(path)
    db.executescript(f"""
        CREATE TABLE `tracker_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `position` INTEGER NOT NULL);
        CREATE TABLE `trackers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `position` INTEGER NOT NULL, `group_id` INTEGER, `color` TEXT NOT NULL DEFAULT 'sage', `icon` TEXT NOT NULL DEFAULT 'check', `size` TEXT NOT NULL DEFAULT 'small', `photo` TEXT, `count_since` INTEGER NOT NULL DEFAULT 0, `reminder_every` INTEGER, `reminder_unit` TEXT, FOREIGN KEY(`group_id`) REFERENCES `tracker_groups`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL );
        CREATE INDEX `index_trackers_group_id` ON `trackers` (`group_id`);
        CREATE TABLE `tracker_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tracker_id` INTEGER NOT NULL, `done_at` INTEGER NOT NULL, FOREIGN KEY(`tracker_id`) REFERENCES `trackers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
        CREATE INDEX `index_tracker_events_tracker_id_done_at` ON `tracker_events` (`tracker_id`, `done_at`);
        CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT);
        INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '{SCHEMA_HASH}');
        CREATE TABLE android_metadata (locale TEXT);
        INSERT INTO android_metadata VALUES ('en_US');
        PRAGMA user_version = {SCHEMA_VERSION};
    """)
    group_ids = {}
    for position, key in enumerate(demo.GROUPS):
        group_ids[key] = db.execute(
            "INSERT INTO tracker_groups (name, position) VALUES (?, ?)", (names[key], position)).lastrowid
    year = 365 * 86400 * 1000
    tiles = [t for t in demo.TILES if only is None or t[0] in only]
    for position, (key, group, color, icon, size, elapsed, presses, reminder) in enumerate(tiles):
        last = now_ms - elapsed * 1000
        created = last - year * 2
        every, unit = reminder or (None, None)
        photo = photo_name(key) if key in demo.PHOTOS else None
        tid = db.execute(
            "INSERT INTO trackers (name, created_at, position, group_id, color, icon, size, photo, count_since,"
            " reminder_every, reminder_unit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)",
            (names[key], created, position, group_ids.get(group), color, icon, size, photo, every, unit)).lastrowid
        # The last press, and the earlier ones spread before it.
        step = (last - created) // presses
        for i in range(presses):
            db.execute("INSERT INTO tracker_events (tracker_id, done_at) VALUES (?, ?)", (tid, last - i * step))
    db.commit()
    db.close()


def photo_name(key: str) -> str:
    """The photo's file name in the app's photo folder (PhotoStore)."""
    return f"demo-{key}.img"


def settings_xml(theme: str, palette: str | None) -> str:
    palette_line = f'\n    <string name="icon_palette">{palette}</string>' if palette else ""
    return f"""<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="theme">{theme}</string>
    <boolean name="wallpaper_colors" value="false" />
    <string name="time_display">relative</string>
    <boolean name="haptics" value="false" />
    <boolean name="click_sound" value="false" />
    <boolean name="show_counter" value="true" />{palette_line}
</map>
"""


def push_as_app(dev: Device, local: Path, remote: str):
    dev.adb("push", str(local), "/data/local/tmp/push.tmp")
    dev.sh(f"cp /data/local/tmp/push.tmp {remote} && rm /data/local/tmp/push.tmp")
    owner = dev.sh(f"stat -c %U {DATA}").strip()
    dev.sh(f"chown {owner}:{owner} {remote} && chmod 660 {remote} && restorecon {remote}")


def seed(dev: Device, locale: str, theme="light", palette: str | None = None, only=None):
    """Replaces the app's data with the demo tiles of [locale], and switches the app to that language."""
    dev.sh(f"am force-stop {PACKAGE}")
    dev.sh(f"cmd locale set-app-locales {PACKAGE} --locales {demo.ANDROID_LOCALE[locale]}")
    dev.sh(f"am force-stop {PACKAGE}")
    # A fresh install has none of these folders until its first launch.
    owner = dev.sh(f"stat -c %U {DATA}").strip()
    folders = f"{DATA}/databases {DATA}/shared_prefs {DATA}/files {DATA}/files/photos"
    dev.sh(f"mkdir -p {folders} && chown {owner}:{owner} {folders} && chmod 771 {folders} && restorecon {folders}")
    for key, photo in demo.PHOTOS.items():
        push_as_app(dev, Path(__file__).parent / photo, f"{DATA}/files/photos/{photo_name(key)}")
    dev.sh(f"rm -f {DATA}/databases/time_clicker.db* {DATA}/shared_prefs/settings.xml {DATA}/shared_prefs/reminders_posted.xml", check=False)
    with tempfile.TemporaryDirectory() as tmp:
        db = Path(tmp) / "time_clicker.db"
        build_database(db, locale, dev.now_ms(), only)
        push_as_app(dev, db, f"{DATA}/databases/time_clicker.db")
        prefs = Path(tmp) / "settings.xml"
        prefs.write_text(settings_xml(theme, palette), encoding="utf-8")
        push_as_app(dev, prefs, f"{DATA}/shared_prefs/settings.xml")
    # A widget broadcast may have started the app meanwhile, holding the old database open.
    dev.sh(f"am force-stop {PACKAGE}")


def launch(dev: Device, wait=2.5):
    dev.sh(f"am start -W -n {PACKAGE}/.MainActivity")
    time.sleep(wait)


def demo_status_bar(dev: Device, on=True):
    """A clean status bar: 10:00, full battery and signal, no notifications."""
    b = "am broadcast -a com.android.systemui.demo"
    if not on:
        dev.sh(f"{b} -e command exit")
        return
    dev.sh("settings put global sysui_demo_allowed 1")
    dev.sh(f"{b} -e command enter")
    dev.sh(f"{b} -e command clock -e hhmm 1000")
    dev.sh(f"{b} -e command battery -e level 100 -e plugged false")
    dev.sh(f"{b} -e command network -e wifi show -e level 4")
    dev.sh(f"{b} -e command network -e mobile show -e datatype none -e level 4")
    dev.sh(f"{b} -e command notifications -e visible false")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="emulator-5558")
    parser.add_argument("--locales", nargs="*", default=demo.LOCALES)
    parser.add_argument("--kind", default="phone", choices=["phone", "tablet", "feature"])
    parser.add_argument("--dump", action="store_true", help="print the visible UI nodes and exit")
    args = parser.parse_args()
    dev = Device(args.serial)
    if args.dump:
        for n in dev.nodes():
            if n["text"] or n["desc"]:
                print(n["bounds"], repr(n["text"]), repr(n["desc"]))
        return
    import scenes
    demo_status_bar(dev)
    for locale in args.locales:
        print(locale, flush=True)
        out = ROOT / "build/store/raw" / ("phone" if args.kind == "feature" else args.kind) / locale
        scenes.run(dev, locale, strings(locale), out, args.kind)
    demo_status_bar(dev, on=False)


if __name__ == "__main__":
    sys.exit(main())
