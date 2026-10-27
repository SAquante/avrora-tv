import io

path = 'app/src/main/java/tv/iptv/app/ui/App.kt'
text = io.open(path, encoding='utf-8').read()

old = "                    val phone = maxWidth < 780.dp\n"
new = (
    "                    // phone/tablet always gets the bottom bar, a TV keeps the side rail\n"
    "                    val isTelevision = remember {\n"
    "                        context.packageManager.hasSystemFeature(\n"
    "                            android.content.pm.PackageManager.FEATURE_LEANBACK,\n"
    "                        ) || context.packageManager.hasSystemFeature(\n"
    "                            android.content.pm.PackageManager.FEATURE_TELEVISION,\n"
    "                        )\n"
    "                    }\n"
    "                    val phone = !isTelevision || maxWidth < 780.dp\n"
)

if old not in text:
    print('pattern missing')
else:
    text = text.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8').write(text)
    print('patched')
