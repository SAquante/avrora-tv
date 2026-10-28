import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()

old = """            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {"""
new = """            FlowRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {"""
if old not in text:
    print('MISS row->flowrow')
else:
    text = text.replace(old, new, 1)
    print('ok flowrow')

for imp, anchor in (
    ('import androidx.compose.foundation.layout.ExperimentalLayoutApi', 'import androidx.compose.foundation.layout.Arrangement'),
    ('import androidx.compose.foundation.layout.FlowRow', 'import androidx.compose.foundation.layout.Arrangement'),
):
    if imp not in text:
        text = text.replace(anchor, imp + '\n' + anchor, 1)

# opt in at the composable that renders the player
marker = 'fun PlayerScreen('
idx = text.find(marker)
if idx != -1 and '@OptIn(ExperimentalLayoutApi::class)' not in text[max(0, idx - 200):idx]:
    line_start = text.rfind('\n', 0, idx)
    text = text[:line_start + 1] + '@OptIn(ExperimentalLayoutApi::class)\n' + text[line_start + 1:]
    print('ok optin')

io.open(path, 'w', encoding='utf-8').write(text)
