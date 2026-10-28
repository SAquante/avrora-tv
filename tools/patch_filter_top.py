import io

path = 'tools/build_catalog.py'
text = io.open(path, encoding='utf-8').read()

old = """        for ch in channels.values():
            ch['streams'] = [s for s in ch['streams']
                             if alive.get(s['url']) or fallback.get(s['url'])]"""

new = """        for ch in channels.values():
            # only streams that answered both probes; a top-20 Russian channel is
            # kept even if its single stream hiccuped once, otherwise it vanishes
            stable = [s for s in ch['streams'] if alive.get(s['url'])]
            if stable:
                ch['streams'] = stable
            elif ch.get('rank', 999) < 20:
                ch['streams'] = [s for s in ch['streams'] if fallback.get(s['url'])]
            else:
                ch['streams'] = []"""

if old in text:
    text = text.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8').write(text)
    print('ok filter')
else:
    print('MISS filter')
