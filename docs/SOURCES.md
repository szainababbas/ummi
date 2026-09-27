# Sources

Where everything in Ummi comes from, what kind of source each thing is allowed to rest on, and what has and has not been checked. Last full audit: **27 September 2026**.

`tests/run.js` fails if a month act ships without an entry in the table below, or if an act, duʿāʾ or food names no source. So a new item needs a source before it can go in.

## The rules

1. **Islamic content rests on Shiʿi sources only.** That covers duʿāʾs, sūrahs, acts, foods from ḥadīth, names and rulings.
   - **Primary:** al-Kāfī, Man lā yaḥḍuruhu al-faqīh, Tahdhīb, Wasāʾil al-Shīʿa, Mustadrak al-Wasāʾil, Biḥār al-Anwār, Makārim al-Akhlāq, Ḥilyat al-Muttaqīn, Ṭibb al-Aʾimmah, al-Amālī, Mafātīḥ al-Jinān.
   - **Secondary, Shiʿi:** From Marriage to Parenthood (World Federation of KSIMC), A Mother's Prayer (Bhimji), duas.org, al-islam.org.
   - **Rulings:** Ayatollah Sistani is the marjaʿ. Cite *Islamic Laws* by ruling number (sistani.org).
   - **Never:** Sunni collections (Bukhārī, Muslim, Tirmidhī, Ibn al-Sunnī and so on), islamqa, general Islamic aggregators or blogs. `tests/run.js` fails if one is cited.
2. **Health content rests on the NHS** (nhs.uk). NICE, RCOG and GOV.UK sit behind it. This is a UK app.
3. **Check the primary source itself.** A search snippet or a secondary summary is not a check. If the primary can't be reached, the item says so ("not yet checked against the book", "via duas.org") rather than guessing a page.
4. **Say what kind of source it is.** A modern devotional book is not a ḥadīth. Where a practice comes only from a book, the app cites the book and page, and this file records that no narration was found.
5. **Record coverage:** what was read, what could not be reached, and what was skipped on purpose.

## What each part of the app rests on

| Data | Source | Status |
|---|---|---|
| `SURAHS`, `AYAHS`, `BISMILLAH` | Uthmani text and ʿAlī Qulī Qarāʾī's translation, from api.alquran.cloud, built by `tools/gen_quran.py` | Checked by script against Tanzil and quran.com: every verse matches. 16:69 is corrected to the muṣḥaf spelling in the generator. The English matches Tanzil's Qarāʾī byte for byte. 24 transliteration slips are fixed in `TL_FIXES`. |
| `DUA_TODAY` | Qurʾān verses, Qarāʾī's wording | Arabic checked against Tanzil. Each card now shows all the Arabic it translates. A test holds each meaning to the app's own Qarāʾī text. |
| `DUA_TEXTS` | Ṣalawāt, long ṣalawāt, istighfār, tasbīḥ | The long ṣalawāt's words are checked against Biḥār 86:289, where they are a Thursday and Friday ṣalawāt; the pregnancy use comes from the book. Tasbīḥ order checked (al-Kāfī 3; Sistani 1108). |
| `MONTHS` | Aimen's planner, which is From Marriage to Parenthood ch.6, pp.109 to 115, almost line for line. That book takes it from *Rayḥāneh-ye Beheshtī*, a modern Persian popular book. | Every act checked; see the table below. |
| `DUAS` | Mixed | Every source is named. Vague lines ("Traditions of the Ahlulbayt", "Sunnah of the Prophet") are gone. |
| `FOOD.sunnah` | al-Kāfī 6 (Kitāb al-ʿAqīqah ch.12; Kitāb al-Aṭʿimah) | Checked. The "recite Yāsīn / Yūsuf over it" lines were removed, because they come from the book, not a narration. |
| `FOOD.med`, `FOOD.avoid`, `WEEKS` (except `faith`), `BASE_TASKS` | NHS | All 38 NHS week pages (4 to 41) and the pregnancy, diet, vitamins, exercise, vaccinations and movements pages were read on 27 Sep 2026. |
| `NAMES` | al-Kāfī 6 ch.10; al-Amālī; history | Meanings checked. |

## The planner's own citations

| Planner said | What the source actually says | Now |
|---|---|---|
| Biḥār al-Anwār 86:289, long ṣalawāt with your hand on your stomach | The words are there (from Miṣbāḥ al-Mutahajjid), recommended often from Thursday ʿAṣr to the end of Friday, 100 times praised. Nothing on pregnancy or the stomach. | The Biḥār citation is kept for the words; the hand on the stomach is cited to the book, p.110. |
| Mustadrak al-Wasāʾil 3:112, dates in the month of birth | Not found there. The ḥadīth is in **Makārim al-Akhlāq p.169**, from the Prophet (s), without a chain. | Cites Makārim. |
| Mustadrak 3:635, melon | The pregnancy ḥadīth is in Mustadrak (Āl al-Bayt ed.) **15:214 no.18038**, taken from al-Mustaghfirī's *Ṭibb al-Nabī*, a Sunni collection. Weak. "After food" has support (16:410, Imam al-ʿAskarī (as)). "No water" and "kharbozeh in particular" have no source. | The act says all of this. |
| Masāʾile ʿIlmī dar Qurʾān p.140, pomegranate | A modern popular-science book, not a ḥadīth source | Replaced with **al-Kāfī 6:355** (Friday on an empty stomach; sweet pomegranate "improves the child"). |

## Where the app had drifted from the book

These are fixed to match From Marriage to Parenthood. If Aimen's sheet changed any of them on purpose, change them back and note it here.

- **Month 3:** ṣalawāt 140 times, every day. The app had 100, on Fridays only.
- **Month 6:** figs and olives *after* breakfast, not for it.
- **Month 7** (p.114):
  - al-Anʿām is over almonds, after Fajr, for forty days. The app had "on Mondays".
  - an-Naḥl is on Mondays.
  - Yāsīn and al-Mulk are on Thursday and Friday.
  - an-Nūr is often.
  - The five sūrahs are al-Ḥadīd, al-Ḥashr, aṣ-Ṣaff, al-Jumuʿah and at-Taghābun. The app had al-Qadr instead of al-Ḥadīd.
  - Yāsīn is over a quince, not almonds.
  - al-Qadr goes with al-Ikhlāṣ in the prayers.
- **Month 9:** Fāṭir "on Friday night" is Thursday evening after Maghrib, so it now shows on Thursdays.

## Every month act

**Status** means:

- **Primary source:** a Shiʿi primary text, or a Sistani ruling, supports it.
- **Primary, in part:** some of it is primary; the rest (a count, a day, the hand on the stomach) comes from the book.
- **Book only:** From Marriage to Parenthood gives it, after Rayḥāneh-ye Beheshtī, but no narration was found.
- **NHS:** health advice.

| id | Act | Status | What the app says |
|---|---|---|---|
| m1a | Adhān and iqāmah before every prayer | Book only | With your hand on your stomach. From Marriage to Parenthood, p.110. |
| m1b | Sūrah Yāsīn (36) and Sūrah aṣ-Ṣāffāt (37) | Book only | Then blow gently on your stomach. From Marriage to Parenthood, p.109. |
| m1c | Sūrah al-Qadr (97) over two dates | Book only | Recite once, blow on the dates, eat them on an empty stomach. From Marriage to Parenthood, p.110. |
| m1d | A sweet apple in the morning | Primary, in part | Apple on an empty stomach is praised in Makārim al-Akhlāq, p.173. From Marriage to Parenthood, p.109. |
| m1e | A pomegranate before breakfast | Primary source | On an empty stomach. Imām al-Kāẓim (as): a pomegranate eaten on Friday before food lights the heart for forty mornings. al-Kāfī, Vol. 6, p.355. |
| m1f | A tiny amount of Khāke Shifāʾ before sunset | Primary, in part | No bigger than a lentil, with the intention of shifāʾ. Sistani allows eating the turbah of Imām Ḥusayn (as) only up to a chickpea’s size, for healing (Islamic Laws, ruling 2645). Soil can carry toxoplasmosis (NHS), so tell your midwife you take it. From Marriage to Parenthood, p.110. |
| m1g | Your prenatal vitamins | NHS | Folic acid, 400 micrograms a day until 12 weeks. Choose a pregnancy multivitamin: ordinary ones and cod liver oil can contain vitamin A (retinol), which is not safe in pregnancy. NHS. |
| m1h | Vitamin D, 10 micrograms a day | NHS | The NHS advises a 10 microgram supplement every day from October to March, and all year if you usually cover most of your skin outdoors. UK milk is not usually fortified, so food alone will not cover it. |
| m2a | Ṣalawāt 140 times | Primary, in part | With wa ʿajjil farajahum at the end. The ṣalawāt from Thursday ʿAṣr to Friday is in Biḥār al-Anwār, Vol. 86, p.289; the count of 140: From Marriage to Parenthood, p.110. |
| m2b | Ṣalawāt 100 times | Primary source | With wa ʿajjil farajahum at the end. Saying it 100 times on Friday: Biḥār al-Anwār, Vol. 86, p.289. |
| m2c | The long ṣalawāt, hand on your stomach | Primary, in part | The words are in Biḥār al-Anwār, Vol. 86, p.289, as a Thursday and Friday ṣalawāt. The hand on your stomach: From Marriage to Parenthood, p.110. |
| m2d | Sūrah al-Mulk (67) | Book only | From Marriage to Parenthood, p.110. |
| m2e | Sūrah al-Ikhlāṣ (112) over two jujube dates | Book only | Eat them on an empty stomach. From Marriage to Parenthood, p.111. |
| m2f | Meat with sweet apple, and milk | Book only | Once a week is enough for these. From Marriage to Parenthood, p.110. |
| m3a | Ṣalawāt 140 times | Book only | With wa ʿajjil farajahum at the end. From Marriage to Parenthood, p.111. |
| m3b | The long ṣalawāt, hand on your stomach | Primary, in part | Before each prayer. Same wording as last month (Biḥār al-Anwār, Vol. 86, p.289); the hand on your stomach: From Marriage to Parenthood, p.111. |
| m3c | Sūrah Āl ʿImrān (3) | Book only | From Marriage to Parenthood, p.111. |
| m3d | Āyatul Kursī over an apple | Book only | Then eat it on an empty stomach. From Marriage to Parenthood, p.111. |
| m3e | Honey in the morning | Book only | From Marriage to Parenthood, p.111. |
| m3f | Wheat, meat and milk | Book only | Once a week is enough for these. From Marriage to Parenthood, p.111. |
| m4a | Sūrah al-Furqān 25:74 | Book only | The prayer for comfort in your family. From Marriage to Parenthood, p.112. |
| m4b | Istighfār 7 times | Book only | Astaghfirullāha Rabbī wa atūbu ilayh. From Marriage to Parenthood, p.112. |
| m4c | Ṣalawāt 140 times after each prayer | Book only | From Marriage to Parenthood, p.112. |
| m4d | Ṣalātul Layl | Primary, in part | As much as you can manage. Sistani allows it any time after ʿIshāʾ until Fajr (Islamic Laws, ruling 760); if you miss it, make it up later. From Marriage to Parenthood, p.113. |
| m4e | Sūrah al-Insān (76) | Book only | From Marriage to Parenthood, p.111. |
| m4f | Sūrah al-Qadr (97) in one rakʿah of every prayer | Book only | After al-Fātiḥah, in the first or second rakʿah. From Marriage to Parenthood, p.112. |
| m4g | After the daily prayers: al-Kawthar, al-Qadr, long ṣalawāt | Book only | Sūrah al-Kawthar (108), Sūrah al-Qadr (97), then the long ṣalawāt with your hand on your stomach. From Marriage to Parenthood, p.112. |
| m4h | Sūrah at-Tīn (95) over two figs | Book only | Eat them on an empty stomach. From Marriage to Parenthood, p.113. |
| m4i | Sweet apples, honey and pomegranates | Primary, in part | Sweet pomegranate “improves the child”: al-Kāfī, Vol. 6, p.355. From Marriage to Parenthood, p.112. |
| m4j | Nutrient-rich foods | NHS | Lentils and beans, wholegrains, cheese and yoghurt, and leafy greens (NHS). |
| m5a | Rub Khāke Shifāʾ on your stomach | Book only | From the start of the fifth month. Turbah from Karbalāʾ. From Marriage to Parenthood, p.113. |
| m5b | Adhān and iqāmah at ṣalāh time | Book only | With your hand on your stomach. From Marriage to Parenthood, p.113. |
| m5c | Sūrah al-Fatḥ (48) | Book only | From Marriage to Parenthood, p.113. |
| m5d | Sūrah an-Naṣr (110) in the daily prayers | Book only | From Marriage to Parenthood, p.113. |
| m5e | Sūrah al-Fātiḥah (1) over an egg | Book only | Then eat it on an empty stomach. From Marriage to Parenthood, p.113. |
| m5f | A date every morning | Primary, in part | Dates on an empty stomach are praised in Makārim al-Akhlāq, p.169. From Marriage to Parenthood, p.113. |
| m5g | Nuts | NHS | Good fats, protein and vitamin E. Nuts are safe in pregnancy unless you are allergic or have been told to avoid them (NHS). |
| m6a | Rub Khāke Shifāʾ on your stomach after every prayer | Book only | Turbah from Karbalāʾ. From Marriage to Parenthood, p.113. |
| m6b | Sūrah al-Wāqiʿah (56) | Primary, in part | Reciting al-Wāqiʿah on Friday is in Biḥār al-Anwār, Vol. 86, p.289; Thursday and this month: From Marriage to Parenthood, p.113. |
| m6c | Sūrah at-Tīn (95) in one rakʿah of Maghrib and ʿIshāʾ | Book only | From Marriage to Parenthood, p.113. |
| m6d | Figs and olives after breakfast | Book only | From Marriage to Parenthood, p.113. |
| m7a | Ṣalawāt 140 times | Book only | From Marriage to Parenthood, p.114. |
| m7b | Sūrah al-Anʿām (6) over almonds after Fajr | Book only | For forty days, then eat the almonds. From Marriage to Parenthood, p.114. |
| m7c | Sūrah Yāsīn (36) and Sūrah al-Mulk (67) | Book only | From Marriage to Parenthood, p.114. |
| m7d | Sūrah an-Nūr (24) | Book only | Often, from this month onward. From Marriage to Parenthood, p.114. |
| m7e | The five sūrahs after your prayers | Book only | After the daily prayers and the tasbīḥ of Sayyidah Fāṭimah (as): al-Ḥadīd (57), al-Ḥashr (59), aṣ-Ṣaff (61), al-Jumuʿah (62) and at-Taghābun (64). From Marriage to Parenthood, p.114. |
| m7f | Sūrah al-Qadr (97) and al-Ikhlāṣ (112) in the daily prayers | Primary, in part | After al-Fātiḥah. Once you start al-Ikhlāṣ, finish it rather than switching (Sistani, Islamic Laws, ruling 974). From Marriage to Parenthood, p.114. |
| m7g | Sūrah Yāsīn (36) over a quince | Primary, in part | Then eat it on an empty stomach. Quince for pregnant women: al-Kāfī, Vol. 6, p.22. From Marriage to Parenthood, p.114. |
| m7h | Melon after your food | Primary, in part | After food rather than on an empty stomach (Imām al-ʿAskarī (as), Mustadrak al-Wasāʾil, Vol. 16, p.410). The pregnancy hadith, in Mustadrak Vol. 15, p.214, comes from a Sunni medical collection, so it is weak. Avoiding water with it is from Aimen’s planner. |
| m7j | Sūrah an-Naḥl (16) | Book only | From Marriage to Parenthood, p.114. |
| m7i | Iron-rich foods | NHS | Red meat, beans and chickpeas, nuts, dried apricots and fortified cereals. Your iron is checked at booking and at 28 weeks, and you’ll be offered a supplement if it is low (NHS). |
| m8a | Adhān and iqāmah before every prayer | Book only | With your hand on your stomach. From Marriage to Parenthood, p.114. |
| m8b | Sūrah al-Qadr (97) ten times after Fajr | Book only | From Marriage to Parenthood, p.115. |
| m8c | Sūrah at-Tīn (95) twice after Fajr | Book only | From Marriage to Parenthood, p.115. |
| m8d | Sūrah Yāsīn (36) | Book only | From Marriage to Parenthood, p.115. |
| m8e | Sūrah al-Furqān (25) | Book only | From Marriage to Parenthood, p.115. |
| m8f | Sūrah al-Insān (76) | Book only | From Marriage to Parenthood, p.115. |
| m8g | Sūrah Muḥammad (47) | Book only | From Marriage to Parenthood, p.115. |
| m8h | Sūrah aṣ-Ṣāffāt (37) | Book only | From Marriage to Parenthood, p.115. |
| m8i | Sweet yogurt and honey | Book only | From Marriage to Parenthood, p.115. |
| m8j | A sweet pomegranate on an empty stomach | Primary source | On an empty stomach. al-Kāfī, Vol. 6, p.355: a pomegranate on Friday before food lights the heart, and sweet pomegranate “improves the child”. |
| m8k | Vinegar in your food | Book only | Once a week, if you have no fear of harm from it. From Marriage to Parenthood, p.115. |
| m9a | Ṣalawāt 140 times | Planner only | Allāhumma ṣalli ʿalā Muḥammadin wa Āli Muḥammad. From Aimen’s planner. |
| m9b | Istighfār 70 times after each prayer | Primary, in part | Astaghfirullāha Rabbī wa atūbu ilayh. The Prophet (s) sought forgiveness seventy times a day (al-Kāfī, Vol. 2, p.505); after each prayer is from Aimen’s planner. |
| m9c | Sūrah al-ʿAṣr (103) and adh-Dhāriyāt (51) | Primary, in part | In the Ẓuhr and ʿAṣr prayers, one in each of the first two rakʿahs. adh-Dhāriyāt is long, so leave it out if time is short (Sistani, ruling 965). From Marriage to Parenthood, p.115. |
| m9d | Sūrah al-Ḥajj (22) | Book only | From Marriage to Parenthood, p.115. |
| m9e | Sūrah Fāṭir (35) | Book only | On Friday night, which is Thursday evening after Maghrib. From Marriage to Parenthood, p.115. |
| m9f | Sūrah al-Insān (76) over dates and milk | Book only | Then take them on an empty stomach. From Marriage to Parenthood, p.115. |
| m9g | Dates and kebabs | Primary, in part | “Feed women dates in the month they give birth, and their child will be forbearing and pure.” The Prophet (s), in Makārim al-Akhlāq, p.169. Kebabs: From Marriage to Parenthood, p.115. |
| m9h | A daily walk | NHS | Any amount helps; the NHS says 30 minutes a day can be enough. |

Month 9's three "also" lines (no garam masala, the sheep for Imām al-Mahdī (aj), no mirror or pictures) are **book only**. No ḥadīth was found for any of them. The sheep now reads "as ṣadaqah on behalf of", since a sacrifice is only ever made in the name of Allah.

## Sistani rulings the guide relies on

- **2645:** eating the turbah of Imām Ḥusayn (as) is allowed only up to a chickpea's size, and only for healing. The app says lentil-sized, as the book does.
- **760:** Ṣalāt al-Layl can be prayed any time after ʿIshāʾ until Fajr.
- **965:** leave out the second sūrah when time is short. This matters for adh-Dhāriyāt in month 9.
- **969 to 970:** none of the sūrahs the guide puts in the daily prayers has a wājib sajdah. Those are 32, 41, 53 and 96.
- **974:** once al-Ikhlāṣ is started, it can't be swapped for another sūrah.
- **1108:** tasbīḥ of Sayyidah Fāṭimah (as) is 34, 33, 33, in that order.
- **1109:** sajdat al-shukr.

## Changed because the source was not Shiʿi

- **"Recitations during labour"** (Āyat al-Kursī, 7:54, al-Falaq, an-Nās at Fāṭimah's delivery) is Ibn al-Sunnī's report, via Suyūṭī. It was replaced with the Shiʿi recitations for a hard labour from *Ṭibb al-Aʾimmah*: 94:5 to 6 (Imam ʿAlī (as)), 46:35 and 79:46 (Imam al-Ṣādiq (as)), and 19:23 to 25 (Imam al-Bāqir (as)).
- **"SubḥānAllāhi wa biḥamdihi 100×"** is the Bukhārī form. It was replaced with the Shiʿi form in al-Amālī of al-Ṣadūq, majlis 47 ḥ.13 (30 times).
- **Sūrah Yūsuf "for a beautiful child"** traces to a Sunni teacher. The book only says to recite Yūsuf over an apple, and the card now says that.
  - al-Kāfī 5 (Kitāb al-Nikāḥ) has a weak (marfūʿ) report against teaching women Sūrat Yūsuf. It is noted here, not in the app.

## Health fixes (NHS, 27 Sep 2026)

- **Vitamin D:** 10 micrograms a day. UK milk is not fortified.
- **Folic acid:** 400 micrograms a day until 12 weeks. No vitamin A (retinol) supplements.
- **Pelvic floor:** 3 sets of 8 a day, held for up to 10 seconds.
- **Glucose test (24 to 28 weeks):** only offered with a risk factor. South Asian heritage is one.
- **Baby's movements:** no kick counting. Know the usual pattern and call straight away if it changes.
- **Foods to avoid:**
  - British Lion eggs are fine runny.
  - Soft ripened and blue cheese are fine if cooked until steaming hot.
  - Tuna: no more than 4 cans or 2 steaks a week.
  - Avoid game.
  - Caffeine: a mug of instant coffee is about 100mg, a mug of tea about 75mg.
- **Fish:** 2 portions a week, one of them oily, and no more than 2 oily.
- **Water:** 6 to 8 glasses a day.
- **Week tips:**
  - Whooping cough vaccine from 16 weeks.
  - MAT B1 from 20 weeks.
  - At 28 weeks: RSV vaccine, iron test, anti-D, and side sleeping from then on.
  - What to call about at 40 weeks.
- **Week 20 length:** head to heel (25.6 cm), as the NHS does from week 20. Week 22 is a sweet potato, not a second coconut.
- **Taḥnīk:** never honey. The NHS says no honey before age one.
- **Frankincense:** "check with your midwife or pharmacist first". The NHS cautions against herbal remedies in pregnancy.
- **Khāke Shifāʾ:** the NHS warns that soil can carry toxoplasmosis, so the act says to tell your midwife.

**The NHS gives no fetal weights,** so `WEEKS[].wt` is unchecked. The values follow the usual Hadlock-style chart.

## Still to check

**A Mother's Prayer.** al-islam.org serves a "verify you are human" check that an automated session can't pass. These lines are marked "not yet checked against the book" in the app, and a test keeps them marked:

- DUAS: al-Fātiḥah continuously; Yāsīn 36:36 forty times for forty days; Duʿāʾ Yastashīr in the ninth month.
- DUA_TODAY: 3:36.

Opening https://www.al-islam.org/mothers-prayer-saleem-bhimji in a browser and reading chapters 2 and 4 would settle them in about ten minutes. Then remove the marker, or remove the item.

**Primary texts not reached** (their sites were down or blocked):

- **Ṭibb al-Aʾimmah:** the labour recitations are quoted via duas.org.
- **Ḥilyat al-Muttaqīn:** apple and pregnancy is quoted via the book, p.102.
- **The old three-volume Mustadrak:** the new edition was used instead.

## Coverage of the 27 September 2026 audit

- **Read:**
  - al-Kāfī vols 1 to 6 (thaqalayn.net, Arabic and English, with Majlisī's gradings) and the Ghaffārī edition page scans (ablibrary.net).
  - Biḥār al-Anwār 86:288 to 290.
  - Mustadrak al-Wasāʾil (Āl al-Bayt) vols 15 and 16, in the relevant chapters.
  - Makārim al-Akhlāq pp.166 to 200.
  - Man lā yaḥḍuruhu al-faqīh 1 and 3; al-Amālī, al-Khiṣāl, Maʿānī al-Akhbār, Thawāb al-Aʿmāl and ʿUyūn akhbār al-Riḍā (al-Ṣadūq); al-Ṭūsī's Kitāb al-Ghayba.
  - Sistani's *Islamic Laws* (the rulings above).
  - From Marriage to Parenthood, the whole book.
  - duas.org (Thursday ṣalawāt; pregnancy pages).
  - Tanzil (Uthmani text and en.qarai) and the quran.com API.
  - The NHS pages listed under "Health fixes".
- **Could not reach:** al-islam.org (so A Mother's Prayer), shiaonlinelibrary.com, lib.eshia.ir, hadith.net, wikishia, web.archive.org.
- **Skipped on purpose:**
  - Sunni collections and general aggregators (they came up in searches only, to trace where a claim came from).
  - Rayḥāneh-ye Beheshtī and Masāʾil ʿIlmī dar Qurʾān, which are modern popular books, not primary sources.
  - The `faith` lines in `WEEKS`, which are short reflections rather than claims.
