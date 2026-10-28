import io

path = 'tools/build_catalog.py'
text = io.open(path, encoding='utf-8').read()
old = "            elif ch.get('rank', 999) < 20:"
new = "            elif 'rus' in ch['languages'] and ru_rank(ch['name']) < 20:"
if old in text:
    text = text.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8').write(text)
    print('ok rank condition')
else:
    print('MISS rank condition')
