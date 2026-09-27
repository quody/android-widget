#!/usr/bin/env python3
"""Fetch the Finnish Wikipedia article intro for every name in the name day list.

The name day sections link each name to its article, e.g.
[[Aapeli (nimi)|Aapeli]]. This script collects those link targets from the
pages cached by fetch_namedays.py, downloads the plain-text lead section of
each article and writes them to scripts/name_sources.json:

    {"Aapeli": [{"title": "Aapeli (nimi)", "url": "...", "extract": "..."}], ...}

The intros are the reference material for writing etymologies.

Usage:
    python3 scripts/fetch_name_articles.py   (run fetch_namedays.py first)
"""

import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

from fetch_namedays import CACHE_DIR, OUTPUT_FILE, fetch_wikitext, nameday_section, page_url

SCRIPT_DIR = Path(__file__).resolve().parent
SOURCES_FILE = SCRIPT_DIR / "name_sources.json"
EXTRACT_CACHE = CACHE_DIR / "extracts.json"
MAX_EXTRACT = 1200

def link_targets():
    """Map displayed name -> set of article titles it links to."""
    targets = {}
    for wiki_file in sorted(CACHE_DIR.glob("*.wiki")):
        section = nameday_section(wiki_file.read_text(encoding="utf-8"))
        for target, display in re.findall(r"\[\[([^\]|#]+)(?:\|([^\]]*))?\]\]", section):
            name = (display or target).strip().strip(",").replace("''", "")
            targets.setdefault(name, set()).add(target.strip())
    return targets


def strip_templates(text):
    """Remove {{...}} templates, including nested ones."""
    out, depth, i = [], 0, 0
    while i < len(text):
        if text.startswith("{{", i):
            depth += 1
            i += 2
        elif text.startswith("}}", i) and depth:
            depth -= 1
            i += 2
        else:
            if not depth:
                out.append(text[i])
            i += 1
    return "".join(out)


def intro_text(wikitext):
    """Plain text of the article's lead section (everything before the first heading)."""
    lead = re.split(r"^==", wikitext, maxsplit=1, flags=re.M)[0]
    lead = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>|<!--.*?-->", "", lead, flags=re.S)
    lead = strip_templates(lead)
    lead = re.sub(r"\{\|.*?\|\}", "", lead, flags=re.S)  # tables
    lead = re.sub(r"\[\[(?:Tiedosto|Kuva|File|Image|Luokka|Category):[^\[\]]*(?:\[\[[^\]]*\]\][^\[\]]*)*\]\]",
                  "", lead, flags=re.I)
    lead = re.sub(r"\[\[(?:[^\]|]*\|)?([^\]]*)\]\]", r"\1", lead)
    lead = re.sub(r"\[https?://\S+ ([^\]]*)\]", r"\1", lead)
    lead = re.sub(r"<[^>]+>", "", lead)
    lead = lead.replace("'''", "").replace("''", "").replace("&nbsp;", " ")
    lines = [line.strip() for line in lead.splitlines()]
    return "\n".join(line for line in lines if line and not line.startswith(("*", "|", "!")))


def fetch_intro(title, hops=3):
    """Return (resolved title, lead text) of an article, following redirects.

    Uses the plain action=raw endpoint: Wikipedia rate-limits the API endpoints
    much more aggressively.
    """
    try:
        wikitext = fetch_wikitext(title)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            return title, ""
        raise
    redirect = re.match(r"\s*#(?:REDIRECT|UUDELLEENOHJAUS)\s*\[\[([^\]|#]+)", wikitext, re.I)
    if redirect and hops:
        time.sleep(0.5)
        return fetch_intro(redirect.group(1).strip(), hops - 1)
    if re.search(r"\{\{\s*(täsmennyssivu|disambig)", wikitext, re.I):
        return title, ""
    return title, intro_text(wikitext)


def fetch_extracts(titles, checkpoint):
    """Return {requested title: [resolved title, extract]}, cached between runs.

    checkpoint(cache) is called every few pages so partial results are usable.
    """
    cache = json.loads(EXTRACT_CACHE.read_text(encoding="utf-8")) if EXTRACT_CACHE.exists() else {}
    todo = [t for t in titles if t not in cache]
    checkpoint(cache)
    for i, title in enumerate(todo, start=1):
        resolved, extract = fetch_intro(title)
        cache[title] = [resolved, extract.strip()[:MAX_EXTRACT]]
        if i % 10 == 0 or i == len(todo):
            print(f"[{i}/{len(todo)}] fetched", file=sys.stderr)
            EXTRACT_CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=1), encoding="utf-8")
            checkpoint(cache)
        time.sleep(0.5)  # be polite to Wikipedia
    return cache


def main():
    namedays = json.loads(OUTPUT_FILE.read_text(encoding="utf-8"))
    names = sorted({n for day in namedays.values() for names in day.values() for n in names})
    targets = link_targets()

    wanted = {}
    for name in names:
        titles = set(targets.get(name, ())) or {f"{name} (nimi)", f"{name} (etunimi)", name}
        wanted[name] = sorted(titles)

    def write_sources(extracts):
        """Write sources for every name whose articles have all been fetched."""
        sources = {}
        for name, titles in wanted.items():
            if not all(t in extracts for t in titles):
                continue
            found = []
            for t in titles:
                resolved, text = extracts[t]
                if text and resolved not in {f["title"] for f in found}:
                    found.append({"title": resolved, "url": page_url(resolved), "extract": text})
            sources[name] = found
        SOURCES_FILE.write_text(json.dumps(sources, ensure_ascii=False, indent=1) + "\n",
                                encoding="utf-8")
        return sources

    extracts = fetch_extracts(sorted({t for ts in wanted.values() for t in ts}), write_sources)
    sources = write_sources(extracts)
    missing = [n for n, s in sources.items() if not s]
    print(f"Wrote {SOURCES_FILE.name}: {len(sources) - len(missing)}/{len(sources)} names "
          f"have an article intro", file=sys.stderr)
    if missing:
        print("No article: " + ", ".join(missing), file=sys.stderr)


if __name__ == "__main__":
    main()
