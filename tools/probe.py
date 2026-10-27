"""Probe stream URLs for liveness; usage: python probe.py [lang] [limit]."""
import json, sys, urllib.request, concurrent.futures, time, os

HERE = os.path.dirname(os.path.abspath(__file__))
CATALOG = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'catalog.json')

UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36')


def check(url, user_agent=None):
    req = urllib.request.Request(url, headers={'User-Agent': user_agent or UA})
    started = time.time()
    try:
        with urllib.request.urlopen(req, timeout=6) as resp:
            resp.read(512)
            return resp.status, round(time.time() - started, 2)
    except Exception as e:  # noqa: BLE001 - any failure means dead for our purpose
        return 0, round(time.time() - started, 2)


def main():
    lang = sys.argv[1] if len(sys.argv) > 1 else 'rus'
    limit = int(sys.argv[2]) if len(sys.argv) > 2 else 200
    with open(CATALOG, encoding='utf-8') as f:
        catalog = json.load(f)
    channels = [c for c in catalog['channels'] if lang in c['languages']][:limit]
    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
        futures = {pool.submit(check, c['streams'][0]['url'], c['streams'][0].get('userAgent')): c
                   for c in channels if c['streams']}
        for fut in concurrent.futures.as_completed(futures):
            status, elapsed = fut.result()
            results.append((status, elapsed, futures[fut]['name'], futures[fut]['streams'][0]['url']))
    alive = [r for r in results if r[0] == 200]
    print(f'checked {len(results)}  alive {len(alive)}')
    for r in sorted(alive, key=lambda x: x[1])[:15]:
        print(r[0], r[1], r[2], r[3][:90])


if __name__ == '__main__':
    main()
