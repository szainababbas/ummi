# Android app: where things stand

Last updated 27 September 2026. The day-by-day history is in `WORKLOG.md`.

## What's built

- **Onboarding**, in 4 steps: welcome, due date (or current week), name, reminders.
- **Today**
  - week ring and week strip;
  - Up next, with Done / Read / Later;
  - today's dua;
  - the day's tasks, grouped into Morning / After each prayer / Maghrib & ʿIshāʾ / Any time;
  - a bell on each task;
  - the next visit as a chip.
- **Journey:** Week, Month and Food tabs. Today links to a given week or to the Month tab.
- **Duas:** tabs, accordion, and the Qur'an reader.
- **Visits:** the next visit, what's coming up, past visits, and an add/edit form. Each visit can have a reminder.
- **More**
  - profile and the names wishlist;
  - Reminders & notifications;
  - theme and due date;
  - calendar export;
  - reflections;
  - backup, restore and the readable record.
- **Reminders:** morning summary, after Fajr / Ẓuhr / Maghrib, water, a time per task, and visit reminders.

## Where it differs from the design handoff, and why

- **No bell at the top of Today.** On the phone it read as "your notifications", not settings. Reminder settings live under More instead.
- **Prayer reminders come after Fajr, Ẓuhr and Maghrib only**, not all five prayers. They are never sent before 6:30 am or after 10 pm. In a London summer, Fajr can fall around 1 am.
- **The visit form has two extra fields**, "Where" and "To prepare / Notes". The handoff's visit card shows these, but its form had no way to enter them.
- **Prayer times use a city picked from a short list**, not the phone's location. That needs no permission. London is the default.
- **A few icons are close substitutes** where the exact Material Symbol isn't in the icon set. They are listed in `ui/theme/UmmiIcons.kt`.

## Known limits

- **Reminders can be a few minutes late.** Exact alarms need a permission Android keeps for alarm-clock apps.
- **Visits, names and reminders exist only in the Android app.** They travel in backups, and the web app keeps them if a backup passes through it.
- **No release signing yet.** It's needed before a Play Store release, not for installing test builds.

## Still to check on a phone

- The notification permission prompt at the end of onboarding.
- A reminder actually arriving, including after the phone restarts.
- Adding, editing and deleting a visit.
- Restoring a backup made in the browser.

## Getting a test build

- Every push builds an APK in CI. It's the `ummi-debug-apk` artifact on the run's page under GitHub Actions → "Android build".
- The cloud session can't download artifacts, so it can't send the file directly. It can give a direct download link that lasts about 10 minutes.
- A possible improvement: CI could attach each APK to a "Test build" pre-release, which the session can download and send as a file. It isn't set up, because it adds releases to the repo.

## Tests

`cd android && ./gradlew testDebugUnitTest`. CI runs these on every pull request. They cover:

- logic: prayer times, the reminder planner, visits, the day's plan, backups and the calendar;
- screenshots of every screen, light and dark (Paparazzi);
- tap-through tests of onboarding (Robolectric).

`tests/fixtures/backup-compat.json` is shared with the web tests, so backups work in both directions.
