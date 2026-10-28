import io

path = 'app/build.gradle.kts'
text = io.open(path, encoding='utf-8').read()
text = text.replace('versionCode = 8', 'versionCode = 9')
text = text.replace('versionName = "1.0.7"', 'versionName = "1.0.8"')
io.open(path, 'w', encoding='utf-8').write(text)

for line in text.splitlines():
    if 'version' in line:
        print(line.strip())
