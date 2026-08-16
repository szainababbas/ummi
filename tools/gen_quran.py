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
        vs.append('  {n:%d,ar:%s,tl:%s,en:%s}' % (v['n'], js(ar), js(tl), js(v['qarai'])))
    lines.append(' %d:{no:%d,name:%s,ar:%s,meaning:%s,bism:%s,v:[' % (
        n, n, js(nm), js(s['name']), js(mn), 'false' if n == 1 else 'true'))
    lines.append(',\n'.join(vs))
    lines.append(' ]},')
lines.append('};')

lines.append('const AYAHS = {')
for ref, a in src['ayahs'].items():
    lines.append(' %s:{ref:%s,ar:%s,tl:%s,en:%s},' % (
        js(ref), js('Sūrah ' + a['surah'] + ' ' + ref), js(a['ar']), js(a['tl']), js(a['qarai'])))
lines.append('};')

open('gen_quran.js', 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
print('\n'.join(lines[:10]))
print('...')
print('bytes', len('\n'.join(lines)))
