import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()

old = '                    text = "Пауза · ОК — продолжить",'
new = '                    text = if (isTelevision) "Пауза · ОК — продолжить" else "Пауза · коснитесь, чтобы продолжить",'

if old in text:
    text = text.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8').write(text)
    print('patched pause hint')
else:
    print('MISS pause hint')

gradle = 'app/build.gradle.kts'
config = io.open(gradle, encoding='utf-8').read()
config = config.replace('versionCode = 13', 'versionCode = 14')
config = config.replace('versionName = "1.1.2"', 'versionName = "1.1.3"')
io.open(gradle, 'w', encoding='utf-8').write(config)
print([line.strip() for line in config.splitlines() if 'version' in line])
