import io

path = 'app/src/main/java/tv/iptv/app/ui/App.kt'
text = io.open(path, encoding='utf-8').read()

old = (
    "                    val phone = !isTelevision || maxWidth < 780.dp\n"
    "                    val columns = if (phone) 1 else 2\n"
)

new = (
    "                    val portrait = maxWidth < maxHeight\n"
    "                    // phone portrait: bottom bar + one column; phone landscape and TV: side rail\n"
    "                    val useRail = isTelevision || (!portrait && maxWidth >= 600.dp)\n"
    "                    val columns = if (useRail) 2 else 1\n"
)

if old not in text:
    print('pattern missing')
else:
    text = text.replace(old, new, 1)
    text = text.replace("                    if (phone) {\n", "                    if (!useRail) {\n", 1)
    io.open(path, 'w', encoding='utf-8').write(text)
    print('patched')
