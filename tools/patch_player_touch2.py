import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()


def sub(old, new, name):
    global text
    if old not in text:
        print('MISS', name)
        return
    text = text.replace(old, new, 1)
    print('ok', name)


# 1. three actions instead of two
sub(
    "                            actionIndex = (actionIndex + 1).coerceAtMost(1)",
    "                            actionIndex = (actionIndex + 1).coerceAtMost(2)",
    'action index range',
)

sub(
    "                    if (down && actionsOpen) actionIndex = 1 - actionIndex",
    "                    if (down && actionsOpen) actionIndex = (actionIndex + 1) % 3",
    'arrow cycling',
)

# 2. OK on the third action toggles playback
sub(
    """                            if (actionIndex == 0) {
                                actionsOpen = false
                                onToggleFavorite()
                                notice = if (isFavorite) "Убрано из избранного" else "Добавлено в избранное ★"
                            } else {""",
    """                            if (actionIndex == 0) {
                                actionsOpen = false
                                onToggleFavorite()
                                notice = if (isFavorite) "Убрано из избранного" else "Добавлено в избранное ★"
                            } else if (actionIndex == 2) {
                                playerRef.value?.let { it.playWhenReady = !it.playWhenReady }
                            } else {""",
    'ok on pause action',
)

# 3. the third pill
sub(
    """                ActionPill(
                    text = "Качество: ${stream?.quality?.ifBlank { "авто" } ?: "авто"}",
                    active = actionIndex == 1,
                    onClick = {
                        if (streams.size > 1) {
                            attempt = (attempt + 1) % streams.size
                            notice = "Качество: ${streams[attempt].quality.ifBlank { "авто" }}"
                        }
                    },
                )""",
    """                ActionPill(
                    text = "Качество: ${stream?.quality?.ifBlank { "авто" } ?: "авто"}",
                    active = actionIndex == 1,
                    onClick = {
                        if (streams.size > 1) {
                            attempt = (attempt + 1) % streams.size
                            notice = "Качество: ${streams[attempt].quality.ifBlank { "авто" }}"
                        }
                    },
                )
                ActionPill(
                    text = if (paused) "▶ Играть" else "⏸ Пауза",
                    active = actionIndex == 2,
                    onClick = { playerRef.value?.let { it.playWhenReady = !it.playWhenReady } },
                )""",
    'pause pill',
)

# 4. hint text adapts: touch on phones, remote on TV
sub(
    """                    Text(
                        text = listOfNotNull(
                            stream?.quality?.takeIf { it.isNotEmpty() },
                            "ОК — пауза · ▼ — действия",
                        ).joinToString(" · "),
                        color = Palette.TextDim,
                        fontSize = 13.sp,
                    )""",
    """                    Text(
                        text = listOfNotNull(
                            stream?.quality?.takeIf { it.isNotEmpty() },
                            if (isTelevision) "ОК — пауза · ▼ — действия" else "Коснитесь — действия и пауза",
                        ).joinToString(" · "),
                        color = Palette.TextDim,
                        fontSize = 13.sp,
                    )""",
    'hint text',
)

# 5. a close button in the header, invisible but harmless on TV
sub(
    """            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp)) {
                Column(Modifier.weight(1f)) {""",
    """            Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp)) {
                Column(Modifier.weight(1f)) {""",
    'header row (unchanged)',
)

io.open(path, 'w', encoding='utf-8').write(text)
