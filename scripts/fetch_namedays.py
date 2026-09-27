#!/usr/bin/env python3
"""Fetch Finnish and Finland-Swedish name days from the Finnish Wikipedia.

Every date page (e.g. https://fi.wikipedia.org/wiki/2._tammikuuta) has a
"Nimipäivät" section with lines like:

    * suomalainen kalenteri: [[Aapeli (nimi)|Aapeli]]
    * suomenruotsalainen kalenteri: [[Gerhard]], [[Gert]], [[Jerry]]

This script builds the list of the 366 date page links, downloads the raw
wikitext of each page and writes the parsed names to a JSON file in the same
format as app/src/main/assets/namedays.json.

Usage:
    python3 scripts/fetch_namedays.py            # fetch + parse
    python3 scripts/fetch_namedays.py --offline  # re-parse cached pages only

Outputs (in scripts/):
    wikipedia_links.txt       MM-DD <tab> page URL, one per day
    namedays_wikipedia.json   {"MM-DD": {"fi": [...], "sv": [...]}}
    .wikicache/               raw wikitext cache (git-ignored)
"""

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
CACHE_DIR = SCRIPT_DIR / ".wikicache"
LINKS_FILE = SCRIPT_DIR / "wikipedia_links.txt"
OUTPUT_FILE = SCRIPT_DIR / "namedays_wikipedia.json"

USER_AGENT = "nimipaivat-widget-namedays/1.0 (https://github.com/quody/android-widget)"

# Finnish month names in the partitive case, as used in the page titles.
MONTHS = [
    ("tammikuuta", 31),
    ("helmikuuta", 29),
    ("maaliskuuta", 31),
    ("huhtikuuta", 30),
    ("toukokuuta", 31),
    ("kesäkuuta", 30),
    ("heinäkuuta", 31),
    ("elokuuta", 31),
    ("syyskuuta", 30),
    ("lokakuuta", 31),
    ("marraskuuta", 30),
    ("joulukuuta", 31),
]

CALENDARS = {
    "fi": "suomalainen kalenteri",
    "sv": "suomenruotsalainen kalenteri",
}


# Days on which no one has a name day (New Year's Day, leap day, Christmas Day).
NO_NAMEDAY = {"01-01", "02-29", "12-25"}


def date_pages():
    """Yield (date key, page title) for every day of a leap year."""
    for month_index, (month_name, days) in enumerate(MONTHS, start=1):
        for day in range(1, days + 1):
            yield f"{month_index:02d}-{day:02d}", f"{day}. {month_name}"


def page_url(title):
    return "https://fi.wikipedia.org/wiki/" + urllib.parse.quote(title.replace(" ", "_"))


def fetch_wikitext(title, retries=5):
    url = "https://fi.wikipedia.org/w/index.php?" + urllib.parse.urlencode(
        {"title": title, "action": "raw"}
    )
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    delay = 2
    for attempt in range(retries):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.read().decode("utf-8")
        except urllib.error.HTTPError as e:
            if e.code not in (429, 500, 502, 503, 504) or attempt == retries - 1:
                raise
            wait = int(e.headers.get("Retry-After") or delay)
        except urllib.error.URLError:
            if attempt == retries - 1:
                raise
            wait = delay
        time.sleep(wait)
        delay *= 2
    raise RuntimeError("unreachable")


def cached_wikitext(key, title, offline):
    cache_file = CACHE_DIR / f"{key}.wiki"
    if cache_file.exists():
        return cache_file.read_text(encoding="utf-8")
    if offline:
        raise FileNotFoundError(f"{cache_file} missing (run without --offline first)")
    text = fetch_wikitext(title)
    CACHE_DIR.mkdir(exist_ok=True)
    cache_file.write_text(text, encoding="utf-8")
    time.sleep(0.5)  # be polite to Wikipedia
    return text


def nameday_section(wikitext):
    match = re.search(r"^==\s*Nimipäivät\s*==\s*$(.*?)(?=^==[^=])", wikitext, re.M | re.S)
    return match.group(1) if match else ""


def clean_markup(text):
    text = re.sub(r"<ref[^>]*/>", "", text)
    text = re.sub(r"<ref[^>]*>.*?</ref>", "", text, flags=re.S)
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    text = re.sub(r"\{\{[^{}]*\}\}", "", text)
    text = re.sub(r"\[\[(?:[^\]|]*\|)?([^\]]*)\]\]", r"\1", text)  # [[a|b]] -> b
    text = re.sub(r"<[^>]+>", "", text)
    text = text.replace("'''", "").replace("''", "").replace("&nbsp;", " ")
    return text


def parse_names(section, calendar):
    for line in section.splitlines():
        line = clean_markup(line).strip()
        if not line.startswith("*"):
            continue
        label, sep, names = line.lstrip("*").partition(":")
        if not sep or label.strip(" –-").lower() != calendar:
            continue
        return [n.strip().rstrip(".") for n in re.split(r"[,;]", names) if n.strip()]
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--offline", action="store_true", help="only use cached pages")
    args = parser.parse_args()

    pages = list(date_pages())
    LINKS_FILE.write_text(
        "".join(f"{key}\t{page_url(title)}\n" for key, title in pages), encoding="utf-8"
    )

    result = {}
    warnings = []
    for i, (key, title) in enumerate(pages, start=1):
        print(f"[{i:3d}/{len(pages)}] {title}", file=sys.stderr)
        section = nameday_section(cached_wikitext(key, title, args.offline))
        entry = {}
        for lang, calendar in CALENDARS.items():
            names = parse_names(section, calendar)
            if names is None:
                if key not in NO_NAMEDAY:
                    warnings.append(f"{key} ({title}): no '{calendar}' line")
                names = []
            entry[lang] = names
        result[key] = entry

    with OUTPUT_FILE.open("w", encoding="utf-8") as f:
        f.write("{\n")
        lines = [
            f'  "{key}": {{ "fi": {json.dumps(e["fi"], ensure_ascii=False)}, '
            f'"sv": {json.dumps(e["sv"], ensure_ascii=False)} }}'
            for key, e in result.items()
        ]
        f.write(",\n".join(lines))
        f.write("\n}\n")

    print(f"\nWrote {OUTPUT_FILE.relative_to(SCRIPT_DIR.parent)} and "
          f"{LINKS_FILE.relative_to(SCRIPT_DIR.parent)}", file=sys.stderr)
    if warnings:
        print(f"\n{len(warnings)} warning(s):", file=sys.stderr)
        for w in warnings:
            print("  " + w, file=sys.stderr)


if __name__ == "__main__":
    main()
