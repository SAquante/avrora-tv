"""Deep liveness check: a stream counts as alive only if its first media segment loads.

A plain 200 on the HLS manifest is not enough: many channels answer with a playlist
while their segments are geo-blocked or expired, and the player then fails.
"""
import json, os, time, urllib.parse, urllib.request
import concurrent.futures

HERE = os.path.dirname(os.path.abspath(__file__))
CATALOG = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'catalog.json')
OUT = os.path.join(HERE, '..', '_data', 'alive_deep.json')

UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36')
TIMEOUT = 10
WORKERS = 32


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


def probe(job):
    url, user_agent = job
    headers = {'User-Agent': user_agent or UA, 'Accept': '*/*'}
    try:
        status, data = fetch(url, headers)
        if status != 200:
            return url, False
        head = data[:4096].decode('utf-8', 'ignore')
        if '#EXTM3U' not in head:
            return url, len(data) > 512

        body = data.decode('utf-8', 'ignore')
        base = url
        if '#EXT-X-STREAM-INF' in body:
            variant = first_uri(body)
            if not variant:
                return url, False
            base = urllib.parse.urljoin(url, variant)
            status, data = fetch(base, headers)
            if status != 200:
                return url, False
            body = data.decode('utf-8', 'ignore')
        elif '#EXT-X-MEDIA' in body and '#EXT-X-STREAM-INF' not in body:
            # audio-only master, take its rendition
            variant = first_uri(body)
            if variant:
                base = urllib.parse.urljoin(url, variant)

        segment = first_uri(body)
        if not segment:
            return url, False
        segment_url = urllib.parse.urljoin(base, segment)
        status, chunk = fetch(segment_url, headers, max_bytes=4096)
        return url, status in (200, 206) and len(chunk) > 0
    except Exception:  # noqa: BLE001 - any failure means the player would fail too
        return url, False


def main():
    with open(CATALOG, encoding='utf-8') as f:
        catalog = json.load(f)

    jobs, seen = [], set()
    for channel in catalog['channels']:
        for stream in channel['streams']:
            if stream['url'] not in seen:
                seen.add(stream['url'])
                jobs.append((stream['url'], stream.get('userAgent')))

    print('deep-probing', len(jobs), 'streams', flush=True)
    started = time.time()
    alive, done = {}, 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=WORKERS) as pool:
        for url, ok in pool.map(probe, jobs):
            alive[url] = ok
            done += 1
            if done % 500 == 0:
                print(f'{done}/{len(jobs)} alive={sum(alive.values())} '
                      f'[{int(time.time() - started)}s]', flush=True)

    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(alive, f)
    print('done', sum(alive.values()), 'of', len(alive),
          'in', int(time.time() - started), 's')


if __name__ == '__main__':
    main()
