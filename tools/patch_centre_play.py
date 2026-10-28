import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()


def sub(old, new, name, count=1):
    global text
    if old not in text:
        print('MISS', name)
        return
    text = text.replace(old, new, count)
    print('ok', name)


# 1. drop the third (pause) pill: playback now has a centre button
pause_pill = """                ActionPill(
                    text = if (paused) "▶ Играть" else "⏸ Пауза",
                    active = actionIndex == 2,
                    onClick = { playerRef.value?.let { it.playWhenReady = !it.playWhenReady } },
                )
"""
sub(pause_pill, '', 'remove pause pill')

# 2. two actions again
sub('                            actionIndex = (actionIndex + 1).coerceAtMost(2)',
    '                            actionIndex = (actionIndex + 1).coerceAtMost(1)',
    'action range')
sub('                    if (down && actionsOpen) actionIndex = (actionIndex + 1) % 3',
    '                    if (down && actionsOpen) actionIndex = 1 - actionIndex',
    'arrow cycling')
sub("""                            } else if (actionIndex == 2) {
                                playerRef.value?.let { it.playWhenReady = !it.playWhenReady }
                            } else {""",
    """                            } else {""",
    'ok action')

# 3. centre play/pause button
centre = """        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(92.dp)
                .clip(CircleShape)
                .background(if (infoVisible) Color(0xCC1B100A) else Color(0x801B100A))
                .clickable { playerRef.value?.let { it.playWhenReady = !it.playWhenReady } },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (paused) "▶" else "❚❚",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (infoVisible) {"""
sub("        if (infoVisible) {", centre, 'centre button', 1)

for imp in (
    'import androidx.compose.foundation.shape.CircleShape',
    'import androidx.compose.foundation.layout.size',
):
    if imp not in text:
        text = text.replace('import androidx.compose.foundation.shape.RoundedCornerShape',
                            imp + '\nimport androidx.compose.foundation.shape.RoundedCornerShape', 1)

io.open(path, 'w', encoding='utf-8').write(text)
print('done')
