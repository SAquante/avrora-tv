"""Build assets/radio.json from radio-browser.info.

Every country gets its own quota, so small countries are represented too instead
of only the globally popular stations.
"""
import json, os, time, urllib.parse, urllib.request
from collections import Counter

from build_catalog import RU_COUNTRY

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', '_data')
OUT = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'radio.json')

BASE = 'https://de1.api.radio-browser.info/json'
UA = 'AvroraTV/1.0'
PER_COUNTRY = 120
GLOBAL_TOP = 2000


def get_json(url, retries=3):
    for attempt in range(retries):
        try:
            request = urllib.request.Request(url, headers={'User-Agent': UA})
            with urllib.request.urlopen(request, timeout=40) as response:
                return json.load(response)
        except Exception as exc:  # noqa: BLE001 - retry, then give up on this query
            if attempt == retries - 1:
                print('  skip', url.split('?')[-1][:60], exc)
                return []
            time.sleep(1 + attempt)
    return []


def search(params):
    return get_json(f'{BASE}/stations/search?' + urllib.parse.urlencode(params))


def main():
    countries = get_json(f'{BASE}/countries')
    iptv_names = {c['code'].upper(): c['name'] for c in json.load(
        open(os.path.join(DATA, 'countries.json'), encoding='utf-8'))}

    stations, seen = [], set()

    def add(items):
        for item in items:
            url = (item.get('url_resolved') or item.get('url') or '').strip()
            name = (item.get('name') or '').strip()
            key = item.get('stationuuid') or url
            if not url or not name or key in seen:
                continue
            seen.add(key)
            stations.append({
                'id': key,
                'name': name[:80],
                'country': (item.get('countrycode') or '').lower(),
                'url': url,
                'tags': [t.strip() for t in (item.get('tags') or '').split(',') if t.strip()][:3],
                'codec': (item.get('codec') or '').upper(),
                'bitrate': int(item.get('bitrate') or 0),
                'language': (item.get('languagecodes') or '').split(',')[0].strip().lower(),
            })

    print('global top by votes', flush=True)
    add(search({'limit': GLOBAL_TOP, 'hidebroken': 'true', 'order': 'votes', 'reverse': 'true'}))

    for index, country in enumerate(countries, 1):
        code = (country.get('iso_3166_1') or '').lower()
        if not code or code == 'none':
            continue
        add(search({
            'countrycode': code.upper(),
            'limit': PER_COUNTRY,
            'hidebroken': 'true',
            'order': 'clickcount',
            'reverse': 'true',
        }))
        if index % 25 == 0:
            print(f'  {index} countries, {len(stations)} stations', flush=True)
        time.sleep(0.15)

    counts = Counter(s['country'] for s in stations if s['country'])
    country_list = [
        {
            'id': code,
            'name': RU_COUNTRY.get(code.upper()) or iptv_names.get(code.upper(), code.upper()),
            'count': n,
        }
        for code, n in counts.most_common()
    ]

    catalog = {'countries': country_list, 'stations': stations}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(catalog, f, ensure_ascii=False, separators=(',', ':'))
    print('stations', len(stations), 'countries', len(country_list),
          'bytes', os.path.getsize(OUT))
    print('top', [(c['name'], c['count']) for c in country_list[:8]])


if __name__ == '__main__':
    main()
