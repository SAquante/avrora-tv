"""Build app/src/main/assets/catalog.json from iptv-org data.

Language of a channel comes from feeds.csv (per-feed, primary language first),
because the generated language playlists list every secondary language too.
"""
import csv, json, os, re, time, urllib.parse, urllib.request
from collections import OrderedDict, Counter

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, '..', '_data')
OUT = os.path.join(HERE, '..', 'app', 'src', 'main', 'assets', 'catalog.json')
NAME_CACHE = os.path.join(DATA, 'channel_names_ru.json')


def translate_names(names, cache_path=NAME_CACHE):
    """Batch translate channel names to Russian; results are cached on disk."""
    cache = json.load(open(cache_path, encoding='utf-8')) if os.path.exists(cache_path) else {}
    todo = [n for n in dict.fromkeys(names) if n and n not in cache]
    print('translating', len(todo), 'names', flush=True)
    for start in range(0, len(todo), 50):
        batch = todo[start:start + 50]
        url = ('https://translate.googleapis.com/translate_a/single'
               '?client=gtx&sl=en&tl=ru&dt=t&q=' + urllib.parse.quote('\n'.join(batch)))
        for attempt in range(3):
            try:
                req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
                with urllib.request.urlopen(req, timeout=45) as resp:
                    data = json.load(resp)
                text = ''.join(part[0] for part in data[0] if part and part[0])
                lines = [line.strip().replace('\u00ad', '') for line in text.split('\n') if line.strip()]
                if len(lines) == len(batch):
                    for src, dst in zip(batch, lines):
                        cache[src] = dst
                break
            except Exception as exc:  # noqa: BLE001 - a failed batch keeps the original name
                print('translate retry', attempt, exc)
                time.sleep(2 + attempt * 3)
        if start % 1000 == 0:
            print('  done', start, flush=True)
        time.sleep(0.35)
    json.dump(cache, open(cache_path, 'w', encoding='utf-8'), ensure_ascii=False)
    return cache

ATTR = re.compile(r'([\w-]+)="([^"]*)"')

RU_LANG = {
    'Russian': 'Русский', 'English': 'Английский', 'Spanish': 'Испанский',
    'Portuguese': 'Португальский', 'French': 'Французский', 'German': 'Немецкий',
    'Italian': 'Итальянский', 'Arabic': 'Арабский', 'Hindi': 'Хинди',
    'Chinese': 'Китайский', 'Ukrainian': 'Украинский', 'Turkish': 'Турецкий',
    'Polish': 'Польский', 'Japanese': 'Японский', 'Korean': 'Корейский',
    'Dutch': 'Нидерландский', 'Swedish': 'Шведский', 'Norwegian': 'Норвежский',
    'Danish': 'Датский', 'Finnish': 'Финский', 'Czech': 'Чешский',
    'Slovak': 'Словацкий', 'Serbian': 'Сербский', 'Croatian': 'Хорватский',
    'Bulgarian': 'Болгарский', 'Romanian': 'Румынский', 'Hungarian': 'Венгерский',
    'Greek': 'Греческий', 'Hebrew': 'Иврит', 'Persian': 'Персидский',
    'Kazakh': 'Казахский', 'Uzbek': 'Узбекский', 'Azerbaijani': 'Азербайджанский',
    'Armenian': 'Армянский', 'Georgian': 'Грузинский', 'Belarusian': 'Белорусский',
    'Lithuanian': 'Литовский', 'Latvian': 'Латышский', 'Estonian': 'Эстонский',
    'Indonesian': 'Индонезийский', 'Vietnamese': 'Вьетнамский', 'Thai': 'Тайский',
    'Bengali': 'Бенгальский', 'Tamil': 'Тамильский', 'Telugu': 'Телугу',
    'Malayalam': 'Малаялам', 'Urdu': 'Урду', 'Swahili': 'Суахили',
    'Albanian': 'Албанский', 'Macedonian': 'Македонский', 'Slovenian': 'Словенский',
    'Icelandic': 'Исландский', 'Irish': 'Ирландский', 'Catalan': 'Каталанский',
    'Basque': 'Баскский', 'Galician': 'Галисийский', 'Afrikaans': 'Африкаанс',
}

RU_COUNTRY = {
    'RU': 'Россия', 'UA': 'Украина', 'KZ': 'Казахстан', 'BY': 'Беларусь',
    'KG': 'Киргизия', 'UZ': 'Узбекистан', 'AM': 'Армения', 'GE': 'Грузия',
    'AZ': 'Азербайджан', 'MD': 'Молдова', 'LV': 'Латвия', 'LT': 'Литва',
    'EE': 'Эстония', 'DE': 'Германия', 'US': 'США', 'GB': 'Великобритания',
    'FR': 'Франция', 'IT': 'Италия', 'ES': 'Испания', 'PT': 'Португалия',
    'NL': 'Нидерланды', 'PL': 'Польша', 'CZ': 'Чехия', 'SK': 'Словакия',
    'HU': 'Венгрия', 'RO': 'Румыния', 'BG': 'Болгария', 'RS': 'Сербия',
    'HR': 'Хорватия', 'SI': 'Словения', 'BA': 'Босния', 'MK': 'Македония',
    'AL': 'Албания', 'GR': 'Греция', 'TR': 'Турция', 'IL': 'Израиль',
    'IR': 'Иран', 'IQ': 'Ирак', 'SA': 'Саудовская Аравия', 'AE': 'ОАЭ',
    'QA': 'Катар', 'KW': 'Кувейт', 'EG': 'Египет', 'MA': 'Марокко',
    'DZ': 'Алжир', 'TN': 'Тунис', 'ZA': 'ЮАР', 'NG': 'Нигерия',
    'KE': 'Кения', 'IN': 'Индия', 'PK': 'Пакистан', 'BD': 'Бангладеш',
    'CN': 'Китай', 'JP': 'Япония', 'KR': 'Корея', 'TW': 'Тайвань',
    'HK': 'Гонконг', 'TH': 'Таиланд', 'VN': 'Вьетнам', 'ID': 'Индонезия',
    'MY': 'Малайзия', 'PH': 'Филиппины', 'SG': 'Сингапур', 'AU': 'Австралия',
    'NZ': 'Новая Зеландия', 'CA': 'Канада', 'MX': 'Мексика', 'BR': 'Бразилия',
    'AR': 'Аргентина', 'CL': 'Чили', 'CO': 'Колумбия', 'PE': 'Перу',
    'VE': 'Венесуэла', 'CU': 'Куба', 'DO': 'Доминикана', 'UY': 'Уругвай',
    'SE': 'Швеция', 'NO': 'Норвегия', 'DK': 'Дания', 'FI': 'Финляндия',
    'IS': 'Исландия', 'IE': 'Ирландия', 'CH': 'Швейцария', 'AT': 'Австрия',
    'BE': 'Бельгия', 'LU': 'Люксембург', 'MT': 'Мальта', 'CY': 'Кипр', 'UK': 'Великобритания',
    'TJ': 'Таджикистан', 'TM': 'Туркменистан', 'MN': 'Монголия', 'NP': 'Непал',
    'LK': 'Шри-Ланка', 'MM': 'Мьянма', 'KH': 'Камбоджа', 'LA': 'Лаос',
}

RU_CAT = {
    'general': 'Общие', 'news': 'Новости', 'movies': 'Кино', 'sports': 'Спорт',
    'music': 'Музыка', 'kids': 'Детям', 'entertainment': 'Развлечения',
    'documentary': 'Документальные', 'education': 'Образование', 'culture': 'Культура',
    'science': 'Наука', 'comedy': 'Юмор', 'series': 'Сериалы', 'animation': 'Мультфильмы',
    'family': 'Семейные', 'lifestyle': 'Стиль жизни', 'cooking': 'Кулинария',
    'travel': 'Путешествия', 'weather': 'Погода', 'business': 'Бизнес',
    'religion': 'Религия', 'religious': 'Религия', 'shop': 'Магазины',
    'auto': 'Авто', 'classic': 'Классика', 'outdoor': 'Природа', 'relax': 'Релакс',
    'public': 'Общественные', 'legislative': 'Государственные', 'interactive': 'Интерактив',
    'undefined': 'Другое',
}


def split_title(line):
    """Title is everything after the last comma outside quoted attribute values."""
    in_quotes = False
    last = -1
    for i, ch in enumerate(line):
        if ch == '"':
            in_quotes = not in_quotes
        elif ch == ',' and not in_quotes:
            last = i
    return line[last + 1:].strip() if last >= 0 else ''


def parse_m3u(path):
    entries = []
    cur = None
    with open(path, encoding='utf-8-sig', errors='replace') as f:
        for line in f:
            line = line.strip()
            if line.startswith('#EXTINF:'):
                attrs = dict(ATTR.findall(line))
                cur = {'attrs': attrs, 'title': split_title(line), 'opts': {}}
            elif line.startswith('#EXTVLCOPT:') and cur is not None:
                k, _, v = line[len('#EXTVLCOPT:'):].partition('=')
                cur['opts'][k.strip()] = v.strip()
            elif line.startswith('#EXTGRP:') and cur is not None:
                cur['attrs'].setdefault('group-title', line[len('#EXTGRP:'):].strip())
            elif line and not line.startswith('#') and cur is not None:
                cur['url'] = line
                entries.append(cur)
                cur = None
    return entries


def channel_key(tvg_id, title):
    base = (tvg_id or '').split('@')[0].strip()
    if base:
        return base
    t = re.sub(r'\((?:[^)]*)\)|\[[^\]]*\]|\b(?:HD|SD|UHD|FHD|4K)\b', '', title, flags=re.I)
    return re.sub(r'\s+', ' ', t).strip().lower() or title.lower()


def clean_name(title):
    t = re.sub(r'\s*\((?:[^)]*?\d{3,4}[pi][^)]*|[^)]*\b(?:HD|SD|UHD|FHD)\b[^)]*)\)\s*$', '', title, flags=re.I)
    t = re.sub(r'\s*\[(?:Not 24/7|[^\]]*)\]', '', t, flags=re.I)
    t = re.sub(r'\s*(?:HD|SD|UHD|FHD|4K)\s*$', '', t, flags=re.I)
    return t.strip() or title.strip()


def quality_of(title):
    m = re.search(r'(\d{3,4}p)', title)
    if m:
        return m.group(1)
    if re.search(r'\b4K\b', title, re.I):
        return '4K'
    return ''


# Ordered by recognisability, roughly the way a Russian operator lists channels.
RU_RANK = [
    ['channel one'],
    ['russia 1', 'russia-1'],
    ['ntv (russia)'],
    ['tnt (russia)'],
    ['sts'],
    ['ren tv'],
    ['channel 5'],
    ['russia 24'],
    ['match!'],
    ['russia-k'],
    ['carousel'],
    ['otr'],
    ['zvezda'],
    ['ntv mir', 'ntv-hit', 'ntv vitrina'],
    ['tv-3'],
    ['friday!'],
    ['domashniy'],
    ['2x2'],
    ['muz', 'ru.tv', 'musicbox', 'europa plus', 'shanson', '1hd music'],
    ['moskva 24'],
    ['360'],
    ['spas', 'tsargrad'],
    ['tnt4'],
    ['che!'],
    ['subbota!'],
    ['pobeda'],
    ['peretz'],
    ['mir 24'],
    ['mir'],
    ['kinopokaz', 'kinopremyera', 'dom kino', 'star cinema'],
    ['viasat', 'viju', 'tv1000', 'amedia'],
    ['discovery', 'animal planet', 'investigation discovery', 'da vinci',
     'nickelodeon', 'nick jr', 'nicktoons', 'tlc', 'fox'],
    ['euronews', 'rt documentary', 'current time', 'izvestia', 'vesti fm', 'radio rossii'],
    ['belarus', 'sochi', 'kuban', 'astrahan', 'groznyj', 'dagestan', 'enisey',
     'gubernia', 'ingushetia', 'osetiya', 'tavriya', 'lugansk', 'novorossiya',
     'belgorod', 'krasnodar', 'len'],
]

RU_RANK_EXACT = {'u': 18, 'o!': 26}


def normalise(text):
    return re.sub(r'[^a-z0-9]+', ' ', text.lower()).strip()


def ru_rank(name):
    lowered = normalise(name)
    exact = RU_RANK_EXACT.get(name.lower())
    if exact is not None:
        return exact
    for index, needles in enumerate(RU_RANK):
        # whole words only: "sts" must not match "bestseller", "otr" must not match "smotrim"
        pattern = r'\b(?:' + '|'.join(re.escape(normalise(n)) for n in needles) + r')\b'
        if re.search(pattern, lowered):
            return index
    return 900


def main():
    feeds = {}
    with open(os.path.join(DATA, 'db_feeds.csv'), encoding='utf-8') as f:
        for row in csv.DictReader(f):
            langs = [code.strip() for code in (row['languages'] or '').split(';') if code.strip()]
            if langs:
                feeds[(row['channel'], row['id'])] = langs

    def load_alive(name):
        path = os.path.join(DATA, name)
        if not os.path.exists(path):
            return {}
        with open(path, encoding='utf-8') as f:
            return json.load(f)

    # strict pass first; a stream that only answered once stays as a fallback so
    # popular channels do not disappear because of one hiccup
    alive = load_alive('alive_stable.json') or load_alive('alive_deep.json') or load_alive('alive.json')
    fallback = load_alive('alive_deep.json')
    print('liveness data for', len(alive), 'urls (fallback', len(fallback), ')')

    cat_entries = parse_m3u(os.path.join(DATA, 'index.category.m3u'))
    lang_entries = parse_m3u(os.path.join(DATA, 'index.language.m3u'))
    print('category entries', len(cat_entries), 'feeds', len(feeds))

    name2code = {}
    with open(os.path.join(DATA, 'languages.json'), encoding='utf-8') as f:
        for lang in json.load(f):
            name2code.setdefault(lang['name'].lower(), lang['code'])

    # fallback for feeds missing from feeds.csv
    playlist_langs = {}
    lang_names = {}
    for e in lang_entries:
        name = e['attrs'].get('group-title', '').strip()
        if not name:
            continue
        code = name2code.get(name.lower(), name.lower().replace(' ', ''))
        lang_names[code] = name
        playlist_langs.setdefault(e['attrs'].get('tvg-id', ''), set()).add(code)
        playlist_langs.setdefault('u:' + e['url'], set()).add(code)

    country_of = {}
    with open(os.path.join(DATA, 'channels.json'), encoding='utf-8') as f:
        for channel in json.load(f):
            if channel.get('country'):
                country_of[channel['id']] = channel['country']

    country_names = {}
    with open(os.path.join(DATA, 'countries.json'), encoding='utf-8') as f:
        for country in json.load(f):
            country_names[country['code']] = country['name']

    channels = OrderedDict()
    known = 0
    for e in cat_entries:
        attrs = e['attrs']
        title = e['title']
        cat = attrs.get('group-title', '').strip().lower()
        if cat in ('xxx', 'adult'):
            continue
        tvg = attrs.get('tvg-id', '')
        base, _, feed_id = tvg.partition('@')
        key = channel_key(tvg, title)
        ch = channels.get(key)
        if ch is None:
            ch = channels[key] = {
                'id': key,
                'name': clean_name(title),
                'logo': attrs.get('tvg-logo', ''),
                'categories': [],
                'languages': set(),
                'streams': [],
            }
        if cat and cat not in ch['categories']:
            ch['categories'].append(cat)

        feed_langs = feeds.get((base, feed_id))
        if feed_langs:
            known += 1
            primary = feed_langs[0]
        else:
            fallback = playlist_langs.get(tvg) or playlist_langs.get('u:' + e['url']) or set()
            primary = sorted(fallback)[0] if fallback else ''
        if primary and primary != 'und':
            ch['languages'].add(primary)

        ua = attrs.get('http-user-agent') or e['opts'].get('http-user-agent') or ''
        ref = attrs.get('http-referrer') or e['opts'].get('http-referrer') or ''
        stream = {'url': e['url'], 'quality': quality_of(title)}
        if primary and primary != 'und':
            stream['language'] = primary
        if ua:
            stream['userAgent'] = ua
        if ref:
            stream['referrer'] = ref
        if stream not in ch['streams'] and len(ch['streams']) < 6:
            ch['streams'].append(stream)

    checked = sum(1 for ch in channels.values() for s in ch['streams'] if s['url'] in alive)
    if alive:
        # keep only streams that answered during the liveness probe
        for ch in channels.values():
            # only streams that answered both probes; a top-20 Russian channel is
            # kept even if its single stream hiccuped once, otherwise it vanishes
            stable = [s for s in ch['streams'] if alive.get(s['url'])]
            if stable:
                ch['streams'] = stable
            elif 'rus' in ch['languages'] and ru_rank(ch['name']) < 20:
                ch['streams'] = [s for s in ch['streams'] if fallback.get(s['url'])]
            else:
                ch['streams'] = []
        print('streams checked:', checked, 'removed:', sum(1 for _ in ()))

    print('entries matched to a feed:', known, 'of', len(cat_entries))

    out = []
    for ch in channels.values():
        if not ch['streams']:
            continue
        ch['categories'] = ch['categories'][:3] or ['general']
        ch['languages'] = sorted(ch['languages']) or ['und']
        ch['name'] = ch['name'][:80]
        if not ch['logo'].startswith('http'):
            ch['logo'] = ''
        ch['country'] = country_of.get(ch['id'], '')
        ch['rank'] = ru_rank(ch['name']) if 'rus' in ch['languages'] else 900
        out.append(ch)

    names_ru = translate_names([ch['name'] for ch in out])
    for ch in out:
        ch['nameRu'] = names_ru.get(ch['name'], '')

    lang_counter = Counter(c for ch in out for c in ch['languages'])
    cat_counter = Counter(c for ch in out for c in ch['categories'])
    country_counter = Counter(ch['country'] for ch in out if ch['country'])

    languages = [{'id': code, 'name': RU_LANG.get(lang_names.get(code, code), lang_names.get(code, code)), 'count': n}
                 for code, n in lang_counter.most_common() if code != 'und']
    cats = [{'id': c, 'name': RU_CAT.get(c, c.capitalize()), 'count': n}
            for c, n in cat_counter.most_common()]
    countries = [{'id': c, 'name': RU_COUNTRY.get(c, country_names.get(c, c)), 'count': n}
                 for c, n in country_counter.most_common()]

    catalog = {'version': 2, 'languages': languages, 'categories': cats,
               'countries': countries, 'channels': out}
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(catalog, f, ensure_ascii=False, separators=(',', ':'))
    print('channels', len(out), 'languages', len(languages), 'bytes', os.path.getsize(OUT))
    print('top languages', [(l['name'], l['count']) for l in languages[:6]])


if __name__ == '__main__':
    main()
