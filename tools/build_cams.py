"""Build assets/cams.json from the Famelack webcam dataset (MIT)."""
import gzip, json, os, time, urllib.parse, urllib.request
from collections import Counter

from build_catalog import RU_COUNTRY

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', '_data')
OUT = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'cams.json')
CACHE = os.path.join(DATA, 'cam_names_ru.json')

BASE = 'https://raw.githubusercontent.com/famelack/famelack-data/main/webcams/compressed'
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'


def translate(names):
    """Batch translate names to Russian; results are cached on disk."""
    cache = json.load(open(CACHE, encoding='utf-8')) if os.path.exists(CACHE) else {}
    todo = [n for n in dict.fromkeys(names) if n and n not in cache]
    for start in range(0, len(todo), 50):
        batch = todo[start:start + 50]
        query = '\n'.join(batch)
        url = ('https://translate.googleapis.com/translate_a/single'
               '?client=gtx&sl=en&tl=ru&dt=t&q=' + urllib.parse.quote(query))
        for attempt in range(3):
            try:
                req = urllib.request.Request(url, headers={'User-Agent': UA})
                with urllib.request.urlopen(req, timeout=40) as resp:
                    data = json.load(resp)
                text = ''.join(part[0] for part in data[0] if part and part[0])
                lines = [line.strip().replace('\u00ad', '') for line in text.split('\n') if line.strip()]
                if len(lines) == len(batch):
                    for src, dst in zip(batch, lines):
                        cache[src] = dst
                break
            except Exception as exc:  # noqa: BLE001 - a failed batch just keeps the original name
                print('translate retry', attempt, exc)
                time.sleep(2 + attempt * 3)
        time.sleep(0.4)
    json.dump(cache, open(CACHE, 'w', encoding='utf-8'), ensure_ascii=False)
    return cache


def get_json(url):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=60) as resp:
        raw = resp.read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    return json.loads(raw.decode('utf-8'))


def video_id(embed_url):
    part = embed_url.rsplit('/', 1)[-1]
    return part.split('?')[0].strip()


def main():
    listing = get_json('https://api.github.com/repos/famelack/famelack-data/contents/webcams/compressed/countries')
    codes = [item['name'][:-5] for item in listing if item['name'].endswith('.json')]

    meta = get_json(f'{BASE}/countries_metadata.json')
    name_of = {}
    if isinstance(meta, dict):
        for code, info in meta.items():
            name_of[code] = info.get('name') if isinstance(info, dict) else info
    elif isinstance(meta, list):
        for info in meta:
            name_of[info.get('code') or info.get('id')] = info.get('name')

    iptv_names = {c['code'].lower(): c['name'] for c in json.load(
        open(os.path.join(DATA, 'countries.json'), encoding='utf-8'))}

    cams = []
    for code in sorted(codes):
        try:
            items = get_json(f'{BASE}/countries/{code}.json')
        except Exception as exc:  # noqa: BLE001 - a missing country file must not kill the build
            print('skip', code, exc)
            continue
        for item in items:
            sources = item.get('sources') or {}
            embed = (sources.get('youtube') or [None])[0]
            if not embed or item.get('isGeoBlocked'):
                continue
            cams.append({
                'id': item.get('nanoid'),
                'name': (item.get('name') or '').strip()[:90],
                'country': code,
                'video': video_id(embed),
            })

    counts = Counter(c['country'] for c in cams)
    names_ru = translate([c['name'] for c in cams])
    for cam in cams:
        cam['name'] = names_ru.get(cam['name']) or cam['name']
    countries = [
        {
            'id': code,
            'name': RU_COUNTRY.get(code.upper()) or iptv_names.get(code)
            or name_of.get(code) or code.upper(),
            'count': n,
        }
        for code, n in counts.most_common()
    ]

    catalog = {'countries': countries, 'cams': cams}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(catalog, f, ensure_ascii=False, separators=(',', ':'))
    print('cams', len(cams), 'countries', len(countries), 'bytes', os.path.getsize(OUT))
    print('top', [(c['name'], c['count']) for c in countries[:6]])


if __name__ == '__main__':
    main()
