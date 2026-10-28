"""Second-pass liveness: a stream must load twice, three seconds apart.

A single success is not enough: some providers answer a playlist and then fail
intermittently, which is exactly what "иногда не играет" looks like in the app.
"""
import json, os, time, urllib.parse, urllib.request
import concurrent.futures

HERE = os.path.dirname(os.path.abspath(__file__))
CATALOG = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'catalog.json')
OUT = os.path.join(HERE, '..', '_data', 'alive_stable.json')

UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36')
TIMEOUT = 10
WORKERS = 32
PAUSE = 3


def fetch(url, headers, max_bytes=131072):
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
        return response.status, response.read(max_bytes)


def first_uri(body):
    for line in body.splitlines():
        line = line.strip()
        if line and not line.startswith('#'):
            return line
    return None


def single_pass(url, user_agent):
    headers = {'User-Agent': user_agent or UA, 'Accept': '*/*'}
    status, data = fetch(url, headers)
    if status != 200:
        return False
    if '#EXTM3U' not in data[:4096].decode('utf-8', 'ignore'):
        return len(data) > 512
    body = data.decode('utf-8', 'ignore')
    base = url
    if '#EXT-X-STREAM-INF' in body:
        variant = first_uri(body)
        if not variant:
            return False
        base = urllib.parse.urljoin(url, variant)
        status, data = fetch(base, headers)
        if status != 200:
            return False
        body = data.decode('utf-8', 'ignore')
    segment = first_uri(body)
    if not segment:
        return False
    status, chunk = fetch(urllib.parse.urljoin(base, segment), headers, max_bytes=4096)
    return status in (200, 206) and len(chunk) > 0


def probe(job):
    url, user_agent = job
    try:
        if not single_pass(url, user_agent):
            return url, False
        time.sleep(PAUSE)
        return url, single_pass(url, user_agent)
    except Exception:  # noqa: BLE001 - any failure means the player would fail too
        return url, False


def main():
    catalog = json.load(open(CATALOG, encoding='utf-8'))
    jobs, seen = [], set()
    for channel in catalog['channels']:
        for stream in channel['streams']:
            if stream['url'] not in seen:
                seen.add(stream['url'])
                jobs.append((stream['url'], stream.get('userAgent')))

    print('double-probing', len(jobs), 'streams', flush=True)
    started = time.time()
    alive, done = {}, 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS) as pool:
        for url, ok in pool.map(probe, jobs):
            alive[url] = ok
            done += 1
            if done % 500 == 0:
                print(f'{done}/{len(jobs)} stable={sum(alive.values())} '
                      f'[{int(time.time() - started)}s]', flush=True)

    json.dump(alive, open(OUT, 'w', encoding='utf-8'))
    print('done', sum(alive.values()), 'of', len(alive), 'in', int(time.time() - started), 's')


if __name__ == '__main__':
    main()
