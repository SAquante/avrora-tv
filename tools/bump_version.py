import io

path = 'app/build.gradle.kts'
text = io.open(path, encoding='utf-8').read()
text = text.replace('versionCode = 7', 'versionCode = 8')
text = text.replace('versionName = "1.0.6"', 'versionName = "1.0.7"')
io.open(path, 'w', encoding='utf-8').write(text)

print('version now:')
for line in text.splitlines():
    if 'version' in line:
        print(' ', line.strip())
