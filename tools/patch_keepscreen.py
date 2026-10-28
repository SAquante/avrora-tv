import io

path = 'app/src/main/java/tv/iptv/app/MainActivity.kt'
text = io.open(path, encoding='utf-8').read()

old = """        if (!isTelevision()) goFullscreen()
        setContent { IptvApp() }"""
new = """        // keep the box awake: the launcher screensaver otherwise blacks the app out
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (!isTelevision()) goFullscreen()
        setContent { IptvApp() }"""

if old not in text:
    print('pattern missing')
else:
    text = text.replace(old, new, 1)
    text = text.replace(
        "        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)\n        if (Build.VERSION.SDK_INT",
        "        if (Build.VERSION.SDK_INT",
        1,
    )
    io.open(path, 'w', encoding='utf-8').write(text)
    print('patched')
