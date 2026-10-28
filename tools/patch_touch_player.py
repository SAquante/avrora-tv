import io

path = 'app/src/main/java/tv/iptv/app/ui/PlayerScreen.kt'
text = io.open(path, encoding='utf-8').read()

# 1. root box: a tap toggles the info/action bar on phones (no remote there)
old_root = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {"""
new_root = """    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    infoVisible = true
                    actionsOpen = !actionsOpen
                    actionIndex = 0
                }
            },
    ) {"""
if old_root not in text:
    print('root box pattern missing')
else:
    text = text.replace(old_root, new_root, 1)

# 2. action pills become tappable
old_pills = """                ActionPill(
                    text = if (isFavorite) "★ В избранном" else "☆ В избранное",
                    active = actionIndex == 0,
                )
                ActionPill(
                    text = "Качество: ${stream?.quality?.ifBlank { "авто" } ?: "авто"}",
                    active = actionIndex == 1,
                )"""
new_pills = """                ActionPill(
                    text = if (isFavorite) "★ В избранном" else "☆ В избранное",
                    active = actionIndex == 0,
                    onClick = {
                        actionsOpen = false
                        onToggleFavorite()
                        notice = if (isFavorite) "Убрано из избранного" else "Добавлено в избранное ★"
                    },
                )
                ActionPill(
                    text = "Качество: ${stream?.quality?.ifBlank { "авто" } ?: "авто"}",
                    active = actionIndex == 1,
                    onClick = {
                        if (streams.size > 1) {
                            attempt = (attempt + 1) % streams.size
                            notice = "Качество: ${streams[attempt].quality.ifBlank { "авто" }}"
                        }
                    },
                )"""
if old_pills not in text:
    print('pills pattern missing')
else:
    text = text.replace(old_pills, new_pills, 1)

old_pill_fn = """@Composable
private fun ActionPill(text: String, active: Boolean) {
    Text(
        text = text,
        color = if (active) Palette.Background else Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))"""
new_pill_fn = """@Composable
private fun ActionPill(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (active) Palette.Background else Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))"""
if old_pill_fn not in text:
    print('pill fn pattern missing')
else:
    text = text.replace(old_pill_fn, new_pill_fn, 1)

for imp in (
    'import androidx.compose.foundation.clickable',
    'import androidx.compose.foundation.gestures.detectTapGestures',
    'import androidx.compose.ui.input.pointer.pointerInput',
):
    if imp not in text:
        text = text.replace('import androidx.compose.foundation.background', imp + '\nimport androidx.compose.foundation.background', 1)

io.open(path, 'w', encoding='utf-8').write(text)
print('done')
