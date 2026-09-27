#!/usr/bin/env python3
"""Write etymologies for the new name day list in small, saved batches.

Etymologies are written a few names at a time: `next` shows the next batch of
names that still lack one (with their Wikipedia article intros and the app's
old text as reference), and `add` merges a finished batch into
scripts/output/etymologies.json and saves it immediately. Nothing is lost if
work stops halfway; just run `next` again.

Usage:
    python3 scripts/etymology_batch.py status
    python3 scripts/etymology_batch.py next [--size 10] [--shard 1/4]
    python3 scripts/etymology_batch.py add batch.json

batch.json maps each name to its entry:
    {"Aapeli": {"text": "Heprealainen nimi, Aabelin suomalainen muoto",
                "sourced": true}}

`text` is the one-line Finnish etymology shown in the widget. `sourced` is
false when the Wikipedia intro did not cover the name's origin; those names
are listed in scripts/output/etymology_review.json for a human to check.
"""

import argparse
import fcntl
import json
import sys
import zlib
from contextlib import contextmanager
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
ROOT = SCRIPT_DIR.parent
NAMEDAYS_FILE = SCRIPT_DIR / "output" / "namedays.json"
ETYMOLOGIES_FILE = SCRIPT_DIR / "output" / "etymologies.json"
REVIEW_FILE = SCRIPT_DIR / "output" / "etymology_review.json"
SOURCES_FILE = SCRIPT_DIR / "name_sources.json"
OLD_ETYMOLOGIES_FILE = ROOT / "app" / "src" / "main" / "assets" / "etymologies.json"
LOCK_FILE = SCRIPT_DIR / "output" / ".etymologies.lock"

MAX_LENGTH = 80
SHOWN_EXTRACT = 700  # enough for the origin sentence; keeps batches small


def load(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def save(path, data):
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(dict(sorted(data.items())) if isinstance(data, dict) else sorted(data),
                              ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    tmp.replace(path)


@contextmanager
def locked():
    """Serialize writers so parallel workers can't overwrite each other."""
    LOCK_FILE.parent.mkdir(exist_ok=True)
    with LOCK_FILE.open("w") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        yield


def all_names():
    namedays = load(NAMEDAYS_FILE, {})
    return sorted({n for day in namedays.values() for names in day.values() for n in names})


def in_shard(name, shard):
    if not shard:
        return True
    index, count = (int(x) for x in shard.split("/"))
    return zlib.crc32(name.encode("utf-8")) % count == index - 1


def cmd_status(_args):
    names = all_names()
    done = load(ETYMOLOGIES_FILE, {})
    review = load(REVIEW_FILE, [])
    print(f"{sum(n in done for n in names)}/{len(names)} names have an etymology, "
          f"{len(review)} marked for review")


def cmd_next(args):
    done = load(ETYMOLOGIES_FILE, {})
    sources = load(SOURCES_FILE, {})
    todo = [n for n in all_names() if n not in done and in_shard(n, args.shard)]
    # Names missing from name_sources.json haven't been fetched yet (an empty
    # list means there's no article); leave them until fetch_name_articles.py
    # has caught up.
    waiting = [n for n in todo if n not in sources]
    todo = [n for n in todo if n in sources]
    if not todo:
        print(f"Nothing ready: {len(waiting)} names waiting for sources." if waiting
              else "All done.")
        return
    old = load(OLD_ETYMOLOGIES_FILE, {})
    batch = {}
    for name in todo[:args.size]:
        batch[name] = {
            "old_app_text": old.get(name),
            "wikipedia": [{"title": s["title"], "extract": s["extract"][:SHOWN_EXTRACT]}
                          for s in sources.get(name, [])],
        }
    print(f"# {len(todo)} names ready in this shard ({len(waiting)} waiting for sources); "
          f"showing {len(batch)}")
    print(json.dumps(batch, ensure_ascii=False, indent=1))


def cmd_add(args):
    batch = json.loads(Path(args.file).read_text(encoding="utf-8"))
    known = set(all_names())
    errors = []
    for name, entry in batch.items():
        text = entry.get("text", "").strip() if isinstance(entry, dict) else ""
        if name not in known:
            errors.append(f"{name}: not in the name day list")
        elif not text:
            errors.append(f"{name}: empty text")
        elif len(text) > MAX_LENGTH:
            errors.append(f"{name}: text is {len(text)} chars (max {MAX_LENGTH})")
        elif "\n" in text:
            errors.append(f"{name}: text must be a single line")
    if errors:
        sys.exit("Batch rejected, nothing saved:\n  " + "\n  ".join(errors))

    with locked():
        etymologies = load(ETYMOLOGIES_FILE, {})
        review = set(load(REVIEW_FILE, []))
        for name, entry in batch.items():
            etymologies[name] = entry["text"].strip()
            if entry.get("sourced", False):
                review.discard(name)
            else:
                review.add(name)
        save(ETYMOLOGIES_FILE, etymologies)
        save(REVIEW_FILE, review)
    print(f"Saved {len(batch)} etymologies ({len(etymologies)} total).")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("status").set_defaults(func=cmd_status)
    p = sub.add_parser("next")
    p.add_argument("--size", type=int, default=10)
    p.add_argument("--shard", help="e.g. 2/4: only names in the 2nd of 4 disjoint shards")
    p.set_defaults(func=cmd_next)
    p = sub.add_parser("add")
    p.add_argument("file")
    p.set_defaults(func=cmd_add)
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
