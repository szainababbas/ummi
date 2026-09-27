/* Ummi test suite. Run with: node tests/run.js
   No dependencies and no framework, so it works on any machine with node. */
const { load } = require('./harness');

let passed = 0;
const failures = [];
let group = '';

function describe(name, fn) { group = name; fn(); }
function it(name, fn) {
  try { fn(); passed++; }
  catch (e) { failures.push({ name: group + ' > ' + name, err: e }); }
}
function eq(actual, expected, msg) {
  const a = JSON.stringify(actual), b = JSON.stringify(expected);
  if (a !== b) throw new Error((msg ? msg + ': ' : '') + 'expected ' + b + ', got ' + a);
}
function ok(cond, msg) { if (!cond) throw new Error(msg || 'expected truthy'); }
function throws(fn, msg) {
  let threw = false;
  try { fn(); } catch (e) { threw = true; }
  if (!threw) throw new Error(msg || 'expected a throw');
}

const DUE = '2026-11-20';            // Aimen's due date

/* A date n days from today as YYYY-MM-DD in *local* time. toISOString would give UTC,
   which is a day out whenever the machine is on BST, and would make these tests lie. */
function dueInDays(n) {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  d.setDate(d.getDate() + n);
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
}
const app = load({ localStorage: { ummi_v1: JSON.stringify({ name: 'Aimen', dueDate: DUE }) } });
const { T, data } = app;
const { MONTHS, SURAHS, AYAHS, WEEKS, DUA_TEXTS } = data;

/* ------------------------------------------------------------------ month data */
describe('month data', () => {
  it('covers weeks 1 to 41 exactly once', () => {
    for (let w = 1; w <= 41; w++) {
      const hits = Object.keys(MONTHS).filter((m) => w >= MONTHS[m].from && w <= MONTHS[m].to);
      eq(hits.length, 1, 'week ' + w + ' covered by ' + hits.length + ' months');
    }
  });

  it('has nine months, contiguous and in order', () => {
    eq(Object.keys(MONTHS).length, 9);
    for (let m = 1; m <= 9; m++) {
      ok(MONTHS[m].to >= MONTHS[m].from, 'month ' + m + ' range inverted');
      if (m > 1) eq(MONTHS[m].from, MONTHS[m - 1].to + 1, 'gap or overlap before month ' + m);
    }
  });

  it('gives every act a unique id', () => {
    const seen = new Set();
    for (const m of Object.keys(MONTHS)) {
      for (const a of MONTHS[m].acts) {
        ok(!seen.has(a.id), 'duplicate act id ' + a.id);
        seen.add(a.id);
      }
    }
    ok(seen.size > 60, 'expected the full planner, got only ' + seen.size + ' acts');
  });

  it('never collides an act id with a baseline task id', () => {
    // both land in the same state.done array, so a collision would tick two things at once
    const base = ['salah', 'water', 'walk'];
    for (const m of Object.keys(MONTHS)) {
      for (const a of MONTHS[m].acts) ok(!base.includes(a.id), a.id + ' collides with a baseline task');
    }
  });

  it('uses only real weekdays', () => {
    for (const m of Object.keys(MONTHS)) {
      for (const a of MONTHS[m].acts) {
        if (a.days === null) continue;
        ok(Array.isArray(a.days) && a.days.length > 0, a.id + ' has an empty days array');
        for (const d of a.days) ok(Number.isInteger(d) && d >= 0 && d <= 6, a.id + ' has weekday ' + d);
      }
    }
  });

  it('points every text reference at something that exists', () => {
    let refs = 0;
    for (const m of Object.keys(MONTHS)) {
      for (const a of MONTHS[m].acts) {
        if (a.surah !== undefined) { refs++; ok(SURAHS[a.surah], a.id + ' references missing surah ' + a.surah); }
        if (a.ayah !== undefined) { refs++; ok(AYAHS[a.ayah], a.id + ' references missing ayah ' + a.ayah); }
        if (a.dua !== undefined) { refs++; ok(DUA_TEXTS[a.dua], a.id + ' references missing dua ' + a.dua); }
      }
    }
    ok(refs >= 20, 'expected the texts to be wired up, found only ' + refs + ' references');
  });

  it('gives every month prose and every act a category', () => {
    const cats = ['Recitation', 'Sūrah', 'Food', 'Wellness'];
    for (const m of Object.keys(MONTHS)) {
      ok(MONTHS[m].note && MONTHS[m].note.length > 40, 'month ' + m + ' note too thin');
      ok(MONTHS[m].activity && MONTHS[m].activity.length > 20, 'month ' + m + ' activity missing');
      ok(MONTHS[m].acts.length > 0, 'month ' + m + ' has no acts');
      for (const a of MONTHS[m].acts) {
        ok(cats.includes(a.cat), a.id + ' has unknown category ' + a.cat);
        ok(a.t && a.t.length > 3, a.id + ' has no title');
      }
    }
  });
});

/* --------------------------------------------------------------- week and month */
describe('week and month maths', () => {
  it('maps weeks onto the right month', () => {
    const cases = [[1, 1], [4, 1], [5, 2], [8, 2], [9, 3], [12, 3], [13, 4], [16, 4],
                   [17, 5], [20, 5], [21, 6], [24, 6], [25, 7], [28, 7], [29, 8], [32, 8], [33, 9], [41, 9]];
    for (const [w, m] of cases) eq(T.monthOf(w), m, 'week ' + w);
  });

  it('clamps out-of-range weeks to the weeks the app has data for', () => {
    eq(T.clampWeek(0), 4);
    eq(T.clampWeek(1), 4);
    eq(T.clampWeek(99), 41);
    eq(T.clampWeek(20), 20);
    for (let w = 4; w <= 41; w++) ok(WEEKS[w], 'no WEEKS entry for week ' + w + ', which clampWeek can return');
  });

  it('reads the current week off the due date', () => {
    // 280 days gestation: a due date 98 days out is 182 days gone, which is week 26
    T.setState({ dueDate: dueInDays(98) });
    eq(T.currentWeek(), 26);
    eq(T.daysToGo(), 98);
  });

  it('does not go negative once the due date has passed', () => {
    T.setState({ dueDate: dueInDays(-10) });
    eq(T.daysToGo(), 0, 'days to go should floor at zero');
    eq(T.currentWeek(), 41, 'past the due date should clamp to the last week with data');
  });

  it('counts whole days across a clock change', () => {
    // UK clocks go back on 25 Oct 2026, three weeks before Aimen is due. Counting in
    // milliseconds across that gives 97.96 days, which must not truncate to 97.
    for (let n = 1; n <= 120; n++) {
      T.setState({ dueDate: dueInDays(n) });
      eq(T.daysToGo(), n, 'day count drifted at ' + n + ' days out (' + dueInDays(n) + ')');
    }
  });

  it('names the trimesters at the boundaries', () => {
    eq(T.trimester(13), 'First'); eq(T.trimester(14), 'Second');
    eq(T.trimester(27), 'Second'); eq(T.trimester(28), 'Third');
  });
});

/* ----------------------------------------------------------- daily distribution */
describe('daily distribution', () => {
  it('shows an everyday act on every day of the week', () => {
    const everyday = MONTHS[1].acts.filter((a) => a.days === null).map((a) => a.id);
    for (let d = 0; d < 7; d++) {
      const ids = T.actsFor(1, d).map((a) => a.id);
      for (const id of everyday) ok(ids.includes(id), 'act ' + id + ' missing on weekday ' + d);
    }
  });

  it('shows a pinned act only on its own days', () => {
    // month 8 is the fully day-split month: one surah per weekday
    const byDay = { 0: 'm8c', 1: 'm8d', 2: 'm8e', 3: 'm8f', 4: 'm8g', 5: 'm8h', 6: 'm8b' };
    for (let d = 0; d < 7; d++) {
      const ids = T.actsFor(8, d).map((a) => a.id);
      ok(ids.includes(byDay[d]), 'weekday ' + d + ' should include ' + byDay[d]);
      for (const [other, id] of Object.entries(byDay)) {
        if (Number(other) !== d) ok(!ids.includes(id), 'weekday ' + d + ' should not include ' + id);
      }
    }
  });

  it('puts the Friday pomegranate on Friday only', () => {
    ok(T.actsFor(8, 5).some((a) => a.id === 'm8j'), 'missing on Friday');
    for (const d of [0, 1, 2, 3, 4, 6]) ok(!T.actsFor(8, d).some((a) => a.id === 'm8j'), 'present on weekday ' + d);
  });

  it('never hands back an empty day', () => {
    for (let m = 1; m <= 9; m++) {
      for (let d = 0; d < 7; d++) ok(T.actsFor(m, d).length > 0, 'month ' + m + ' weekday ' + d + ' is empty');
    }
  });

  it('leads the day with a pinned act when there is one', () => {
    const thursday = new Date('2026-08-13T00:00:00');   // a Thursday
    eq(thursday.getDay(), 4);
    const lead = T.featuredAct(1, thursday, 30);
    eq(lead.days, [4, 5], 'month 1 Thursday should lead with the Thursday/Friday surah');
  });

  it('rotates the lead act rather than repeating one all month', () => {
    const monday = new Date('2026-08-10T00:00:00');     // month 1 has nothing pinned to Monday
    eq(monday.getDay(), 1);
    const leads = new Set();
    for (let day = 0; day < 28; day++) leads.add(T.featuredAct(1, monday, day).id);
    ok(leads.size > 1, 'the lead act never changed across 28 days');
  });

  it('always leads with an act that actually applies that day', () => {
    for (let m = 1; m <= 9; m++) {
      for (let day = 0; day < 40; day++) {
        const date = new Date(2026, 7, 2 + day);   // walks through every weekday
        const lead = T.featuredAct(m, date, day);
        const ids = T.actsFor(m, date.getDay()).map((a) => a.id);
        ok(lead && ids.includes(lead.id), 'month ' + m + ' day ' + day + ' led with an act not valid today');
      }
    }
  });

  it('adds the baseline tasks to whatever the month asks for', () => {
    T.setState({ dueDate: DUE });
    const ids = T.tasksToday().map((t) => t.id);
    for (const b of ['salah', 'water', 'walk']) ok(ids.includes(b), 'baseline task ' + b + ' missing');
    ok(ids.length > 3, 'no month acts came through');
    eq(ids.length, new Set(ids).size, 'the checklist contains a duplicate id');
  });
});

/* ------------------------------------------------------------------ qur'an data */
describe("qur'an data", () => {
  it('has the expected verse counts', () => {
    const counts = { 1: 7, 95: 8, 97: 5, 103: 3, 108: 3, 110: 3, 112: 4 };
    for (const [n, c] of Object.entries(counts)) {
      ok(SURAHS[n], 'surah ' + n + ' missing');
      eq(SURAHS[n].v.length, c, 'surah ' + n + ' verse count');
    }
  });

  it('numbers verses from 1 with no gaps', () => {
    for (const n of Object.keys(SURAHS)) {
      SURAHS[n].v.forEach((v, i) => eq(v.n, i + 1, 'surah ' + n + ' verse numbering'));
    }
  });

  it('carries arabic, transliteration and translation on every verse', () => {
    for (const n of Object.keys(SURAHS)) {
      for (const v of SURAHS[n].v) {
        ok(v.ar && v.ar.length > 2, 'surah ' + n + ':' + v.n + ' has no arabic');
        ok(v.tl && v.tl.length > 2, 'surah ' + n + ':' + v.n + ' has no transliteration');
        ok(v.en && v.en.length > 2, 'surah ' + n + ':' + v.n + ' has no translation');
      }
    }
    for (const r of Object.keys(AYAHS)) {
      const a = AYAHS[r];
      ok(a.ar && a.tl && a.en && a.ref, 'ayah ' + r + ' incomplete');
    }
  });

  it('strips the basmalah out of the first verse, except in al-Fatiha', () => {
    // the API prefixes it; leaving it in would print it twice, since the app adds it separately
    for (const n of Object.keys(SURAHS)) {
      if (n === '1') continue;
      const first = SURAHS[n].v[0].ar;
      ok(!/^بِّ?سْمِ/.test(first), 'surah ' + n + ' verse 1 still starts with the basmalah');
    }
    ok(/^﻿?بِسْمِ/.test(SURAHS[1].v[0].ar) || SURAHS[1].v[0].ar.includes('بِسْمِ'),
      'al-Fatiha verse 1 should still be the basmalah');
  });

  it('has no mojibake or byte-order marks left in the text', () => {
    const bad = /[�﻿]/;
    for (const n of Object.keys(SURAHS)) {
      for (const v of SURAHS[n].v) ok(!bad.test(v.ar + v.tl + v.en), 'surah ' + n + ':' + v.n + ' has a replacement char or BOM');
    }
    for (const r of Object.keys(AYAHS)) ok(!bad.test(AYAHS[r].ar + AYAHS[r].tl + AYAHS[r].en), 'ayah ' + r + ' has a replacement char or BOM');
  });

  it('carries the dhikr texts the months reference', () => {
    for (const k of ['salawat', 'salawat-long', 'istighfar', 'tasbih']) {
      ok(DUA_TEXTS[k], 'missing dhikr ' + k);
      ok(DUA_TEXTS[k].ar && DUA_TEXTS[k].tl && DUA_TEXTS[k].en && DUA_TEXTS[k].sr, k + ' incomplete');
    }
  });
});

/* The 2026-09-27 research audit (docs/SOURCES.md) checked the Qurʾān block against
   Tanzil and quran.com, and the duʿāʾ cards against Qarāʾī. These hold those fixes. */
describe('texts, as checked against their sources', () => {
  it('spells 16:69 as the Madinah muṣḥaf does', () => {
    ok(AYAHS['16:69'].ar.includes('لَـَٔايَةًۭ'), '16:69 should seat the hamza on a tatweel, as Tanzil and quran.com do');
    ok(!AYAHS['16:69'].ar.includes('لَءَايَةً'), '16:69 still has the API spelling');
  });

  it('names each surah the way the rest of the app does', () => {
    const names = { 2: 'al-Baqarah', 3: 'Āl ʿImrān', 14: 'Ibrāhīm', 16: 'an-Naḥl', 19: 'Maryam', 20: 'Ṭā Hā',
      25: 'al-Furqān', 36: 'Yāsīn', 37: 'aṣ-Ṣāffāt', 46: 'al-Aḥqāf', 94: 'ash-Sharḥ' };
    for (const r of Object.keys(AYAHS)) {
      eq(AYAHS[r].ref, 'Sūrah ' + names[r.split(':')[0]] + ' ' + r, 'ref label for ' + r);
    }
  });

  it('has none of the transliteration slips the API ships with', () => {
    // each of these is a wrong word, not a style choice: see tools/gen_quran.py TL_FIXES
    const slips = ['mww', "waqa'athaa", 'Summma', "aqzi'neee", "b'adu", 'alfee', 'afwajah', 'was taghfir', 'Hunaaalika'];
    const all = [];
    for (const n of Object.keys(SURAHS)) for (const v of SURAHS[n].v) all.push(n + ':' + v.n + ' ' + v.tl);
    for (const r of Object.keys(AYAHS)) all.push(r + ' ' + AYAHS[r].tl);
    for (const line of all) for (const s of slips) ok(!line.includes(s), 'transliteration slip "' + s + '" in ' + line);
    ok(SURAHS[110].v[2].tl.includes('wastaghfirhu'), '110:3 should keep the pronoun: wastaghfirhu');
  });

  it("gives every duʿāʾ card the Qarāʾī meaning of the verse it quotes", () => {
    // the app says its translation is Qarāʾī, so the cards must not carry another one
    const flat = (x) => x.replace(/[‘’“”'"]/g, '').replace(/[—–]/g, ' ').replace(/\s+/g, ' ').trim();
    for (const d of data.DUA_TODAY) {
      const m = /(\d+):(\d+)(?:–(\d+))?/.exec(d.sr);
      ok(m, d.lbl + ' has no verse reference');
      let en = '';
      for (let v = +m[2]; v <= +(m[3] || m[2]); v++) {
        const a = AYAHS[m[1] + ':' + v];
        ok(a, d.lbl + ' quotes ' + m[1] + ':' + v + ', which the app does not carry');
        en += ' ' + a.en;
      }
      const quoted = /“([^”]+)”/.exec(d.mn);
      ok(quoted, d.lbl + ' has no quoted meaning');
      ok(flat(en).includes(flat(quoted[1])), d.lbl + ': meaning is not Qarāʾī for ' + m[0] + ': ' + quoted[1]);
    }
  });

  it('shows all the Arabic that each duʿāʾ card translates', () => {
    // three cards used to translate the end of the verse without showing it
    const ends = { 'For a righteous child': 'الدُّعَاءِ', 'Comfort of the eyes': 'إِمَامًا', 'Steadfast in prayer': 'دُعَاءِ' };
    for (const d of data.DUA_TODAY) if (ends[d.lbl]) ok(d.arabic.endsWith(ends[d.lbl]), d.lbl + ' stops short of what it translates');
    const g = data.DUA_TODAY.find((d) => d.lbl.startsWith('Gratitude'));
    ok(g.arabic.includes('وَالِدَيَّ'), '46:15 card skips words in the middle of the verse');
  });
});

/* The same audit traced the nine-month guide to From Marriage to Parenthood (World
   Federation, 2006, ch.6), which the planner was copied from, and checked each act
   for a primary source. These hold what it found. */
describe('the guide, as checked against its sources', () => {
  const SOURCES = ['From Marriage to Parenthood', 'al-Kāfī', 'Biḥār al-Anwār', 'Makārim al-Akhlāq',
    'Mustadrak al-Wasāʾil', 'Sistani', 'NHS', 'Aimen’s planner'];
  const acts = [];
  for (const m of Object.keys(MONTHS)) for (const a of MONTHS[m].acts) acts.push(a);

  it('says where every act comes from', () => {
    for (const a of acts) ok(SOURCES.some((src) => a.s.includes(src)), a.id + ' names no source: ' + JSON.stringify(a.s));
    for (const m of Object.keys(MONTHS)) for (const x of MONTHS[m].also || []) {
      ok(SOURCES.some((src) => x.includes(src)), 'month ' + m + ' "also" names no source: ' + x);
    }
  });

  it('records every act in docs/SOURCES.md', () => {
    // so an act cannot ship without someone having looked for its source
    const doc = require('fs').readFileSync(require('path').join(__dirname, '..', 'docs', 'SOURCES.md'), 'utf8');
    for (const a of acts) ok(doc.includes('| ' + a.id + ' | '), a.id + ' has no row in docs/SOURCES.md');
  });

  it('carries none of the citations that did not hold up', () => {
    // "Mustadrak vol.3 p.112/635" are not where those hadith are; Masāʾil ʿIlmī is not a hadith book
    const text = JSON.stringify(MONTHS) + JSON.stringify(DUA_TEXTS);
    ok(!/Mustadrak al-Wasāʾil, Vol\. 3/.test(text), 'a Mustadrak vol. 3 citation is back');
    ok(!/Masāʾile? ʿIlmī/.test(text), 'Masāʾil ʿIlmī dar Qurʾān is cited as a source again');
  });

  it('puts Friday night on Thursday, since that is when it falls', () => {
    const fatir = acts.find((a) => a.t.includes('Fāṭir'));
    eq(fatir.days, [4], 'Sūrah Fāṭir is for Friday night, which is Thursday evening');
  });

  it('keeps month seven as the book gives it', () => {
    const m7 = MONTHS[7].acts;
    eq(m7.find((a) => a.t.includes('an-Naḥl')).days, [1], 'an-Naḥl is for Mondays');
    ok(m7.find((a) => a.t.includes('al-Anʿām')).days === null, 'al-Anʿām is for forty days, not Mondays');
    ok(m7.some((a) => a.s.includes('al-Ḥadīd (57)')), 'the five sūrahs start with al-Ḥadīd');
    ok(m7.some((a) => a.t.includes('quince')), 'Yāsīn is recited over a quince');
  });

  it('keeps the turbah within what Sistani allows', () => {
    const eaten = acts.filter((a) => /Khāke Shifāʾ/.test(a.t) && !/^Rub/.test(a.t));
    ok(eaten.length > 0, 'expected an act about eating Khāke Shifāʾ');
    for (const a of eaten) {
      ok(!/pinch/i.test(a.t + a.s), a.id + ': a pinch is more than the chickpea size Sistani allows');
      ok(a.s.includes('ruling 2645'), a.id + ' should cite the ruling');
    }
  });
});

/* And the duʿāʾ library, sunnah foods and names: Shiʿi sources only, each named,
   and nothing passed off as checked that could not be. */
describe('the library, as checked against its sources', () => {
  const { DUAS, FOOD, NAMES, DUA_TODAY } = data;
  const all = [];
  for (const cat of Object.keys(DUAS)) for (const d of DUAS[cat]) all.push(d);
  const SOURCES = ['Qurʾān', 'Sūrah', 'al-Kāfī', 'Man lā yaḥḍuruhu', 'al-Amālī', 'Ṭibb al-Aʾimmah', 'Sistani',
    'From Marriage to Parenthood', 'A Mother’s Prayer'];

  it('names a real source for every duʿāʾ, not "tradition" or "sunnah"', () => {
    for (const d of all) {
      ok(!/^(Traditions|Narrated traditions|Sunnah of the Prophet|General supplication)/.test(d.src), d.title + ' has a vague source: ' + d.src);
      ok(SOURCES.some((x) => d.src.includes(x)), d.title + ' names no known source: ' + d.src);
    }
  });

  it('cites no Sunni collection', () => {
    const text = JSON.stringify([DUAS, FOOD, DUA_TODAY, MONTHS, DUA_TEXTS]);
    for (const b of ['Bukhārī', 'Bukhari', 'Muslim,', 'Tirmidh', 'Ibn al-Sunn', 'Abū Dāwūd', 'Abu Dawud', 'Suyūṭī']) {
      ok(!text.includes(b), 'cites ' + b);
    }
    // the labour recitations with 7:54 are Ibn al-Sunnī's report, not a Shiʿi one
    ok(!all.some((d) => /7:54/.test(d.translit + d.body)), 'the Ibn al-Sunnī labour recitations are back');
  });

  it('cites A Mother’s Prayer by chapter, and only for what the book says', () => {
    // checked against the book itself on 27 September 2026; see docs/SOURCES.md
    const lines = all.map((d) => d.src).concat(DUA_TODAY.map((d) => d.sr));
    for (const l of lines) {
      ok(!l.includes('not yet checked'), 'still marked unchecked: ' + l);
      if (l.includes('A Mother’s Prayer')) ok(/A Mother’s Prayer, ch\.\d/.test(l), 'no chapter given: ' + l);
    }
    // the book never recommends 3:36; it only has 3:35, written out for labour
    const p = DUA_TODAY.find((d) => /3:36/.test(d.sr));
    ok(!p.sr.includes('A Mother’s Prayer'), '3:36 is credited to A Mother’s Prayer, which does not recommend it');
  });

  it('never offers honey to a newborn', () => {
    const t = all.find((d) => d.title === 'Tahnik');
    ok(!/or honey/i.test(t.translit + t.body), 'taḥnīk offers honey, which the NHS says not to give before one');
    ok(/Never honey/.test(t.body), 'taḥnīk should warn against honey');
  });

  it('flags frankincense as a herbal remedy to check first', () => {
    const f = FOOD.sunnah.find((x) => /Frankincense/.test(x.name));
    ok(/midwife or pharmacist/.test(f.note), 'frankincense lost its NHS caution');
    ok(!/chew/i.test(f.note), 'the ḥadīth says give (feed), not chew');
  });

  it('gives names their narrated meanings', () => {
    const m = {};
    for (const g of Object.keys(NAMES)) for (const x of NAMES[g]) m[x.n] = x.m;
    ok(/Weaned from evil/.test(m.Fatima), 'Fatima: the narrated meaning is weaned (kept) from evil');
    ok(!/daughter of the Prophet/.test(m.Ruqayya), 'Ruqayya: the Shiʿi association is the daughter of Imam Husayn');
    ok(!/Noble/.test(m.Khadija), 'Khadija means born early');
    ok(m.Abdullah, 'Abdullah is the first name in the ḥadīth on the best names');
  });
});

/* -------------------------------------------------------------- calendar export */
describe('calendar export', () => {
  T.setState({ dueDate: DUE });
  const ics = T.buildICS();

  it('refuses to build without a due date', () => {
    T.setState({});
    eq(T.buildICS(), null);
    T.setState({ dueDate: DUE });
  });

  it('wraps the calendar properly', () => {
    ok(ics.startsWith('BEGIN:VCALENDAR'), 'missing VCALENDAR opener');
    ok(ics.trimEnd().endsWith('END:VCALENDAR'), 'missing VCALENDAR closer');
    ok(ics.includes('VERSION:2.0'), 'missing VERSION');
    ok(ics.includes('PRODID:'), 'missing PRODID');
  });

  it('balances every event and gives each the fields a calendar needs', () => {
    const open = (ics.match(/BEGIN:VEVENT/g) || []).length;
    const close = (ics.match(/END:VEVENT/g) || []).length;
    eq(open, close, 'unbalanced VEVENT blocks');
    ok(open > 200, 'expected an event for most days, got ' + open);
    for (const f of ['UID:', 'DTSTAMP:', 'DTSTART;VALUE=DATE:', 'DTEND;VALUE=DATE:', 'SUMMARY:']) {
      eq((ics.match(new RegExp(f.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')) || []).length, open, 'missing ' + f + ' on some events');
    }
  });

  it('gives every event a unique id', () => {
    const uids = [...ics.matchAll(/UID:(.*)/g)].map((m) => m[1].trim());
    eq(uids.length, new Set(uids).size, 'duplicate UIDs would make calendars overwrite events');
  });

  it('uses CRLF and folds every line to 75 octets', () => {
    ok(ics.includes('\r\n'), 'not CRLF');
    for (const line of ics.split('\r\n')) {
      ok(Buffer.byteLength(line, 'utf8') <= 75, 'line over 75 octets: ' + line.slice(0, 40));
    }
  });

  it('survives being unfolded, with the arabic intact', () => {
    const unfolded = ics.replace(/\r\n /g, '');
    ok(unfolded.includes('Sūrah al-Ikhlāṣ (112) over two jujube dates'), 'a folded title did not come back together');
    ok(!/[�]/.test(ics), 'a multi-byte character was split by the folding');
  });

  it('escapes the characters iCalendar reserves', () => {
    eq(T.icsEscape('a,b;c\\d'), 'a\\,b\\;c\\\\d');
    eq(T.icsEscape('one\ntwo'), 'one\\ntwo');
    // an unescaped comma in a SUMMARY would split it into two values
    const unfolded = ics.replace(/\r\n /g, '');
    for (const line of unfolded.split('\r\n')) {
      if (!line.startsWith('SUMMARY:') && !line.startsWith('DESCRIPTION:')) continue;
      const body = line.slice(line.indexOf(':') + 1);
      ok(!/(^|[^\\])[,;]/.test(body), 'unescaped delimiter in: ' + line.slice(0, 60));
    }
  });

  it('folds without losing or adding a single character', () => {
    const long = 'DESCRIPTION:' + 'Sūrah al-Fātiḥah ﷽ '.repeat(30);
    eq(T.icsFold(long).replace(/\r\n /g, ''), long, 'fold then unfold changed the content');
    eq(T.icsFold('SHORT:line'), 'SHORT:line', 'a short line should be left alone');
  });

  it('runs each day from week 4 to the due date, one event per day', () => {
    const starts = [...ics.matchAll(/DTSTART;VALUE=DATE:(\d{8})/g)].map((m) => m[1]);
    eq(starts.length, new Set(starts).size, 'more than one event on the same day');
    eq(starts.slice().sort().join(), starts.join(), 'events are not in date order');
    const first = starts[0], last = starts[starts.length - 1];
    const due = DUE.replace(/-/g, '');
    eq(last, due, 'last event should land on the due date');
    ok(first < last, 'first event is not before the last');
  });

  it('ends each all-day event on the following day, as the spec requires', () => {
    const pairs = [...ics.matchAll(/DTSTART;VALUE=DATE:(\d{8})\r\nDTEND;VALUE=DATE:(\d{8})/g)];
    ok(pairs.length > 200, 'could not read the start/end pairs');
    for (const [, s, e] of pairs.slice(0, 50)) {
      const sd = new Date(s.slice(0, 4) + '-' + s.slice(4, 6) + '-' + s.slice(6));
      const ed = new Date(e.slice(0, 4) + '-' + e.slice(4, 6) + '-' + e.slice(6));
      eq((ed - sd) / 86400000, 1, 'all-day event ' + s + ' does not end the next day');
    }
  });
});

/* ---------------------------------------------------------------------- backups */
describe('backup and restore', () => {
  it('round-trips a full state', () => {
    const state = { name: 'Aimen', dueDate: DUE, done: { '2026-08-16': ['m4a', 'salah'] }, duaDone: { '2026-08-16': true }, journal: [{ d: '2026-08-16', t: 'grateful' }] };
    T.setState(state);
    const text = JSON.stringify(T.backupPayload());
    eq(T.readBackup(text), state, 'what went in did not come back');
  });

  it('labels the file so a future version knows what it is', () => {
    T.setState({ dueDate: DUE });
    const p = T.backupPayload();
    eq(p.app, 'ummi');
    eq(p.schema, 1);
    ok(/^\d{4}-\d{2}-\d{2}T/.test(p.exportedAt), 'no export timestamp');
  });

  it('still restores the older bare-state backups', () => {
    const old = { name: 'Aimen', dueDate: DUE, journal: [] };
    eq(T.readBackup(JSON.stringify(old)), old);
  });

  it('refuses anything that is not a backup', () => {
    for (const junk of ['', 'not json', '{]', 'null', '[]', '{}', '{"dueDate":"nonsense"}', '{"app":"ummi"}', '"a string"', '42',
      // an array stringifies to something the date pattern happily matches, so the
      // type has to be checked as well as the shape
      '{"dueDate":["2026-11-20"]}', '{"dueDate":2026}', '{"dueDate":null}']) {
      eq(T.readBackup(junk), null, 'accepted junk: ' + junk);
    }
  });

  it('writes a readable record of days and reflections', () => {
    T.setState({ name: 'Aimen', dueDate: DUE, done: { '2026-08-15': ['m4a'], '2026-08-16': ['m4a', 'salah'] }, duaDone: { '2026-08-16': true }, journal: [{ d: '2026-08-16', t: 'a good day' }] });
    const txt = T.historyText();
    ok(txt.includes('2026-08-15 — 1 item ticked'), 'singular item wording wrong');
    ok(txt.includes('2026-08-16 — 2 items ticked, dua recited'), 'day line wrong');
    ok(txt.includes('a good day'), 'reflection missing');
    ok(txt.includes(DUE), 'due date missing');
  });

  it('actually restores through the file picker, not just the parser', () => {
    const saved = { name: 'Aimen', dueDate: DUE, done: { '2026-08-16': ['m4a'] }, journal: [{ d: '2026-08-16', t: 'kept this' }] };
    const fresh = load({});
    fresh.App.init();
    const ev = { target: { files: [{ _text: JSON.stringify({ app: 'ummi', schema: 1, state: saved }) }], value: 'x' } };
    fresh.App.importData(ev);
    eq(fresh.T.getState(), saved, 'the restored state is not what was in the file');
    eq(JSON.parse(fresh.store.ummi_v1), saved, 'the restore was not written to storage');
    ok(fresh.alerts.some((a) => /restored/i.test(a)), 'no confirmation shown');
  });

  it('leaves existing data alone when the file is not a backup', () => {
    const existing = { name: 'Aimen', dueDate: DUE, journal: [{ d: '2026-08-16', t: 'do not lose me' }] };
    const fresh = load({ localStorage: { ummi_v1: JSON.stringify(existing) } });
    fresh.App.init();
    fresh.App.importData({ target: { files: [{ _text: 'not a backup at all' }], value: 'x' } });
    const s = fresh.T.getState();
    // rendering lazily adds empty done/duaDone maps, so compare what she would miss
    eq(s.dueDate, existing.dueDate, 'a bad file overwrote the due date');
    eq(s.name, existing.name, 'a bad file overwrote the name');
    eq(s.journal, existing.journal, 'a bad file overwrote the journal');
    ok(fresh.alerts.some((a) => /doesn't look like/i.test(a)), 'no warning shown');
  });

  it('writes a record even when nothing has been done yet', () => {
    T.setState({ dueDate: DUE });
    const txt = T.historyText();
    ok(txt.includes('DAILY RECORD (0 days)'), 'empty record should still say so');
    ok(txt.includes('REFLECTIONS (0)'));
  });
});

/* ---------------------------------------------------------- existing installs */
describe('upgrading an existing install', () => {
  /* Aimen is already using this on her phone. The update arrives over the top of
     her data, so what she has must survive it. This is the test that says so. */
  const OLD = {
    name: 'Aimen',
    dueDate: DUE,
    joined: '2026-07-20',
    // task ids from the version before the month plan existed
    done: { '2026-08-14': ['quran', 'salawat', 'supp', 'salah'], '2026-08-15': ['water'] },
    duaDone: { '2026-08-14': true },
    journal: [{ d: '2026-08-14', t: 'felt the first kicks' }],
  };

  it('keeps her due date, name, journal and history', () => {
    const up = load({ localStorage: { ummi_v1: JSON.stringify(OLD) } });
    up.App.init();
    const s = up.T.getState();
    eq(s.dueDate, OLD.dueDate);
    eq(s.name, OLD.name);
    eq(s.journal, OLD.journal);
    eq(s.duaDone, OLD.duaDone);
    eq(s.done, OLD.done, 'her tick history was rewritten');
  });

  it('opens on Today rather than sending her back through the gate', () => {
    const up = load({ localStorage: { ummi_v1: JSON.stringify(OLD) } });
    up.App.init();
    eq(up.doc.getElementById('s-today').classList.contains('active'), true);
  });

  it('still honours the baseline ticks she shares with the old version', () => {
    const up = load({ localStorage: { ummi_v1: JSON.stringify(OLD) } });
    up.App.init();
    // salah, water and walk survived the rewrite; the retired ids are simply ignored
    const ids = up.T.tasksToday().map((t) => t.id);
    ok(ids.includes('salah') && ids.includes('water') && ids.includes('walk'));
    ok(!ids.includes('quran'), 'a retired task id came back');
  });

  it('backs up and restores across the version change', () => {
    const up = load({ localStorage: { ummi_v1: JSON.stringify(OLD) } });
    up.App.init();
    const backup = JSON.stringify(up.T.backupPayload());
    const fresh = load({});
    fresh.App.init();
    eq(fresh.T.readBackup(backup), OLD, 'a backup taken now does not restore cleanly onto a new phone');
  });
});

/* -------------------------------------------------------------------- rendering */
describe('rendering', () => {
  it('renders every screen without reaching for a missing element', () => {
    T.setState({ name: 'Aimen', dueDate: DUE });
    for (const [name, fn] of Object.entries(T.renderers)) {
      try { fn(); } catch (e) { throw new Error('render ' + name + ' threw: ' + e.message); }
    }
  });

  it('renders every month and every week', () => {
    T.setState({ name: 'Aimen', dueDate: DUE });
    for (let m = 1; m <= 9; m++) { app.App.go('month'); app.App.shiftMonth(1); }
    app.App.go('week');
    for (let w = 4; w <= 41; w++) app.App.shiftWeek(1);
    for (let w = 4; w <= 41; w++) app.App.shiftWeek(-1);
  });

  it('opens the reader for every text the app can link to', () => {
    for (const n of Object.keys(SURAHS)) app.App.openText('surah:' + n);
    for (const r of Object.keys(AYAHS)) app.App.openText('ayah:' + r);
    for (const k of Object.keys(DUA_TEXTS)) app.App.openText('dua:' + k);
    const body = app.doc.getElementById('rd-body').innerHTML;
    ok(body.length > 50, 'the reader rendered nothing');
  });

  it('ignores a reference to something that does not exist', () => {
    app.App.openText('surah:999');
    app.App.openText('ayah:1:1');
    app.App.openText('dua:nope');
    app.App.openText('nonsense');
  });

  it('escapes user text rather than pasting it into the page', () => {
    T.setState({ dueDate: DUE, journal: [{ d: '2026-08-16', t: '<img src=x onerror=alert(1)>' }] });
    T.renderers.more();
    const html = app.doc.getElementById('m-journal-list').innerHTML;
    ok(!html.includes('<img'), 'journal text was not escaped');
    ok(html.includes('&lt;img'), 'expected the escaped form');
  });

  it('opens the month screen on the month she is actually in', () => {
    // a fresh instance, so nothing an earlier test browsed to can carry over
    const fresh = load({ localStorage: { ummi_v1: JSON.stringify({ name: 'Aimen', dueDate: dueInDays(98) }) } });
    fresh.App.init();
    fresh.App.go('month');
    eq(fresh.doc.getElementById('mo-num').textContent, 'Month 7', 'week 26 sits in month 7');
    ok(fresh.doc.getElementById('mo-weeks').textContent.includes('you are here'));
  });

  it('starts a new user on the gate, and an existing one on today', () => {
    const blank = load({});
    blank.App.init();
    eq(blank.doc.getElementById('s-gate').classList.contains('active'), true, 'no due date should land on the gate');
    const known = load({ localStorage: { ummi_v1: JSON.stringify({ dueDate: DUE }) } });
    known.App.init();
    eq(known.doc.getElementById('s-today').classList.contains('active'), true, 'a known user should land on today');
  });
});

/* --------------------------------------------------------------------- the file */
describe('the shipped file', () => {
  it('references no element id that the markup does not define', () => {
    const ids = new Set([...app.html.matchAll(/id="([a-zA-Z0-9_-]+)"/g)].map((m) => m[1]));
    const used = new Set([...app.html.matchAll(/getElementById\(['"]([a-zA-Z0-9_-]+)['"]\)/g)].map((m) => m[1]));
    const missing = [...used].filter((u) => !ids.has(u));
    eq(missing, [], 'markup is missing these ids');
  });

  it('exports every App method the markup calls', () => {
    const ret = /return \{init:init[\s\S]*?\};/.exec(app.html)[0];
    const exported = new Set([...ret.matchAll(/([a-zA-Z_]+):/g)].map((m) => m[1]));
    const called = new Set([...app.html.matchAll(/App\.([a-zA-Z_]+)\(/g)].map((m) => m[1]));
    const missing = [...called].filter((c) => !exported.has(c));
    eq(missing, [], 'these are called from the markup but not exported');
  });

  it('loads nothing from the network at runtime except the fonts', () => {
    // an offline-first app that silently depends on a CDN is broken on a plane
    const urls = [...app.html.matchAll(/https?:\/\/[^"'\s)]+/g)].map((m) => m[0]);
    const notFonts = urls.filter((u) => !/fonts\.(googleapis|gstatic)\.com/.test(u));
    eq(notFonts, [], 'unexpected external references');
  });

  it('keeps the storage key stable, or everyone loses their data', () => {
    ok(app.html.includes("KEY='ummi_v1'"), 'the localStorage key changed; existing users would start from scratch');
  });
});

/* ---------------------------------------------------------- clock changes */
/* Adding 24-hour blocks to a local midnight lands at 23:00 the day before once the
   clocks go back, so anything that counts days must count calendar days. These run
   in the UK's time zone with "now" pinned, so they don't depend on when they run. */
describe('across a clock change (Europe/London)', () => {
  function inLondon(fn) {
    const prev = process.env.TZ;
    process.env.TZ = 'Europe/London';
    try { fn(); } finally { if (prev === undefined) delete process.env.TZ; else process.env.TZ = prev; }
  }
  const utcDay = (s) => Date.UTC(+s.slice(0, 4), +s.slice(4, 6) - 1, +s.slice(6, 8));

  it('puts every calendar event on its own day for a summer due date', () => inLondon(() => {
    const a = load({ now: '2026-09-27T10:00' });
    a.T.setState({ dueDate: '2027-07-15' });
    const starts = [...a.T.buildICS().matchAll(/DTSTART;VALUE=DATE:(\d{8})/g)].map((m) => m[1]);
    eq(starts[0], '20261105', 'first event (week 4) is on the wrong day');
    eq(starts[starts.length - 1], '20270715', 'last event is not the due date');
    for (let i = 1; i < starts.length; i++) {
      eq((utcDay(starts[i]) - utcDay(starts[i - 1])) / 86400000, 1, 'the calendar skips or repeats a day after ' + starts[i - 1]);
    }
  }));

  it('works out the due date from the week you signed up with, in summer', () => inLondon(() => {
    // Being a day out still lands in the same week, so check the date itself.
    for (let w = 4; w <= 41; w++) {
      const a = load({ now: '2026-09-27T10:00' });
      a.App.init();
      a.doc.getElementById('g-week').value = String(w);
      a.App.signup();
      const expected = new Date(Date.UTC(2026, 8, 27) + (40 - w) * 7 * 86400000).toISOString().slice(0, 10);
      eq(a.T.getState().dueDate, expected, 'signed up at week ' + w + ' on 27 September');
      eq(a.T.currentWeek(), w, 'signed up at week ' + w);
    }
  }));

  it("keeps today's dua for the whole day, even just after midnight in summer", () => inLondon(() => {
    // 1 June 2026 is day 152; the Android app picks the same dua from the day of the year
    eq(load({ now: '2026-06-01T00:30' }).T.dayIndex(), 152, 'just after midnight');
    eq(load({ now: '2026-06-01T23:30' }).T.dayIndex(), 152, 'just before midnight');
  }));
});

/* ------------------------------------------------------------ the Android app */
/* The Android app (android/) is a separate codebase that must stay compatible
   with this one: same content, and backups that restore in either direction.
   tests/fixtures/backup-compat.json is checked by both suites. */
describe('android app compatibility', () => {
  const fs = require('fs');
  const path = require('path');
  const { ROOT } = require('./harness');
  const fixture = JSON.parse(fs.readFileSync(path.join(__dirname, 'fixtures', 'backup-compat.json'), 'utf8'));

  function canonical(v) {
    if (Array.isArray(v)) return v.map(canonical);
    if (v && typeof v === 'object') {
      const out = {};
      for (const k of Object.keys(v).sort()) out[k] = canonical(v[k]);
      return out;
    }
    return v;
  }

  /* The first path at which two JSON values differ, so a failure names the field
     rather than dumping the whole of WEEKS. */
  function firstDiff(a, b, at) {
    if (JSON.stringify(canonical(a)) === JSON.stringify(canonical(b))) return null;
    if (a && b && typeof a === 'object' && typeof b === 'object') {
      for (const k of new Set([...Object.keys(a), ...Object.keys(b)])) {
        const d = firstDiff(a[k], b[k], at + '.' + k);
        if (d) return d;
      }
    }
    return at + ': index.html has ' + JSON.stringify(b) + ', Android has ' + JSON.stringify(a);
  }

  it('ships the same content as index.html', () => {
    const asset = JSON.parse(fs.readFileSync(path.join(ROOT, 'android/app/src/main/assets/ummi-data.json'), 'utf8'));
    for (const key of ['WEEKS', 'MONTHS', 'FOOD', 'DUAS', 'DUA_TEXTS', 'DUA_TODAY', 'BISMILLAH', 'AYAHS', 'SURAHS']) {
      const d = firstDiff(asset[key], data[key], key);
      ok(!d, 'android/app/src/main/assets/ummi-data.json is out of date with index.html at ' + d);
    }
  });

  it('shows the same every-day tasks as index.html', () => {
    // BASE_TASKS is private to the App closure; tasksToday() is the month's acts followed by it
    const base = T.tasksToday().slice(T.todaysActs().length);
    ok(base.length > 0, 'index.html has no every-day tasks; this test needs rethinking');
    const asset = JSON.parse(fs.readFileSync(path.join(ROOT, 'android/app/src/main/assets/ummi-data.json'), 'utf8'));
    const d = firstDiff(asset.BASE_TASKS, base, 'BASE_TASKS');
    ok(!d, 'android/app/src/main/assets/ummi-data.json is out of date with index.html at ' + d);
  });

  it('restores every shared valid file with the same result the Android app gets', () => {
    for (const c of fixture.valid) {
      const s = T.readBackup(c.file);
      ok(s, 'rejected: ' + c.why);
      for (const k of Object.keys(c.expect)) eq(s[k], c.expect[k], c.why + ', field ' + k);
    }
  });

  it('rejects every shared invalid file', () => {
    for (const junk of fixture.invalid) eq(T.readBackup(junk), null, 'accepted junk: ' + junk);
  });

  it('restores a backup written by the Android app', () => {
    const s = T.readBackup(fixture.androidBackup.file);
    ok(s, 'the Android backup was not recognised');
    for (const k of Object.keys(fixture.androidBackup.state)) eq(s[k], fixture.androidBackup.state[k], 'field ' + k);
  });

  it('keeps the Android app\'s visits, names and reminders in its own backup', () => {
    // Otherwise a trip through the browser would quietly lose them.
    T.setState(T.readBackup(fixture.androidBackup.file));
    const written = JSON.parse(JSON.stringify(T.backupPayload()));
    for (const k of ['names', 'visits', 'reminders', 'place']) {
      ok(k in fixture.androidBackup.state, 'the fixture no longer has ' + k);
      eq(written.state[k], fixture.androidBackup.state[k], 'field ' + k);
    }
  });

  it('writes the same calendar file as the Android app', () => {
    // android/.../CalendarExportTest checks its own output against this file too
    const expected = fs.readFileSync(path.join(__dirname, 'fixtures', 'calendar-2026-12-15.ics'), 'utf8');
    T.setState({ dueDate: '2026-12-15' });
    const ics = T.buildICS().replace(/^DTSTAMP:.*$/gm, 'DTSTAMP:20260926T000000Z');
    ok(ics === expected, 'buildICS no longer matches tests/fixtures/calendar-2026-12-15.ics; regenerate it and make the Android test pass too');
  });

  it('writes the same readable record as the Android app', () => {
    T.setState(JSON.parse(JSON.stringify(fixture.history.state)));
    eq(T.historyText().replace(/^Exported .*$/m, 'Exported ' + fixture.history.today), fixture.history.text);
  });

  it('lists visits in the readable record the same way as the Android app', () => {
    const h = fixture.historyWithVisits;
    T.setState(JSON.parse(JSON.stringify(h.state)));
    eq(T.historyText().replace(/^Exported .*$/m, 'Exported ' + h.today), h.text);
  });

  it('keeps visits through a restore and a new backup', () => {
    const s = T.readBackup(fixture.androidBackup.file);
    T.setState(s);
    eq(T.backupPayload().state.visits, fixture.androidBackup.state.visits);
  });
});

/* ---------------------------------------------------------------------- results */
const total = passed + failures.length;
if (failures.length) {
  console.log('\n' + failures.length + ' of ' + total + ' failed:\n');
  for (const f of failures) console.log('  ✗ ' + f.name + '\n    ' + f.err.message + '\n');
  process.exit(1);
}
console.log('\n  ' + passed + ' of ' + total + ' passed\n');
