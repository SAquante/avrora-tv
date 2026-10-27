"""Fetch famelack webcam dataset and report its schema."""
import gzip, json, os, urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', '_data', 'cams')
BASE = 'https://raw.githubusercontent.com/famelack/famelack-data/main/webcams/compressed'
UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'


def get(url):
    req = urllib.request.Request(url, headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=30) as resp:
        raw = resp.read()
    return gzip.decompress(raw) if raw[:2] == b'\x1f\x8b' else raw


def main():
    os.makedirs(DATA, exist_ok=True)
    countries = json.load(open(os.path.join(HERE, '..', '_data', 'cams_meta.json'), encoding='utf-8')) \
        if os.path.exists(os.path.join(HERE, '..', '_data', 'cams_meta.json')) else None

    codes = sorted(f[:-5] for f in os.listdir(DATA) if f.endswith('.json'))
    if not codes:
        listing = json.loads(get(f'{BASE}/countries').decode()
                             if False else json.dumps([]))
    total = 0
    sample = None
    for code in codes[:1]:
        data = json.loads(get(f'{BASE}/countries/{code}.json'))
        print(code, type(data), len(data))
        print(json.dumps(data[:1] if isinstance(data, list) else data, ensure_ascii=False)[:1500])


if __name__ == '__main__':
    main()
