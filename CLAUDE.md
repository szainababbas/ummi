# Working on Ummi

## Always write tests

Every change comes with tests, in the same commit. No exceptions for "small"
changes or UI work: pull the logic out of the composable or render function
into something plain enough to test, then test that.

Before calling a test done, break the code it covers on purpose and check the
test fails (the README calls this mutation-checking). A test that passes
either way is not a test.

- **Web app** (`index.html`): `node tests/run.js`. Runs the real `index.html`
  in a VM through `App.__test`; see `tests/harness.js`.
- **Android app** (`android/`): `cd android && ./gradlew testDebugUnitTest`.
  JVM unit tests in `android/app/src/test`, for logic kept free of Android
  classes (`domain/`, `data/model/`, `data/AppState.kt`, `data/BackupCodec.kt`).
- **Both**: `tests/fixtures/backup-compat.json` is checked by both suites, so a
  backup from either app restores in the other. When the backup format or the
  readable record changes, update the fixture and make both suites pass.
  `tests/run.js` also fails if `android/app/src/main/assets/ummi-data.json`
  drifts from the constants in `index.html`.

**Screens:** `android/app/src/test/.../ui/screenshots/ScreenshotTest.kt` renders
every screen in light and dark with Paparazzi (no emulator). A new or changed
screen gets a snapshot there. CI uploads the images as the `screenshots`
artifact; look at them before calling UI work done, since a screen that
renders without crashing can still be unreadable.

CI runs both suites on every pull request (`.github/workflows/`).
