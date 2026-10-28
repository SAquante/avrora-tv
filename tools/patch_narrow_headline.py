import io

path = 'app/src/main/java/tv/iptv/app/ui/LanguageScreen.kt'
text = io.open(path, encoding='utf-8').read()

old = """        Text(
            text = "Что посмотрим?",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )"""

new = """        Text(
            text = "Что посмотрим?",
            color = Color.White,
            fontSize = if (narrow) 30.sp else 38.sp,
            fontWeight = FontWeight.Bold,
        )"""

if old in text:
    text = text.replace(old, new, 1)
    print('headline patched')
else:
    print('MISS headline')

anchor = 'fun LanguageScreen('
if 'narrow' not in text.split(anchor)[1][:400]:
    text = text.replace('    columns: Int = 2,', '    columns: Int = 2,\n    narrow: Boolean = false,', 1)
    print('param added')

io.open(path, 'w', encoding='utf-8').write(text)
