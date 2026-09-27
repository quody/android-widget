# Name day data scripts

Tools for rebuilding the widget's name day and etymology data from the Finnish
Wikipedia, instead of relying on hand- or LLM-compiled lists.

## Data format

The app reads two JSON files from `app/src/main/assets/`. The new data in
`scripts/output/` uses exactly the same format, so swapping it in is a copy:

```sh
python3 scripts/validate_data.py            # must print OK
cp scripts/output/namedays.json scripts/output/etymologies.json app/src/main/assets/
```

### `namedays.json`

```json
{
  "01-01": { "fi": [], "sv": [] },
  "01-02": { "fi": ["Aapeli"], "sv": ["Gerhard", "Gert", "Jerry"] }
}
```

- Keys are `MM-DD`, exactly the 366 days of a leap year, in calendar order.
- `fi`: names in the Finnish-language almanac ("suomalainen kalenteri").
- `sv`: names in the Finland-Swedish almanac ("suomenruotsalainen kalenteri").
- Both lists are always present; they're empty on days with no name day
  (1 January, 29 February and 25 December).
- Names keep the order Wikipedia lists them in and appear at most once per list.

### `etymologies.json`

```json
{
  "Aapeli": "Heprealainen nimi, Aabelin suomalainen muoto"
}
```

- One entry for every name that appears in `namedays.json`, in either list.
- The value is a single line of Finnish text, at most 80 characters, shown in
  the widget.

`scripts/output/etymology_review.json` lists the names whose etymology couldn't
be checked against their Wikipedia article. Review these by hand before
shipping.

## Workflow

1. `python3 scripts/fetch_namedays.py` downloads all 366 date pages
   (links in `wikipedia_links.txt`) and writes `output/namedays.json`.
2. `python3 scripts/compare_namedays.py` writes `namedays_comparison.md`,
   a day-by-day diff against the app's current data.
3. `python3 scripts/fetch_name_articles.py` fetches the intro of each name's
   Wikipedia article into `name_sources.json`.
4. `python3 scripts/etymology_batch.py next` / `add batch.json` writes the
   etymologies 10 names at a time, saving after each batch (see the script's
   docstring). `status` shows progress.
5. `python3 scripts/validate_data.py` checks both output files against the
   format above.

Downloaded pages are cached in `scripts/.wikicache/`, which git ignores.
