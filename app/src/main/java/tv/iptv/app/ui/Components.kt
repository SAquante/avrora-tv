package tv.iptv.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) Palette.Accent else Palette.Border,
        label = "border",
    )
    val scale by animateFloatAsState(if (focused) 1.04f else 1f, label = "scale")
    val background by animateColorAsState(
        if (focused) Palette.CardFocused else Palette.Card,
        label = "background",
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(background)
            .border(if (focused) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        content = content,
    )
}

@Composable
fun FocusableChip(
    text: String,
    selected: Boolean,
    small: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val background by animateColorAsState(
        when {
            selected -> Palette.Accent
            focused -> Palette.CardFocused
            else -> Palette.Card
        },
        label = "chip",
    )
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(background)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Palette.Accent else Palette.Border,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (small) 16.dp else 22.dp,
                vertical = if (small) 7.dp else 10.dp,
            ),
    ) {
        androidx.compose.material3.Text(
            text = text,
            color = if (selected) Palette.Background else Palette.TextDim,
            fontSize = if (small) 14.sp else 15.sp,
        )
    }
}
