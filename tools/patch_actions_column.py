import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()

text = text.replace('@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)\n\n', '', 1)
text = text.replace('@OptIn(UnstableApi::class, ExperimentalLayoutApi::class)', '@OptIn(UnstableApi::class)', 1)
text = text.replace('import androidx.compose.foundation.layout.ExperimentalLayoutApi\n', '', 1)
text = text.replace('import androidx.compose.foundation.layout.FlowRow\n', '', 1)

old = """            FlowRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {"""
new = """            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {"""
if old not in text:
    print('MISS flowrow block')
else:
    text = text.replace(old, new, 1)
    print('ok column')

io.open(path, 'w', encoding='utf-8').write(text)
