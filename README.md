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
- `tests/` — the test suite (for `index.html`, the web app).
- `android/` — a native Android app (Kotlin + Jetpack Compose), a from-scratch
  redesign rather than a wrapper around `index.html`. See "Android app" below.
- `design/handoff-android/` — the design handoff the Android app is built from
  (screen specs, tokens, and `ummi-data.json`, the content extracted from this
  repo's own `WEEKS`/`MONTHS`/etc. constants).

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

`android/` is a separate, native Android app — Kotlin, Jetpack Compose,
Material 3 — built from the redesign in `design/handoff-android/`, not a
WebView wrapper around `index.html`. The two share nothing at build time; they
share *content* (`android/app/src/main/assets/ummi-data.json` is a copy of
`design/handoff-android/ummi-data.json`, itself generated from this repo's own
`WEEKS`/`MONTHS`/etc. constants) and a *backup format* (see below), so someone
already using the web app can move their progress across.

Build it:

- **Android Studio** — open the `android/` folder, then Run.
- **Command line** — needs the Android SDK installed locally:
  `cd android && ./gradlew assembleDebug`, APK lands in
  `android/app/build/outputs/apk/debug/`.
- **CI, no local Android SDK needed** — `.github/workflows/android.yml` builds a
  debug APK on every push to `main` and on pull requests, and uploads it as a
  workflow artifact. This is also the first place the app actually gets
  compiled — this repo's own dev environment has no Android SDK and blocks
  `dl.google.com`, so the Gradle project here has not been built locally, only
  written carefully and sanity-checked by hand.

App id `com.szainabbas.ummi`. Fonts (Literata, Figtree, Amiri) are bundled from
google/fonts under `res/font/`; launcher icon and adaptive-icon background are
carried over from the icons already in this repo (`icon-512.png`).

**Backup / restore, and moving data from the web app:** the Android app
persists the same JSON shape the PWA writes with "Download backup"
(`{app: "ummi", schema, exportedAt, state}` — see `data/AppState.kt` /
`data/AppStateRepository.kt`), so a backup file downloaded from the browser
version restores directly in the Android app's More → Restore, and vice versa.

**What's not built yet** (tracked as follow-up PRs, per the handoff's own
"Screens" section): the 4-step onboarding (a minimal due-date prompt stands in
for it for now), Visits, Reminders/notifications, and the "Names we're
thinking about" wishlist. The 5-tab shell, Today, Journey, Duas and the rest
of More (settings, backup, reflection journal) are built. A few of the
handoff's exact "Material Symbols Rounded" icons aren't in the icon set this
app depends on (`material-icons-extended`) and use a close substitute instead
— noted in `ui/theme/UmmiIcons.kt`.

Release signing (a real keystore, not the debug one) is not set up yet — needed
before a Play Store submission, not for sideloading test builds.
