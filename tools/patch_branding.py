import io
import os

RES = 'app/src/main/res'

# 1. colour for the adaptive icon background
colors = os.path.join(RES, 'values', 'colors.xml')
if not os.path.exists(colors):
    io.open(colors, 'w', encoding='utf-8').write(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<resources>\n'
        '    <color name="ic_launcher_background">#1A0E08</color>\n'
        '</resources>\n'
    )
    print('colors.xml written')

# 2. manifest: adaptive icon
manifest_path = 'app/src/main/AndroidManifest.xml'
manifest = io.open(manifest_path, encoding='utf-8').read()
if 'android:icon="@mipmap/ic_launcher"' not in manifest:
    manifest = manifest.replace('android:icon="@drawable/ic_launcher"',
                                'android:icon="@mipmap/ic_launcher"', 1)
    manifest = manifest.replace('android:banner="@drawable/banner"',
                                'android:banner="@drawable/banner"\n        android:roundIcon="@mipmap/ic_launcher"', 1)
    io.open(manifest_path, 'w', encoding='utf-8').write(manifest)
    print('manifest updated')

# 3. font family in the palette
palette_path = 'app/src/main/java/tv/iptv/app/ui/Palette.kt'
palette = io.open(palette_path, encoding='utf-8').read()
if 'avroraFont' not in palette:
    anchor = 'object Palette {'
    fonts = '''val avroraFont: FontFamily = FontFamily(
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
)

object Palette {'''
    palette = palette.replace(anchor, fonts, 1)
    imports = (
        'import androidx.compose.ui.text.font.Font\n'
        'import androidx.compose.ui.text.font.FontFamily\n'
        'import androidx.compose.ui.text.font.FontWeight\n'
        'import tv.iptv.app.R\n'
    )
    palette = palette.replace('package tv.iptv.app.ui\n', 'package tv.iptv.app.ui\n\n' + imports, 1)
    io.open(palette_path, 'w', encoding='utf-8').write(palette)
    print('palette updated')

# 4. every Text inherits the new typeface
app_path = 'app/src/main/java/tv/iptv/app/ui/App.kt'
app = io.open(app_path, encoding='utf-8').read()
old = '        Surface(Modifier.fillMaxSize(), color = Palette.Background) {'
new = '''        Surface(Modifier.fillMaxSize(), color = Palette.Background) {
          androidx.compose.runtime.CompositionLocalProvider(
              androidx.compose.material3.LocalTextStyle provides
                  androidx.compose.ui.text.TextStyle(
                      fontFamily = avroraFont,
                      color = Color.White,
                  ),
          ) {'''
if old in app and 'LocalTextStyle provides' not in app:
    app = app.replace(old, new, 1)
    # close the extra lambda: find the Surface block end by matching the last closing brace of the file
    marker = '\n    }\n}\n'
    if app.rstrip().endswith('}'):
        app = app.rstrip()
        # append closing brace for the provider right before the final ones
        app = app[:app.rfind('\n}')] + '\n          }\n    }\n    }\n}\n'
    io.open(app_path, 'w', encoding='utf-8').write(app)
    print('app wrapped (check braces)')
else:
    print('app already wrapped or anchor missing')
