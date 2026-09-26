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
- `tools/` — the Qurʾān text generator and `sync-web.js`. See `tools/README.md`.
- `tests/` — the test suite.
- `android/` — a Capacitor wrapper around the same `index.html`, for a real,
  installable APK. See "Android app" below.

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

## Android app

`index.html` is still the one source of truth. `android/` is a
[Capacitor](https://capacitorjs.com) wrapper that loads a synced copy of it in a
native WebView, so it installs and runs like any other app instead of needing
"Add to Home screen" in Chrome.

    npm install
    npm run android:sync   # copies index.html, sw.js, manifest.json, icons into
                            # www/ and into android/app/src/main/assets/public

From there, either:

- **Android Studio** — `npm run android:open` (needs `ANDROID_HOME` set), or
  open the `android/` folder directly, then Run.
- **Command line** — needs the Android SDK installed locally:
  `cd android && ./gradlew assembleDebug`, APK lands in
  `android/app/build/outputs/apk/debug/`.
- **CI, no local Android SDK needed** — `.github/workflows/android.yml` builds a
  debug APK on every push to `main` and on pull requests, and uploads it as a
  workflow artifact.

Whenever `index.html`, `sw.js`, `manifest.json` or the icons change, run
`npm run android:sync` before rebuilding the Android app — it is a plain copy,
not automatic. App id is `com.szainabbas.ummi`; launcher icon and splash screen
are generated from `resources/icon.png` via `npx @capacitor/assets generate`.

Release signing (a real keystore, not the debug one) is not set up yet — needed
before a Play Store submission, not for sideloading test builds.
