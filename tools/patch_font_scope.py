import io

path = 'app/src/main/java/tv/iptv/app/ui/App.kt'
text = io.open(path, encoding='utf-8').read()

open_old = """    ) {
        Surface(Modifier.fillMaxSize(), color = Palette.Background) {"""
open_new = """    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides
                androidx.compose.ui.text.TextStyle(fontFamily = avroraFont, color = Color.White),
        ) {
        Surface(Modifier.fillMaxSize(), color = Palette.Background) {"""

close_old = """            }
        }
    }
}

private fun channelScope"""
close_new = """            }
        }
        }
    }
}

private fun channelScope"""

if open_old in text:
    text = text.replace(open_old, open_new, 1)
    print('opened provider')
else:
    print('MISS open')

if close_old in text:
    text = text.replace(close_old, close_new, 1)
    print('closed provider')
else:
    print('MISS close')

io.open(path, 'w', encoding='utf-8').write(text)
