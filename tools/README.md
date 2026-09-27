# Qurʾān text tooling

The Qurʾān block in `index.html` is generated, not typed, so the Arabic and the
translation can be audited and rebuilt rather than trusted.

1. `fetch_quran.py` pulls the Uthmani Arabic, a transliteration, and the ʿAlī Qulī
   Qaraʾī and Muḥammad Sarwar translations from the alquran.cloud API into
   `quran_source.json`. Qaraʾī is the one used in the app: it is the standard Shiʿi
   English rendering, and the one al-islam.org publishes. Sarwar is kept in the
   source file as a second Shiʿi reading to check against.
2. `gen_quran.py` turns that into the `BISMILLAH` / `SURAHS` / `AYAHS` block.

The API's text is not taken on trust. `gen_quran.py` carries the fixes from the
27 September 2026 audit (see `docs/SOURCES.md`): `AR_FIXES` puts 16:69 in the muṣḥaf
spelling, `EN_FIXES` drops a closing quote mark that belongs to 16:68, `TL_FIXES`
replaces 24 verses whose transliteration had wrong words or odd splits, and
`REF_NAMES` spells the surah names the way the rest of the app does. It fails loudly
if a fix no longer matches the source, rather than silently dropping it.

The API prefixes the basmalah to the first verse of every surah except al-Fātiḥah
and at-Tawbah, and the prefixed copy is not byte-identical between surahs (95 and 97
carry an extra shadda, some carry a BOM). `gen_quran.py` strips it by comparing the
bare letters of the first four words and fails loudly if they do not match, rather
than silently leaving it in.

Which surahs and ayahs are included is set by the `SURAHS` and `AYAHS` lists at the
top of `fetch_quran.py`. The seven short surahs are the ones the planner has you
recite over a date, a fig or an egg, or inside a prayer. Longer surahs are referred
to by name and number only.

To rebuild:

    python fetch_quran.py     # writes quran_source.json
    python gen_quran.py       # writes gen_quran.js

then paste the generated block over the existing one in `index.html`.

## Android content

`export_android_data.js` writes `android/app/src/main/assets/ummi-data.json` and
`tests/fixtures/calendar-2026-12-15.ics` from `index.html`. Run it after changing any
content constant; `tests/run.js` fails until you do.

    node tools/export_android_data.js
