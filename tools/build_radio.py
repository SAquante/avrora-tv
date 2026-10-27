"""Build assets/radio.json from radio-browser.info (community, keyless)."""
import json, os, time, urllib.parse, urllib.request
from collections import Counter

from build_catalog import RU_COUNTRY

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', '_data')
OUT = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'radio.json')

API = 'https://de1.api.radio-browser.info/json/stations/search'
UA = 'AvroraTV/1.0 (+https://github.com/iptv-org)'
PAGE = 500
TARGET = 9000


def fetch(offset):
    query = urllib.parse.urlencode({
        'limit': PAGE,
        'offset': offset,
        'hidebroken': 'true',
        'order': 'clickcount',
        'reverse': 'true',
    })
    req = urllib.request.Request(f'{API}?{query}', headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=45) as resp:
        return json.load(resp)


def main():
    stations = []
    offset = 0
    while len(stations) < TARGET:
        try:
            page = fetch(offset)
        except Exception as exc:  # noqa: BLE001 - a failed page just ends the crawl
            print('stop at', offset, exc)
            break
        if not page:
            break
        stations.extend(page)
        offset += PAGE
        print('fetched', len(stations), flush=True)
        time.sleep(0.4)

    countries = {c['code'].upper(): c['name'] for c in json.load(
        open(os.path.join(DATA, 'countries.json'), encoding='utf-8'))}

    out = []
    seen = set()
    for item in stations:
        url = (item.get('url_resolved') or item.get('url') or '').strip()
        name = (item.get('name') or '').strip()
        if not url or not name or url in seen:
            continue
        seen.add(url)
        code = (item.get('countrycode') or '').lower()
        tags = [t.strip() for t in (item.get('tags') or '').split(',') if t.strip()][:3]
        out.append({
            'id': item.get('stationuuid') or url,
            'name': name[:80],
            'country': code,
            'url': url,
            'tags': tags,
            'codec': (item.get('codec') or '').upper(),
            'bitrate': int(item.get('bitrate') or 0),
            'language': (item.get('languagecodes') or '').split(',')[0].strip().lower(),
        })

    counts = Counter(s['country'] for s in out if s['country'])
    country_list = [
        {
            'id': code,
            'name': RU_COUNTRY.get(code.upper()) or countries.get(code.upper(), code.upper()),
            'count': n,
        }
        for code, n in counts.most_common()
    ]

    catalog = {'countries': country_list, 'stations': out}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(catalog, f, ensure_ascii=False, separators=(',', ':'))
    print('stations', len(out), 'countries', len(country_list),
          'bytes', os.path.getsize(OUT))


if __name__ == '__main__':
    main()
