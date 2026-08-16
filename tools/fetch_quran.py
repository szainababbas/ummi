import json, urllib.request, time, sys
sys.stdout.reconfigure(encoding='utf-8')

EDITIONS = "quran-uthmani,en.transliteration,en.qarai,en.sarwar"
SURAHS = [1, 95, 97, 103, 108, 110, 112]          # short surahs, full text
AYAHS  = ["2:255","3:36","3:38","14:40","16:69","19:25","20:25","20:26",
          "25:74","36:36","37:100","46:15","94:5","94:6"]

def get(url):
    for attempt in range(3):
        try:
            with urllib.request.urlopen(url, timeout=30) as r:
                return json.loads(r.read().decode('utf-8'))
        except Exception as e:
            print('retry', url, e); time.sleep(2)
    raise SystemExit('failed: '+url)

out = {'surahs': {}, 'ayahs': {}}

for s in SURAHS:
    d = get(f"https://api.alquran.cloud/v1/surah/{s}/editions/{EDITIONS}")['data']
    ar, tr, qa, sa = d[0], d[1], d[2], d[3]
    out['surahs'][str(s)] = {
        'number': s,
        'name': ar['name'],
        'englishName': ar['englishName'],
        'englishNameTranslation': ar['englishNameTranslation'],
        'verses': [
            {'n': ar['ayahs'][i]['numberInSurah'],
             'ar': ar['ayahs'][i]['text'],
             'tl': tr['ayahs'][i]['text'],
             'qarai': qa['ayahs'][i]['text'],
             'sarwar': sa['ayahs'][i]['text']}
            for i in range(len(ar['ayahs']))
        ]
    }
    print('surah', s, 'ok', len(out['surahs'][str(s)]['verses']), 'verses')

for ref in AYAHS:
    d = get(f"https://api.alquran.cloud/v1/ayah/{ref}/editions/{EDITIONS}")['data']
    out['ayahs'][ref] = {
        'ar': d[0]['text'], 'tl': d[1]['text'],
        'qarai': d[2]['text'], 'sarwar': d[3]['text'],
        'surah': d[0]['surah']['englishName'], 'surahNum': d[0]['surah']['number'],
        'ayahNum': d[0]['numberInSurah']}
    print('ayah', ref, 'ok')

json.dump(out, open('quran_source.json','w',encoding='utf-8'), ensure_ascii=False, indent=1)
print('WROTE quran_source.json')
