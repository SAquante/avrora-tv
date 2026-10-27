"""Compare catalog language tags against the source language playlist."""
import json, os, re, sys
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', '_data')
CATALOG = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'catalog.json')
ATTR = re.compile(r'([\w-]+)="([^"]*)"')


def split_title(line):
    in_quotes = False
    last = -1
    for i, ch in enumerate(line):
        if ch == '"':
            in_quotes = not in_quotes
        elif ch == ',' and not in_quotes:
            last = i
    return line[last + 1:].strip() if last >= 0 else ''


def parse(path):
    out = []
    cur = None
    for line in open(path, encoding='utf-8-sig', errors='replace'):
        line = line.strip()
        if line.startswith('#EXTINF:'):
            cur = {'attrs': dict(ATTR.findall(line)), 'title': split_title(line)}
        elif line and not line.startswith('#') and cur is not None:
            cur['url'] = line
            out.append(cur)
            cur = None
    return out


def main():
    lang_name = sys.argv[1] if len(sys.argv) > 1 else 'Russian'
    langs = parse(os.path.join(DATA, 'index.language.m3u'))
    by_tvg = defaultdict(set)
    for e in langs:
        tvg = e['attrs'].get('tvg-id', '').split('@')[0]
        if tvg:
            by_tvg[tvg].add(e['attrs'].get('group-title', ''))

    with open(CATALOG, encoding='utf-8') as f:
        catalog = json.load(f)
    code = {'Russian': 'rus', 'English': 'eng', 'Spanish': 'spa'}[lang_name]
    tagged = [c for c in catalog['channels'] if code in c['languages']]
    print(f'{lang_name}: catalog {len(tagged)} channels, source tvg-ids {sum(1 for v in by_tvg.values() if lang_name in v)}')

    not_in_source = [c for c in tagged if lang_name not in by_tvg.get(c['id'], set())]
    print('tagged but absent from source language:', len(not_in_source))
    for c in not_in_source[:10]:
        print('  ', c['id'], c['name'], sorted(by_tvg.get(c['id'], []))[:4])

    share = Counter(len(by_tvg.get(c['id'], set())) for c in tagged)
    print('languages per tagged channel:', sorted(share.items())[:6])

    samples = [c for c in tagged if len(by_tvg.get(c['id'], set())) > 2][:10]
    print('multi-language examples:', [(c['name'], sorted(by_tvg.get(c['id'], set()))[:5]) for c in samples])


if __name__ == '__main__':
    main()
