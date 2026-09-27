/* Writes android/app/src/main/assets/ummi-data.json from the constants in
   index.html, so the Android app's copy of the content never has to be edited by
   hand, and tests/fixtures/calendar-2026-12-15.ics, which both suites check the
   calendar export against and which carries the content too. tests/run.js fails if
   either drifts from index.html; run this to bring them back.

       node tools/export_android_data.js
*/
const fs = require('fs');
const path = require('path');
const { load, ROOT } = require('../tests/harness');

const KEYS = ['WEEKS', 'MONTHS', 'FOOD', 'DUAS', 'DUA_TEXTS', 'DUA_TODAY', 'BISMILLAH', 'AYAHS', 'SURAHS'];

function calendar() {
  const { T } = load();
  T.setState({ dueDate: '2026-12-15' });
  return T.buildICS().replace(/^DTSTAMP:.*$/gm, 'DTSTAMP:20260926T000000Z');
}

function build() {
  const { data, T } = load();
  const out = {};
  for (const k of KEYS) out[k] = data[k];
  // BASE_TASKS is private to the App closure; tasksToday() is the month's acts followed by it
  out.BASE_TASKS = T.tasksToday().slice(T.todaysActs().length);
  return JSON.stringify(out);
}

if (require.main === module) {
  const file = path.join(ROOT, 'android/app/src/main/assets/ummi-data.json');
  fs.writeFileSync(file, build());
  console.log('wrote ' + path.relative(ROOT, file));
  const ics = path.join(ROOT, 'tests/fixtures/calendar-2026-12-15.ics');
  fs.writeFileSync(ics, calendar());
  console.log('wrote ' + path.relative(ROOT, ics));
}

module.exports = { build, calendar };
