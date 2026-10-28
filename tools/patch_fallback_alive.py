import io

path = 'tools/build_catalog.py'
text = io.open(path, encoding='utf-8').read()

old_load = """    alive = {}
    for candidate in ('alive_stable.json', 'alive_deep.json', 'alive.json'):
        candidate_path = os.path.join(DATA, candidate)
        if os.path.exists(candidate_path):
            with open(candidate_path, encoding='utf-8') as f:
                alive = json.load(f)
            print('liveness data from', candidate, 'for', len(alive), 'urls')
            break
"""

new_load = """    def load_alive(name):
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
"""

if old_load in text:
    text = text.replace(old_load, new_load, 1)
    print('ok loader')
else:
    print('MISS loader')

old_filter = """        for ch in channels.values():
            ch['streams'] = [s for s in ch['streams'] if alive.get(s['url'], False)]"""
new_filter = """        for ch in channels.values():
            ch['streams'] = [s for s in ch['streams']
                             if alive.get(s['url']) or fallback.get(s['url'])]"""
if old_filter in text:
    text = text.replace(old_filter, new_filter, 1)
    print('ok filter')
else:
    print('MISS filter')

io.open(path, 'w', encoding='utf-8').write(text)
