"""Country distribution of channels tagged with a language + see what else they speak."""
import json, os, sys
from collections import Counter

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', '_data')
CATALOG = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'catalog.json')


def main():
    code = sys.argv[1] if len(sys.argv) > 1 else 'rus'
    channels = json.load(open(os.path.join(DATA, 'channels.json'), encoding='utf-8'))
    countries = {c['code']: c['name'] for c in json.load(open(os.path.join(DATA, 'countries.json'), encoding='utf-8'))}
    country_of = {c['id']: c.get('country') for c in channels}

    catalog = json.load(open(CATALOG, encoding='utf-8'))
    tagged = [c for c in catalog['channels'] if code in c['languages']]
    dist = Counter(country_of.get(c['id']) for c in tagged)
    print(f'{len(tagged)} channels tagged {code}')
    for cc, n in dist.most_common(15):
        print(f'  {cc} {countries.get(cc, "?")}: {n}')

    single = [c for c in tagged if len(c['languages']) == 1]
    print('with exactly one language:', len(single))


if __name__ == '__main__':
    main()
