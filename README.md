# Ummi

A week-by-week pregnancy companion, built around the Shiʿi pregnancy guide and NHS
weekly notes. One HTML file, no build step, no server, no account. Everything lives
in `localStorage` on the phone it is installed on.

Live at <https://szainababbas.github.io/ummi/>.

## The shape of it

- `index.html` — the whole app: markup, styles, data and logic.
- `sw.js` — service worker. Network-first with a cache fallback, so updates arrive
  as soon as the phone is online and the app still opens when it is not. Bump
  `CACHE` when shipping, so old caches are dropped on activate.
- `manifest.json`, `icon-*.png` — what makes it installable to the home screen.
- `tools/` — the Qurʾān text generator. See `tools/README.md`.
- `tests/` — the test suite.

### Data

- `WEEKS` — weeks 4 to 41: size, development, body, what's normal, a faith note and
  a tip. NHS-derived.
- `MONTHS` — the nine months of the Islamic guide. Each act is its own item with the
  weekdays it applies to (`days: null` for every day), so the app can hand out a
  month a day at a time rather than as one paragraph. Acts carry `surah`, `ayah` or
  `dua` keys pointing at the text, which is what puts a "Read" link on them.
- `SURAHS`, `AYAHS`, `BISMILLAH` — generated, do not hand-edit. See `tools/`.
- `DUA_TEXTS` — the dhikr that is not Qurʾān, so typed rather than fetched.
- `DUAS`, `FOOD`, `NAMES`, `DUA_TODAY` — the older library content.

Arabic is the Uthmani text and the translation is ʿAlī Qulī Qaraʾī, the standard
Shiʿi English rendering. Month content keeps the planner's own citations on each
item.

## Tests

    node tests/run.js

No dependencies and no framework: it runs the real `index.html` inside a VM with a
small DOM stub, so the tests drive the shipped code rather than a copy of it. The
app exposes `App.__test` for this and nothing else uses it.

Covered: month coverage and act integrity, the weekday distribution, week and month
maths (including whole-day counts across a clock change), the Qurʾān data, the
`.ics` export down to line folding and escaping, backup and restore both ways, that
an existing install survives an update, and that every screen renders without
reaching for an element the markup does not have.

The suite has been mutation-checked: breaking the month ranges, the line folding,
the storage key, a text reference, a pinned weekday, the backup validation, the
journal escaping, the all-day event end date or the restore path each make it fail.

What the tests do not cover, and is still checked by hand in a browser: layout,
colour and contrast in both themes, and whether a download actually lands.

## Deploying

GitHub Pages serves `main` directly, so a push is the deploy. Bump `CACHE` in
`sw.js` in the same commit as any change to `index.html`.
