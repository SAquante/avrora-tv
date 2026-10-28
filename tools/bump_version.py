import io

path = 'app/build.gradle.kts'
text = io.open(path, encoding='utf-8').read()
text = text.replace('versionCode = 15', 'versionCode = 16')
text = text.replace('versionName = "1.1.4"', 'versionName = "1.1.5"')
io.open(path, 'w', encoding='utf-8').write(text)
print([line.strip() for line in text.splitlines() if 'version' in line])
