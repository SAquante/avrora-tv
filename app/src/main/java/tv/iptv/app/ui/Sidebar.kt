package tv.iptv.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val SECTION_LANGUAGES = "languages"
const val SECTION_ALL = "all"
const val SECTION_RADIO = "radio"
const val SECTION_FAVORITES = "favorites"
const val SECTION_CAMS = "cams"

@Composable
fun Sidebar(selected: String, onSelect: (String) -> Unit, onEnterContent: () -> Unit) {
    Column(
        modifier = Modifier
            .width(248.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE6331B10), Color(0xF21B100A)),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Avrora",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 14.dp),
        )
        Text(
            text = "TV",
            color = Palette.Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 15.dp),
        )
        Text(
            text = "рассвет · утренняя заря",
            color = Color(0x8CFFFFFF),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 15.dp, bottom = 16.dp),
        )

        SidebarItem("Языки", selected == SECTION_LANGUAGES, onEnterContent) { onSelect(SECTION_LANGUAGES) }
        SidebarItem("Все каналы", selected == SECTION_ALL, onEnterContent) { onSelect(SECTION_ALL) }
        SidebarItem("Радио", selected == SECTION_RADIO, onEnterContent) { onSelect(SECTION_RADIO) }
        if (tv.iptv.app.data.Features.cams) {
            SidebarItem("Веб-камеры", selected == SECTION_CAMS, onEnterContent) { onSelect(SECTION_CAMS) }
        }
        SidebarItem("★ Избранное", selected == SECTION_FAVORITES, onEnterContent) { onSelect(SECTION_FAVORITES) }
    }
}

@Composable
private fun SidebarItem(
    label: String,
    selected: Boolean,
    onEnterContent: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        when {
            selected -> Color(0x59FFC46B)
            focused -> Palette.CardFocused
            else -> Color(0x14FFD9A8)
        },
        label = "sidebar",
    )
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                    onEnterContent()
                    true
                } else {
                    false
                }
            }
            .clip(shape)
            .background(background)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Palette.Accent else Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = if (selected || focused) Color.White else Palette.TextDim,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
