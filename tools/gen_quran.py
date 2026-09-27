import json, sys, re
sys.stdout.reconfigure(encoding='utf-8')
src = json.load(open('quran_source.json', encoding='utf-8'))

BISM = 'بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ'
BISM_TL = 'Bismillāhir-Raḥmānir-Raḥīm'


# alquran.cloud prefixes the basmalah to ayah 1 of every surah except al-Fatiha (1)
# and at-Tawbah (9). The prefixed copy is not byte-identical between surahs (97 and 95
# carry an extra shadda, some carry a BOM), so compare on the bare letters instead.
MARKS = re.compile(r'[ً-ٰٟۖ-ۭ﻿]')
BISM_WORDS = ['بسم', 'الله', 'الرحمن', 'الرحيم']


def bare(word):
    return MARKS.sub('', word).replace('ٱ', 'ا').replace('ا', 'ا')


def strip_bism(surah_no, verse_no, text):
    if surah_no == 1 or verse_no != 1:
        return text
    words = text.replace('﻿', '').split(' ')
    got = [bare(w) for w in words[:4]]
    if got != BISM_WORDS:
        raise SystemExit('basmalah not matched in surah %d: %r' % (surah_no, got))
    return ' '.join(words[4:]).strip()


def strip_bism_tl(surah_no, verse_no, text):
    if surah_no != 1 and verse_no == 1:
        return re.sub(r'^Bismil+aahir\s+Rahmaanir\s+Raheem\s*', '', text, flags=re.I).strip()
    return text


# Surah names in the app's own spelling, for the AYAHS ref labels. The API's
# englishName ("Aal-i-Imraan", "Taa-Haa") does not match the rest of the app.
REF_NAMES = {2: 'al-Baqarah', 3: 'Āl ʿImrān', 14: 'Ibrāhīm', 16: 'an-Naḥl', 19: 'Maryam',
             20: 'Ṭā Hā', 25: 'al-Furqān', 36: 'Yāsīn', 37: 'aṣ-Ṣāffāt', 46: 'al-Aḥqāf',
             94: 'ash-Sharḥ'}

# The API's Arabic for 16:69 writes a standalone hamza where the Madinah muṣḥaf (and
# Tanzil and quran.com, checked 2026-09-27) seat it on a tatweel. Same sound.
AR_FIXES = {'16:69': ('لَءَايَةًۭ', 'لَـَٔايَةًۭ')}

# 16:69 in Qarāʾī closes a quotation opened in 16:68, which reads as a stray mark here.
EN_FIXES = {'16:69': ('your Lord.’ There', 'your Lord. There')}

# The API's transliteration (en.transliteration) has wrong words and odd splits in
# these verses, found in the 2026-09-27 audit (docs/SOURCES.md). Each replaces the
# whole verse; the style (z/s for ذ/ث, doubled vowels) is the API's own.
TL_FIXES = {
    '2:255': "Allahu laaa ilaaha illaa Huwal Haiyul Qaiyoom; laa taakhuzuhoo sinatunw wa laa nawm; lahoo maa fissamaawaati wa maa fil ard; man zal lazee yashfa'u 'indahooo illaa bi-iznih; ya'lamu maa baina aydeehim wa maa khalfahum wa laa yuheetoona bishai'im min 'ilmihee illaa bimaa shaaa'; wasi'a Kursiyyuhus samaawaati wal arda wa laa ya'ooduhoo hifzuhumaa; wa Huwal 'Aliyyul 'Azeem",
    '3:36': "Falammaa wada'at-haa qaalat Rabbi innee wada'tuhaaa unsaa wallaahu a'lamu bimaa wada'at wa laisaz zakaru kalunsaa wa innee sammaituhaa Maryama wa innee u'eezuhaa bika wa zurriyyatahaa minash Shaitaanir Rajeem",
    '3:38': "Hunaalika da'aa Zakariyyaa Rabbahoo qaala Rabbi hab lee mil ladunka zurriyyatan taiyibatan innaka samee'ud du'aaa'",
    '16:69': "Summa kulee min kullis samaraati faslukee subula Rabbiki zululaa; yakhruju mim butoonihaa sharaabum mukhtalifun alwaanuhoo feehi shifaaa'ul linnaas, inna fee zaalika la-aayatal liqawminy yatafakkaroon",
    '19:25': "Wa huzzeee ilaiki bijiz'in nakhlati tusaaqit 'alaiki rutaban janiyyaa",
    '46:15': "Wa wassainal insaana biwaalidaihi ihsaanan hamalathu ummuhoo kurhanw-wa wada'athu kurhanw wa hamluhoo wa fisaaluhoo salaasoona shahraa; hattaaa izaa balagha ashuddahoo wa balagha arba'eena sanatan qaala Rabbi awzi'neee an ashkura ni'matakal lateee an'amta 'alaiya wa 'alaa waalidaiya wa an a'mala saalihan tardaahu wa aslih lee fee zurriyyatee innee tubtu ilaika wa innee minal muslimeen",
    '94:5': "Fa inna ma'al 'usri yusra",
    '95:3': "Wa haazal baladil ameen",
    '95:5': "Thumma radadnaahu asfala saafileen",
    '95:6': "Illal lazeena aamanoo wa 'amilus saalihaati falahum ajrun ghairu mamnoon",
    '95:7': "Famaa yukazzibuka ba'du bid deen",
    '95:8': "Alaisal laahu bi-ahkamil haakimeen",
    '97:2': "Wa maaa adraaka maa lailatul qadr",
    '97:3': "Lailatul qadri khairum min alfi shahr",
    '97:4': "Tanazzalul malaaa'ikatu war roohu feehaa bi-izni Rabbihim min kulli amr",
    '97:5': "Salaamun hiya hattaa matla'il fajr",
    '103:1': "Wal 'Asr",
    '103:3': "Illal lazeena aamanoo wa 'amilus saalihaati wa tawaasaw bil haqqi wa tawaasaw bis sabr",
    '108:1': "Innaaa a'tainaakal Kauthar",
    '108:3': "Inna shaani'aka huwal abtar",
    '110:1': "Izaa jaaa'a nasrul laahi wal fath",
    '110:2': "Wa ra-aitan naasa yadkhuloona fee deenil laahi afwaajaa",
    '110:3': "Fasabbih bihamdi Rabbika wastaghfirhu, innahoo kaana tawwaabaa",
    '112:2': "Allaahus Samad",
}
USED = set()


def fixed(ref, ar, tl, en):
    if ref in AR_FIXES:
        old, new = AR_FIXES[ref]
        if old not in ar:
            raise SystemExit('Arabic fix for %s no longer matches the source' % ref)
        ar = ar.replace(old, new)
        USED.add('ar' + ref)
    if ref in EN_FIXES:
        old, new = EN_FIXES[ref]
        if old not in en:
            raise SystemExit('English fix for %s no longer matches the source' % ref)
        en = en.replace(old, new)
        USED.add('en' + ref)
    if ref in TL_FIXES:
        tl = TL_FIXES[ref]
        USED.add('tl' + ref)
    return ar, tl, en


NAMES = {
    1: ('Al-Fātiḥah', 'The Opening'),
    95: ('At-Tīn', 'The Fig'),
    97: ('Al-Qadr', 'The Ordainment'),
    103: ('Al-ʿAṣr', 'Time'),
    108: ('Al-Kawthar', 'Abundance'),
    110: ('An-Naṣr', 'Help'),
    112: ('Al-Ikhlāṣ', 'Monotheism'),
}

BS = chr(92)   # backslash
SQ = chr(39)   # single quote


def js(s):
    s = s.replace('﻿', '')
    # the API's transliteration edition has a stray OCR slip in al-Fatiha 1:5
    s = s.replace('wa lyyaaka', 'wa iyyaaka')
    s = s.replace(BS, BS + BS).replace(SQ, BS + SQ)
    return SQ + s + SQ


lines = []
lines.append('/* ============ DATA: QURʾĀN ============ */')
lines.append('/* Arabic: Uthmani text. Translation: ʿAlī Qulī Qaraʾī (the standard Shiʿi English')
lines.append('   rendering, used on al-islam.org). Text, transliteration and translation come')
lines.append('   from the alquran.cloud API, not typed by hand. Generated — do not hand-edit. */')
lines.append('const BISMILLAH = {ar:%s,tl:%s,en:%s};' % (
    js(BISM), js(BISM_TL), js('In the Name of Allah, the All-beneficent, the All-merciful.')))
lines.append('const SURAHS = {')
for n in [1, 95, 97, 103, 108, 110, 112]:
    s = src['surahs'][str(n)]
    nm, mn = NAMES[n]
    vs = []
    for v in s['verses']:
        ar = strip_bism(n, v['n'], v['ar'])
        tl = strip_bism_tl(n, v['n'], v['tl'])
        ar, tl, en = fixed('%d:%d' % (n, v['n']), ar, tl, v['qarai'])
        vs.append('  {n:%d,ar:%s,tl:%s,en:%s}' % (v['n'], js(ar), js(tl), js(en)))
    lines.append(' %d:{no:%d,name:%s,ar:%s,meaning:%s,bism:%s,v:[' % (
        n, n, js(nm), js(s['name']), js(mn), 'false' if n == 1 else 'true'))
    lines.append(',\n'.join(vs))
    lines.append(' ]},')
lines.append('};')

lines.append('const AYAHS = {')
for ref, a in src['ayahs'].items():
    ar, tl, en = fixed(ref, a['ar'], a['tl'], a['qarai'])
    name = REF_NAMES[int(ref.split(':')[0])]
    lines.append(' %s:{ref:%s,ar:%s,tl:%s,en:%s},' % (
        js(ref), js('Sūrah ' + name + ' ' + ref), js(ar), js(tl), js(en)))
lines.append('};')

unused = [k for k in list(TL_FIXES) if 'tl' + k not in USED] + \
         [k for k in list(AR_FIXES) if 'ar' + k not in USED] + \
         [k for k in list(EN_FIXES) if 'en' + k not in USED]
if unused:
    raise SystemExit('fixes for verses not in the output: %r' % unused)

open('gen_quran.js', 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
print('\n'.join(lines[:10]))
print('...')
print('bytes', len('\n'.join(lines)))
