"""
Regenerates the app's icon catalog from the latest Lucide release (https://lucide.dev, ISC licence).

    python tools/lucide/generate_icons.py

Writes app/src/main/assets/lucide/icons.tsv (one icon per line) and the Lucide licence next to it.
Each line is: name <TAB> categories (comma-separated) <TAB> search tags (comma-separated) <TAB> path data.
Every icon is flattened to a single SVG path on a 24x24 grid, drawn with a 2-unit round stroke:
circles, rectangles and lines become path commands, and every arc flag is spelled out
(Android's path parsers don't accept the compact "0010-10" arc form).

Tile icons are stored by Lucide name, so an icon removed or renamed upstream would disappear
from tiles that use it: check the diff of icons.tsv for removed names before committing.

Lucide's tags are only in English. The search words in other languages, tags-<language>.tsv next to icons.tsv
(name <TAB> words, comma-separated), are written by hand: this script lists the icons they lack or no longer have.
"""
import io
import json
import re
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "lucide"
NPM = "https://registry.npmjs.org/lucide-static/latest"
CATEGORIES = "https://lucide.dev/api/categories"


def fetch(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read()


NUM = re.compile(r"-?(?:\d+\.?\d*|\.\d+)(?:[eE][-+]?\d+)?")
ARGS = {"m": 2, "l": 2, "h": 1, "v": 1, "c": 6, "s": 4, "q": 4, "t": 2, "a": 7, "z": 0}


def fmt(x: float) -> str:
    s = f"{x:.3f}".rstrip("0").rstrip(".")
    if s.startswith("0."):
        s = s[1:]
    elif s.startswith("-0."):
        s = "-" + s[2:]
    return s if s not in ("", "-", "-0") else "0"


def normalize(d: str) -> str:
    """Re-serializes path data with explicit separators; the first moveto becomes absolute."""
    i, out, cmd, fresh, first = 0, [], None, False, True
    # The lineto implied by coordinates repeated after a moveto, written out explicitly when it comes.
    implied = None

    def skip():
        nonlocal i
        while i < len(d) and d[i] in " ,\t\n\r":
            i += 1

    def number() -> float:
        nonlocal i
        skip()
        m = NUM.match(d, i)
        if not m:
            raise ValueError(f"bad path data at {i}: {d}")
        i = m.end()
        return float(m.group())

    def flag() -> float:
        nonlocal i
        skip()
        c = d[i]
        i += 1
        return float(c)

    while True:
        skip()
        if i >= len(d):
            break
        if d[i].isalpha():
            cmd = d[i]
            i += 1
            implied = {"m": "l", "M": "L"}.get(cmd)
            # A path's first moveto is absolute even when written "m"; once paths are joined it must say so.
            if first and cmd == "m":
                cmd = "M"
            first = False
            out.append(cmd)
            fresh = True
            if cmd in "zZ":
                continue
        elif cmd is None:
            raise ValueError(d)
        elif implied:
            cmd = implied
            out.append(cmd)
            fresh = True
            implied = None
        if cmd.lower() == "a":
            values = [number(), number(), number(), flag(), flag(), number(), number()]
        else:
            values = [number() for _ in range(ARGS[cmd.lower()])]
        text = " ".join(fmt(v) for v in values)
        out.append(text if fresh else " " + text)
        fresh = False
    return "".join(out)


def element_path(kind: str, a: dict) -> str:
    f = lambda key, default=0.0: float(a.get(key, default))
    if kind == "path":
        return a["d"]
    if kind in ("circle", "ellipse"):
        cx, cy = f("cx"), f("cy")
        rx = f("r") if kind == "circle" else f("rx")
        ry = f("r") if kind == "circle" else f("ry")
        return (f"M{fmt(cx - rx)} {fmt(cy)}a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(2 * rx)} 0"
                f"a{fmt(rx)} {fmt(ry)} 0 1 0 {fmt(-2 * rx)} 0z")
    if kind == "rect":
        x, y, w, h = f("x"), f("y"), f("width"), f("height")
        r = min(f("rx", a.get("ry", 0)), w / 2, h / 2)
        if r == 0:
            return f"M{fmt(x)} {fmt(y)}h{fmt(w)}v{fmt(h)}h{fmt(-w)}z"
        arc = lambda dx, dy: f"a{fmt(r)} {fmt(r)} 0 0 1 {fmt(dx)} {fmt(dy)}"
        return (f"M{fmt(x + r)} {fmt(y)}h{fmt(w - 2 * r)}{arc(r, r)}v{fmt(h - 2 * r)}{arc(-r, r)}"
                f"h{fmt(-(w - 2 * r))}{arc(-r, -r)}v{fmt(-(h - 2 * r))}{arc(r, -r)}z")
    if kind == "line":
        return f"M{fmt(f('x1'))} {fmt(f('y1'))}L{fmt(f('x2'))} {fmt(f('y2'))}"
    if kind in ("polyline", "polygon"):
        points = [float(p) for p in re.split(r"[\s,]+", a["points"].strip())]
        pairs = [f"{fmt(points[k])} {fmt(points[k + 1])}" for k in range(0, len(points), 2)]
        return "M" + "L".join(pairs) + ("z" if kind == "polygon" else "")
    raise ValueError(f"unsupported element {kind}")


def clean(words) -> str:
    return ",".join(w.replace(",", " ").replace("\t", " ").strip() for w in words if w.strip())


def main():
    meta = json.loads(fetch(NPM))
    version = meta["version"]
    print(f"lucide-static {version}")
    with tarfile.open(fileobj=io.BytesIO(fetch(meta["dist"]["tarball"]))) as tar:
        read = lambda name: tar.extractfile(f"package/{name}").read().decode("utf-8")
        nodes = json.loads(read("icon-nodes.json"))
        tags = json.loads(read("tags.json"))
        licence = read("LICENSE")
    categories = json.loads(fetch(CATEGORIES))

    lines = [f"# Lucide {version} (https://lucide.dev), ISC licence. Generated by tools/lucide/generate_icons.py: do not edit."]
    for name in sorted(nodes):
        path = "".join(normalize(element_path(kind, attrs)) for kind, attrs in nodes[name])
        lines.append("\t".join([name, clean(categories.get(name, [])), clean(tags.get(name, [])), path]))

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "icons.tsv").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    (OUT_DIR / "LICENSE").write_text(licence, encoding="utf-8", newline="\n")
    uncategorized = [n for n in nodes if not categories.get(n)]
    print(f"{len(nodes)} icons written; {len(uncategorized)} without a category (only found by search)")

    for path in sorted(OUT_DIR.glob("tags-*.tsv")):
        named = {line.split("\t", 1)[0] for line in path.read_text(encoding="utf-8").splitlines()
                 if line.strip() and not line.startswith("#")}
        removed = sorted(named - nodes.keys())
        missing = sorted(nodes.keys() - named)
        more = "…" if len(missing) > 20 else ""
        print(f"{path.name}: {len(missing)} icons without words" + (f" ({', '.join(missing[:20])}{more})" if missing else ""))
        if removed:
            print(f"{path.name}: remove the lines of icons no longer in Lucide: {', '.join(removed)}")


if __name__ == "__main__":
    main()
