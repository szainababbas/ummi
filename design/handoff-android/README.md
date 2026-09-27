# Handoff: Ummi — Android app redesign

## Overview
Ummi is a week-by-week pregnancy companion that combines NHS medical guidance with Shiʿi devotional practice (duas, Qurʾān recitation, month-by-month recommended acts from the Ahlulbayt (as)). The current product is a single-file PWA (`index.html` in `szainababbas/ummi`, branch `main`). This handoff covers a redesign for **Android** with:

- A new colour system with real **light and dark** themes (the old app had one dark mode and poor contrast)
- A consistent **Material Symbols Rounded** line-icon set in place of emoji
- **Arabic that wraps** across lines. The old app clipped it after one line.
- A restructured flow with a 5-tab bottom nav
- New features: **Visits** (appointments and scans), **Reminders** (daily + prayer-linked + per-task), and a **baby-names wishlist** in place of the old fixed names list
- A 4-step **onboarding**

## About the Design Files
The files in this bundle are **design references built in HTML**: prototypes that show the intended look and behaviour. They are not production code to copy. Rebuild them in the target codebase's own environment using its existing patterns. For Android that means Jetpack Compose + Material 3. If the team keeps the PWA, use its existing HTML/JS structure instead.

The `.dc.html` files are self-contained "design components". To view them, serve the folder over HTTP (e.g. `npx serve .`) and open `Ummi App.dc.html`. They need `support.js` (a runtime) and `ummi-data.json` (content) next to them. Each file has a template (HTML with `{{ }}` holes) and a `class Component` logic block at the bottom that shows the state and behaviour.

## Fidelity
**High-fidelity.** Colours, type, spacing, radii and interactions are final. Recreate them closely using Material 3 components where they match (NavigationBar, SegmentedButton, Switch, FilterChip, ModalBottomSheet, ExtendedFAB, Snackbar).

## Global layout
- Reference viewport: **412 × 892 dp** (Pixel-class). Content sits between the status bar and a bottom **NavigationBar**.
- Screen horizontal padding: **16dp** for cards, **20dp** for headings.
- Screen title: Literata 600, **26sp**, line-height 1.25, padding `8 20 12`.
- Section label ("eyebrow"): Figtree 700, **12sp**, uppercase, letter-spacing 0.08em, colour `accentText`.
- Standard card: `surface` background, 1dp `line` border, **20dp** radius, padding 16–18dp. List rows inside cards have a 1dp `line` bottom divider.
- Minimum touch target: 48dp (icon buttons are 48×48 circles; small in-row bells are 40×40).

### Bottom navigation (all screens except onboarding)
5 items: **Today** (`wb_sunny`), **Journey** (`pregnant_woman`), **Duas** (`menu_book`), **Visits** (`event`), **More** (`more_horiz`).
- Bar: background `nav`, 1dp top border `line`, padding `12 4 14`.
- Each item is 72dp wide, with a 56×32 pill indicator (radius 16) holding a 24sp icon above a 12sp label, 4dp apart.
- Active: pill `pc`, icon `onpc` and FILL 1, label `ink` weight 700. Inactive: transparent pill, icon and label `ink2`, weight 600, FILL 0. Background transition 200ms.

### Snackbar / toast
Bottom 12–16dp, inset 12dp each side. Background `ink`, text `bg`, radius 8, padding `14 16`, 14sp, shadow `0 4 16 rgba(0,0,0,.2)`. Optional leading icon (20sp) and an "Undo" action (Figtree 700 14sp, colour `accent`). Auto-dismisses after 2.8–3.2s.

### Reader bottom sheet (global)
Opened from any "Read" button. Scrim `rgba(10,12,10,.55)`. Sheet: `surface`, top radius 28, max-height 88%, 32×4 drag handle.
- Header: title Literata 600 20sp, subtitle 13sp `ink2`, and a 48dp close button on `surface2`.
- Bismillah (if the sūrah has it): Amiri 26sp, line-height 2, RTL, centred, `accentText`, with a bottom divider.
- Each verse: number badge (28dp circle, 1.5dp `accent` border, 12sp 700 `accentText`), then Arabic **Amiri 27sp, line-height 2.1, RTL, right-aligned, wrapping**, then transliteration (14sp italic `primary`), then English (15sp, line-height 1.6). Rows are divided by `line`.
- Footer source: 12sp italic `accentText` ("Translation: ʿAlī Qulī Qaraʾī").
- Keys: `surah:<no>`, `ayah:<s:v>`, `dua:<id>`. They resolve against `SURAHS`, `AYAHS` and `DUA_TEXTS` in `ummi-data.json`.

**Arabic rule (applies everywhere):** never clamp or ellipsize. Use line-height ≥ 2.0, add 2dp vertical padding so the diacritics don't clip, set RTL direction, and use balanced or pretty wrapping. This fixes the main complaint about the old app.

## Screens

### 1. Onboarding (`Screen Onboarding.dc.html`)
Full screen, no bottom nav. Top row (min 48dp): a back arrow (steps 2–4) and step dots on the right. The current dot is 20×6 and the others are 6×6, radius 3. Dots at or before the current step are `primary`, the rest `line`, with a 200ms width transition. Content has 24dp horizontal padding. The footer has a 56dp full-width pill button (`primary`/`onp`, 16sp 600).
1. **Welcome:** the 96dp app icon (radius 24), "Ummi" in Literata 600 40sp, the tagline "A week-by-week pregnancy companion rooted in science and the teachings of the Ahlulbayt (as)." (17sp, `ink2`, line-height 1.6), then the Arabic رَبِّ هَبْ لِي مِنَ الصَّالِحِينَ (Amiri 26sp, `accentText`) with its translation in 14sp italic. CTA "Begin the journey", with the footnote "Free, always. Nothing leaves your device."
2. **Due date:** heading "When is your baby due?" (Literata 600 28sp) and a 2-segment control, Due date / Current week (40dp tall, radius 20, selected `pc`/`onpc`). Due date mode shows a date field (56dp, radius 12, 1.5dp `line` border). Current week mode shows a −/+ stepper with the week number in Literata 40sp, clamped 4–41. A summary chip below (`pc`/`onpc`, radius 16) reads "That's week N, Xth trimester. Welcome." Weeks are computed as `40 − ceil(daysUntilDue/7)`.
3. **Name:** "What should we call you?" is optional. The input is 56dp. The CTA reads "Skip" when empty and "Continue" when filled. A lock note reads "Nothing leaves your device. No account, no server."
4. **Reminders:** a 56dp `ac` circle with a bell, the heading "Gentle reminders?", and a checklist card with three items: Morning summary (on), Prayer-linked acts (on), Water (off). Checkboxes are 24dp, radius 6, `primary` when checked. CTA "Allow notifications" (this should trigger the Android POST_NOTIFICATIONS permission), with a secondary text button "Not now". Both finish onboarding and go to Today.

### 2. Today (`Screen Today.dc.html`)
Scrolls vertically. Top to bottom:
- **Header row:** "Salaam, Fatima" (16sp 600) and a 48dp bell button (`surface2`) with a badge (18dp, `primary`/`onp`, 11sp 700) showing the count of active reminders. Tapping it opens Reminders.
- **Progress ring:** 196dp with a 12dp stroke. The track is `surface2`, progress is `primary` with a round cap, and it starts at 12 o'clock. Progress is week/40 (24/40 → dasharray 324.2/541). Centre: "WEEK" eyebrow, the number in Literata 600 64sp, and "105 days to go" 13sp `ink2`. Below it: "About the size of a corn" (Literata 17sp).
- **Week strip:** horizontal scroll of 40dp circles, 6dp gap, auto-centred on the current week at load. Current week is `primary`/`onp`. Past weeks are `surface2` fill with `ink2` text. Future weeks are transparent with a `line` border. Tapping one opens Journey › Week at that week. Caption below: "Tap a week to read about it" (12sp).
- **Meta line:** "2nd trimester · Due ≈ 9 Jan" (13sp `ink2`, 3dp dot separators) plus a next-visit chip (28dp, `ac`/`accentText`, `event` icon, e.g. "Tue 9:10 · Glucose test"). The chip opens Visits.
- **Up next card:** `surface`, **1.5dp `primary` border**, radius 24, padding 18.
  - Top row: "UP NEXT · N OF 7" and 7 progress segments (14×4, radius 2, `primary` when done, `line` otherwise).
  - Slot label (icon + text, `accentText`), the title in Literata 600 21sp, and an optional subtitle.
  - Buttons (48dp pills): **Done** (`primary`, check icon, flex 1), **Read** (`pc`, only when the act has a text), **Later** (outlined). Later cycles to the next open item.
  - When everything is done the card reads "Today complete" and "Everything for today is ticked off. Alhamdulillah."
- **Today's dua band:** full-bleed `ac` background, padding `24 20`. Eyebrow "Today's dua · Protection for mother & child". Arabic in Amiri 30sp, line-height 2, centred, balanced wrap. Transliteration 15sp italic `accentText`, then the English translation 15sp. Toggle button (44dp): "Mark as recited" (outlined `accentText`, 1.5dp) ↔ "✓ Recited today" (`primary` fill).
- **Your day:** heading (Literata 600 20sp) with "N of 7" on the right. A timeline laid out as a 52dp time column (time 13sp 700 / am-pm 11sp / 2dp `line` vertical rule) beside the content. Groups:
  - Morning (`wb_twilight`, 5:24 am)
  - After each prayer (`mosque`)
  - Maghrib & ʿIshāʾ (`nights_stay`, 6:52 pm)
  - Any time today (`schedule`)

  Each task is a card (`surface`, radius 16, 1dp border, `primary` border if it's the current Up next item) containing:
  - A 24dp check circle: empty with a 2dp `accent` ring, or `primary` filled with a check.
  - Title 15sp 600 and subtitle 13sp. Done tasks show at 55% opacity with a line-through.
  - If a reminder is set, a small line in 12sp `accentText` with a filled bell and the time.
  - An optional inline "Read" chip (28dp, `pc`).
  - A trailing 40dp bell: `notifications_active` on an `ac` circle when set, `notification_add` in `ink2` when not. Toggling it shows a snackbar ("Reminder set · 7:30 am" / "Reminder off") with Undo.
- Footer link "Month 6 of the guide →" (14sp 700 `primary`) opens Journey › Month.

### 3. Journey (`Screen Journey.dc.html`)
Title "Your journey" and a 3-segment control, **Week | Month | Food** (40dp, radius 20, selected `pc`/`onpc`).
- **Week:** prev/next 48dp circular outlined buttons around "Week N" (Literata 600 22sp) and the subtitle "2nd trimester · this week". Cards:
  - **Baby this week:** a size sentence in Literata 600 20sp, a 2-column stat grid (Length / Weight, `surface2`, radius 14), and an optional milestone chip (30dp, `pc`, `flag` icon).
  - **Baby's development:** bullet list with 6dp `accent` dots, 15sp, line-height 1.55.
  - **Your body:** bullet list plus a "What's normal:" callout (`pc`/`onpc`, radius 14).
  - **Faith focus** and **This week's tip:** text cards.
  - When you're not on the current week, a "Back to week 24" text button shows. The range is weeks 4–41.
- **Month:** prev/next around "Month N" with the subtitle "Weeks A–B". A "What's happening" card, then one card per category (Recitation `auto_stories`, Sūrah `menu_book`, Food `nutrition`, Wellness `spa`). Each act shows its title (15sp 600), a note (13sp), a day chip ("Every day" or e.g. "Mon · Thu", 28dp `surface2`) and a "Read" chip when it links to a text. Then a Physical activity card and a sources footnote (13sp `ink2`).
- **Food:** three list cards under Literata 19sp headings: "From the Ahlulbayt (as)", "Everyday essentials", "Best avoided" (with a `block` icon in `danger`). A sources footnote follows.

### 4. Duas (`Screen Duas.dc.html`)
Title "Duas & Qurʾān". A horizontally scrolling row of pill tabs (36dp, radius 18): Pregnancy, Easy delivery, After birth, Daily dhikr, Qurʾān. Selected is `primary`/`onp`; unselected is `surface` with a `line` border.
- **Dua tabs:** an accordion of cards (radius 20). The collapsed header shows the title (Literata 600 17sp), a "when" line with a `schedule` icon (13sp `ink2`), and a chevron that rotates 180° over 200ms. Expanded, it shows the Arabic (Amiri 26sp, lh 2, centred, balanced), the transliteration (15sp italic `primary`, centred), the body (15sp) and the source (12sp italic `accentText`). One item is open at a time. A sources footnote sits under the list.
- **Qurʾān tab:** a "Short sūrahs in full" list card. Each row has a 36dp number circle, the name and "meaning · N āyāt", the Arabic name in Amiri 20sp and a chevron, and opens the reader. Below it, "Āyāt the guide asks for" is a wrap of outlined 36dp chips (e.g. `2:255`), each opening the reader.

### 5. Visits (`Screen Visits.dc.html`), new
Title "Visits" with the subtitle "Appointments and scans".
- **Next visit hero:** gradient `heroA`→`heroB` at 160°, radius 24, padding 20, text `heroInk`/`heroInk2`.
  - Eyebrow "NEXT · IN 3 DAYS", title in Literata 600 24sp, and date and location rows with 18sp icons.
  - A "To prepare" panel (rgba(255,255,255,.1), radius 16).
  - A reminder row with a custom switch (52×32 track, 24dp knob, 200ms). Toggling it shows a snackbar.
- **Coming up:** list card. Each row has a 48dp date tile (`surface2`, month 11sp uppercase `accentText`, day 18sp 700), the title and "type · time · reminder …", and a trailing filled bell (`accentText` if set, `ink2` / `notifications_off` if not). Sorted by date.
- **Info card** (`ac`): "Typical NHS schedule, first pregnancy". Midwife checks are usually offered at 25, 28, 31, 34, 36, 38 and 40 weeks.
- **Past:** outlined date tiles, with the note shown in quotes and italic.
- **ExtendedFAB** "Add visit" (bottom-right 16dp, 56dp tall, radius 16, `pc`/`onpc`, shadow). It opens a **bottom sheet** (radius 28) containing:
  - Type chips: Midwife `stethoscope`, Scan `monitor_heart`, Blood test `bloodtype`, GP `medical_services`, Consultant `person`, Other `more_horiz`.
  - A title input (52dp).
  - Date and time inputs side by side.
  - Reminder chips: None, Evening before, Morning of, 2 hours before.
  - Cancel / Save buttons (48dp). Save is disabled at 50% opacity until there's a title. Saving adds the visit to Coming up and shows a snackbar.

### 6. More (`Screen More.dc.html`)
- **Profile card:** 48dp avatar initial (`pc`), name, "Due 9 January 2027 · week 24", and an Edit pill.
- **Names we're thinking about** (replaces the old fixed names list): an input with a 48dp `primary` add button (Enter also adds). New names go to the top. Each row has the name (Literata 600 17sp), an optional note, a heart toggle (`danger`, FILL 1 when favourited) and a remove button. Footnote: "Only on this phone. Tap the heart on the ones you both love."
- **Settings:**
  - Reminders (opens the Reminders screen).
  - **Appearance:** a 3-segment Light / Dark / System control that changes the theme live.
  - Due date.
  - Export to calendar (.ics), which shows a snackbar.
- **A gentle reflection:** a one-line input with Save. The last 4 entries show below with the date in `primary` bold.
- **Backup:** Download backup (shows "Last: 19 Sep"), Restore, and Download a readable record. Then a "Reset app" text button in `danger`, and the medical/marjaʿ disclaimer (12sp centred).

### 7. Reminders (`Screen Reminders.dc.html`), new
Top app bar: back arrow and "Reminders" (Literata 600 22sp). A status card (`pc`) reads "Notifications are allowed. Prayer times are worked out for London." with a "Change" action.
- **Every day:** Morning summary (7:30 am), Prayer-linked acts (a few minutes after each adhān), and Water (every 2 hours, 9 am–7 pm).
- **Today · Friday:** each task with a time column (56dp, 14sp 700 `accentText`).
- **Visits:** upcoming visit reminders.

Switches follow the Material 3 style: a 52×32 track with a 2dp border. **On:** `primary` track, 24dp `onp` knob. **Off:** `surface2` track with an `ink2` border and a 16dp `ink2` knob.

## Interactions & navigation
- The bottom nav switches between top-level screens. Reminders is a pushed screen and back returns to where you came from.
- From Today: bell → Reminders, visit chip → Visits, week circle → Journey/Week(n), "Month 6 of the guide" → Journey/Month, Read → reader sheet.
- Checking a task updates the Up next card, the segment bar and the "N of 7" count. Later skips to the next open task and wraps around.
- A reminder toggle shows a snackbar with Undo.
- Theme changes apply app-wide straight away. System should follow `isSystemInDarkTheme()`.
- Transitions: 150–200ms on switches, pills, chevrons and the dots. Use the M3 standard easing.

## State
- `profile`: name, dueDate. Derived from it: currentWeek, trimester, daysToGo, currentMonth (1–9).
- `dailyDone[date]`: set of task ids. `skipped` is session-only.
- `duaRecited[date]`: boolean.
- `reminders`: per-task on/off with a time. Global toggles: morning summary, prayer-linked, water.
- `visits[]`: { date, time, title, type, reminder, note? }.
- `names[]`: { name, note, favourite }.
- `journal[]`: { date, text }.
- `theme`: light | dark | system.
- All of it stays on the device (the current app uses localStorage; on Android use DataStore or Room). Backup and restore are JSON export/import.
- Prayer times need a location and a calculation method (the old app already has a prayer-time calculation for this). Prayer-linked reminders are scheduled relative to adhān times. On Android use AlarmManager exact alarms or WorkManager.

## Design tokens

### Light
| token | hex | use |
|---|---|---|
| bg | #F6F1E7 | app background |
| surface | #FFFDF8 | cards, sheets |
| surface2 | #EEE7D8 | tonal fills, tracks |
| line | #E0D6C2 | borders, dividers |
| ink | #1E1C18 | primary text |
| ink2 | #5A5446 | secondary text |
| primary | #2D4A3E | primary actions, ring |
| onp | #FFFFFF | on primary |
| pc | #DCE8E0 | primary container |
| onpc | #16332A | on primary container |
| accent | #A07A45 | gold accents, bullets, rings |
| accentText | #6E4F24 | gold text (AA on bg) |
| ac | #F2E6CC | accent container (dua band, chips) |
| heroA / heroB | #2D4A3E / #1C342B | visit hero gradient |
| heroInk / heroInk2 | #FFFFFF / #EBDDBB | text on hero |
| nav | #EFE8DA | bottom nav |
| danger | #A5473A | destructive, favourite heart |

### Dark
| token | hex |
|---|---|
| bg | #0F1411 |
| surface | #171E1A |
| surface2 | #1F2823 |
| line | #2A342E |
| ink | #ECE6D9 |
| ink2 | #AEB5AB |
| primary | #A3CBB5 |
| onp | #0D2A1E |
| pc | #26463A |
| onpc | #D2E8DB |
| accent | #D6B478 |
| accentText | #E4C990 |
| ac | #352C1C |
| heroA / heroB | #21443A / #162D25 |
| heroInk / heroInk2 | #F4EFE4 / #E4C990 |
| nav | #151B18 |
| danger | #E39A8A |

### Typography
- **Literata** (serif, opsz 7–72, weights 400/600): headings, titles, numbers. Sizes 64 (ring), 40, 28, 26, 24, 22, 21, 20, 19, 17.
- **Figtree** (400/500/600/700): UI and body. Body 15/1.55–1.6, secondary 13–14, labels 12 (700, uppercase, 0.08em tracking), nav 12.
- **Amiri** (400/700): all Arabic. 26–30sp with line-height 2.0–2.1.
- **Material Symbols Rounded**: icons at 14–28sp. FILL 0 by default, FILL 1 for active or selected states.

### Radii
6 (checkbox) · 8 (snackbar) · 10–12 (chips, inputs) · 14–16 (inner panels, task cards, FAB) · 18–20 (cards, pills, segmented) · 24 (hero, Up next card) · 28 (sheet tops) · full circles for icon buttons.

### Spacing
4 · 6 · 8 · 10 · 12 · 14 · 16 · 18 · 20 · 24 dp. Cards are 16dp from the screen edge with 12dp between them. Sections are 20dp apart.

### Elevation
Mostly flat, using borders instead of shadows. Shadows only on the FAB (`0 3 10 rgba(0,0,0,.18)`) and the snackbar (`0 4 16 rgba(0,0,0,.2)`).

## Assets
- `icon-192.png`: the existing app icon (from the repo).
- Fonts: Google Fonts (Literata, Figtree, Amiri, Material Symbols Rounded). Bundle them for offline use in the app.
- `ummi-data.json`: content taken from the repo's `index.html` constants: `WEEKS` (4–41, NHS), `MONTHS` (1–9 acts, from the Shiʿi pregnancy planner), `FOOD`, `DUAS`, `DUA_TEXTS`, `DUA_TODAY`, `AYAHS`, `BISMILLAH`, and 7 `SURAHS` (1, 95, 97, 103, 108, 110, 112) with Uthmani Arabic and the Qaraʾī translation. In production, keep using the repo's own data as the source of truth.
- Sample data in the prototype (not real): the user name "Fatima", prayer times, visits, names, journal entries and backup dates.

## Files
- `Ummi App.dc.html`: the overview canvas. Two interactive phones (light, dark) and every screen in both themes. **Start here.**
- `Ummi.dc.html`: the app shell. Theme tokens, bottom nav, routing, reader sheet, snackbar.
- `Screen Onboarding.dc.html`, `Screen Today.dc.html`, `Screen Journey.dc.html`, `Screen Duas.dc.html`, `Screen Visits.dc.html`, `Screen More.dc.html`, `Screen Reminders.dc.html`: one file per screen.
- `android-frame.jsx`: the device frame used only in the preview. Not part of the app.
- `support.js`: runtime needed to open the `.dc.html` files. Not part of the app.
- `ummi-data.json`: content data.
