"""Try to read hlsManifestUrl straight from YouTube watch pages."""
import re
import urllib.request

IDS = ['WV465ngatlY', 'AylWnPkXd-U', 'sI7oCUe1dmo', 'jfKfPfyJRdk']
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36')


def main():
    for vid in IDS:
        url = 'https://www.youtube.com/watch?v=' + vid
        req = urllib.request.Request(url, headers={'User-Agent': UA,
                                                   'Accept-Language': 'en-US,en;q=0.9'})
        try:
            html = urllib.request.urlopen(req, timeout=25).read().decode('utf-8', 'replace')
        except Exception as exc:  # noqa: BLE001
            print(vid, 'ERR', exc)
            continue
        m = re.search(r'"hlsManifestUrl":"(.*?)"', html)
        print(vid, 'len', len(html), 'isLive', '"isLive":true' in html,
              'hls', bool(m))
        if m:
            print('   ', m.group(1)[:100])


if __name__ == '__main__':
    main()
