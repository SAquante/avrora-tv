package tv.iptv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.iptv.app.data.Catalog

@Composable
fun LanguageScreen(
    catalog: Catalog,
    firstItemFocus: FocusRequester,
    onPick: (String) -> Unit,
) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }

    // Russian and English first, then by number of channels
    val orderedLanguages = remember(catalog) {
        catalog.languages.sortedWith(
            compareBy(
                { if (it.id == "rus") 0 else if (it.id == "eng") 1 else 2 },
                { -it.count },
            ),
        )
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 26.dp)) {
        Text(
            text = greeting(hour),
            color = Palette.Accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Что посмотрим?",
            color = Color.White,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${formatCount(catalog.channels.size)} каналов · ${catalog.languages.size} языков · выберите язык",
            color = Palette.TextDim,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(22.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(orderedLanguages, key = { it.id }) { language ->
                val modifier = if (language.id == orderedLanguages.first().id) {
                    Modifier.focusRequester(firstItemFocus)
                } else {
                    Modifier
                }
                LanguageTile(
                    name = language.name,
                    count = language.count,
                    modifier = modifier,
                    onClick = { onPick(language.id) },
                )
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { firstItemFocus.requestFocus() } }
}

@Composable
private fun LanguageTile(name: String, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FocusableCard(onClick = onClick, modifier = modifier.fillMaxWidth().height(64.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("${formatCount(count)}", color = Palette.TextDim, fontSize = 13.sp)
        }
    }
}
