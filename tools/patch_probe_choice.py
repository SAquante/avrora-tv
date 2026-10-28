import io

path = 'tools/build_catalog.py'
text = io.open(path, encoding='utf-8').read()
old = "for candidate in ('alive_deep.json', 'alive.json'):"
new = "for candidate in ('alive_stable.json', 'alive_deep.json', 'alive.json'):"
if old in text:
    text = text.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8').write(text)
    print('candidates updated')
else:
    print('MISS candidates' if new not in text else 'already updated')
