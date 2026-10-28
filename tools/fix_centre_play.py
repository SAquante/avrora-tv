import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()

block = """        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(92.dp)
                .clip(CircleShape)
                .background(if (infoVisible) Color(0xCC1B100A) else Color(0x801B100A))
                .clickable { playerRef.value?.let { it.playWhenReady = !it.playWhenReady } },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (paused) "\u25b6" else "\u275a\u275a",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
        }

"""

# 1. remove the misplaced copy inside LaunchedEffect
if block in text:
    text = text.replace(block, '', 1)
    print('removed misplaced block')
else:
    print('MISS block removal')

# 2. put it inside the player Box, right before the action bar
anchor = "        if (actionsOpen) {"
if anchor in text:
    text = text.replace(anchor, block + anchor, 1)
    print('inserted centre button before actions')
else:
    print('MISS anchor')

io.open(path, 'w', encoding='utf-8').write(text)
