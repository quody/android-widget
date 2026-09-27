#!/usr/bin/env python3
"""Validate name day and etymology files against the app's data format.

Usage:
    python3 scripts/validate_data.py                    # checks scripts/output/
    python3 scripts/validate_data.py app/src/main/assets

Format (see scripts/README.md):
    namedays.json     {"MM-DD": {"fi": [name, ...], "sv": [name, ...]}}
                      exactly the 366 days of a leap year, in calendar order
    etymologies.json  {name: "one-line Finnish etymology"}
                      one entry for every name that appears in namedays.json

Exits non-zero if anything is wrong.
"""

import calendar
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def main():
    data_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "scripts" / "output"
    errors, warnings = [], []

    namedays = json.loads((data_dir / "namedays.json").read_text(encoding="utf-8"))
    expected_keys = [f"{m:02d}-{d:02d}" for m in range(1, 13)
                     for d in range(1, calendar.monthrange(2024, m)[1] + 1)]
    if list(namedays) != expected_keys:
        missing = set(expected_keys) - set(namedays)
        extra = set(namedays) - set(expected_keys)
        errors.append(f"namedays.json: keys must be the 366 dates in order "
                      f"(missing {sorted(missing)}, unexpected {sorted(extra)})")

    names = set()
    for key, day in namedays.items():
        if not isinstance(day, dict) or set(day) != {"fi", "sv"}:
            errors.append(f"namedays.json {key}: must have exactly 'fi' and 'sv' lists")
            continue
        for lang, day_names in day.items():
            if not isinstance(day_names, list) or not all(isinstance(n, str) and n.strip() == n and n
                                                          for n in day_names):
                errors.append(f"namedays.json {key}.{lang}: must be a list of non-empty strings")
                continue
            if len(set(day_names)) != len(day_names):
                errors.append(f"namedays.json {key}.{lang}: duplicate names")
            names.update(day_names)

    etymology_file = data_dir / "etymologies.json"
    if etymology_file.exists():
        etymologies = json.loads(etymology_file.read_text(encoding="utf-8"))
        for name, text in etymologies.items():
            if not isinstance(text, str) or not text.strip() or "\n" in text:
                errors.append(f"etymologies.json {name}: must be a non-empty single-line string")
        missing = sorted(names - set(etymologies))
        unused = sorted(set(etymologies) - names)
        if missing:
            errors.append(f"etymologies.json: {len(missing)} names have no etymology, "
                          f"e.g. {', '.join(missing[:10])}")
        if unused:
            warnings.append(f"etymologies.json: {len(unused)} entries for names not in "
                            f"namedays.json: {', '.join(unused[:10])}")
    else:
        errors.append(f"{etymology_file} not found")

    for w in warnings:
        print("warning: " + w)
    for e in errors:
        print("error: " + e)
    print(f"{data_dir}: {len(names)} names, {'OK' if not errors else f'{len(errors)} error(s)'}")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
