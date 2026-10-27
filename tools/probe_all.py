"""Probe every stream in the catalog and record which URLs answer."""
import json, os, socket, ssl, sys, time, urllib.request, urllib.error
import concurrent.futures

HERE = os.path.dirname(os.path.abspath(__file__))
CATALOG = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'catalog.json')
OUT = os.path.join(HERE, '..', '_data', 'alive.json')

UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36')
TIMEOUT = 8
WORKERS = 48


def probe(job):
    url, user_agent = job
    req = urllib.request.Request(url, headers={'User-Agent': user_agent or UA})
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
            resp.read(1024)
            return url, resp.status == 200
    except Exception:  # noqa: BLE001 - any failure counts as dead for the user
        return url, False


def main():
    with open(CATALOG, encoding='utf-8') as f:
        catalog = json.load(f)

    jobs = []
    seen = set()
    for channel in catalog['channels']:
        for stream in channel['streams'][:2]:
            url = stream['url']
            if url in seen:
                continue
            seen.add(url)
            jobs.append((url, stream.get('userAgent')))

    print('probing', len(jobs), 'streams', flush=True)
    started = time.time()
    alive = {}
    done = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS) as pool:
        for url, ok in pool.map(probe, jobs):
            alive[url] = ok
            done += 1
            if done % 500 == 0:
                print(f'{done}/{len(jobs)} alive={sum(alive.values())} '
                      f'[{int(time.time() - started)}s]', flush=True)

    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(alive, f)
    print('done', sum(alive.values()), 'of', len(alive), 'alive in',
          int(time.time() - started), 's')


if __name__ == '__main__':
    main()
