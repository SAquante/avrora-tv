"""Inspect adaptive formats of a live YouTube stream for a direct HLS entry."""
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
        data = json.loads(m.group(1))
        print('==', vid)
        for fmt in data.get('streamingData', {}).get('adaptiveFormats', [])[:6]:
            print('  ', fmt.get('mimeType'), 'url' if fmt.get('url') else 'cipher',
                  (fmt.get('url') or '')[:80])


if __name__ == '__main__':
    main()
