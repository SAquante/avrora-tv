"""Parse ytInitialPlayerResponse from a watch page and inspect streaming data."""
import json
import re
import urllib.request

IDS = ['WV465ngatlY', 'AylWnPkXd-U']
UA = ('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 '
      '(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36')


def main():
    for vid in IDS:
        req = urllib.request.Request('https://www.youtube.com/watch?v=' + vid,
                                     headers={'User-Agent': UA,
                                              'Accept-Language': 'en-US,en;q=0.9'})
        html = urllib.request.urlopen(req, timeout=25).read().decode('utf-8', 'replace')
        m = re.search(r'ytInitialPlayerResponse\s*=\s*(\{.+?\});', html, re.S)
        if not m:
            print(vid, 'no player response')
            continue
        try:
            data = json.loads(m.group(1))
        except Exception as exc:  # noqa: BLE001
            print(vid, 'json err', exc)
            continue
        status = data.get('playabilityStatus', {}).get('status')
        streaming = data.get('streamingData', {})
        print(vid, 'status', status, 'reason', data.get('playabilityStatus', {}).get('reason'))
        print('   hls', bool(streaming.get('hlsManifestUrl')),
              'formats', len(streaming.get('formats', [])),
              'adaptive', len(streaming.get('adaptiveFormats', [])))
        if streaming.get('hlsManifestUrl'):
            print('   ', streaming['hlsManifestUrl'][:110])


if __name__ == '__main__':
    main()
