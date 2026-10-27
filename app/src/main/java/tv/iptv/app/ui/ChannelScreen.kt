package tv.iptv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import tv.iptv.app.data.Catalog
import tv.iptv.app.data.Channel

private const val MAX_CHIPS = 12

private val Channel.sortName: String
    get() = nameRu.ifBlank { name }.lowercase()

@Composable
fun ChannelScreen(
    catalog: Catalog,
    languageId: String,
    playingId: String?,
    favoriteIds: Set<String>,
    firstItemFocus: FocusRequester,
    onToggleFavorite: (String) -> Unit,
    onPlay: (Channel) -> Unit,
) {
    var category by remember(languageId) { mutableStateOf<String?>(null) }
    var countryId by remember(languageId) { mutableStateOf<String?>(null) }
    var lastPlayedId by remember(languageId) { mutableStateOf<String?>(null) }
    val focusedId = remember { mutableStateOf<String?>(null) }

    // channels declaring exactly one language are the safest bet for that language,
    // so they come first; multi-language entries from the source are noisier
    val channels = remember(catalog, languageId, favoriteIds) {
        when (languageId) {
            FAVORITES -> catalog.channels.filter { it.id in favoriteIds }
            ALL_LANGUAGES -> catalog.channels.sortedWith(
                compareBy(
                    { languagePriority(it.languages.firstOrNull() ?: "", it.country) },
                    { it.sortName },
                ),
            )
            // Russian channels follow a recognisability ranking, like an operator's lineup
            "rus" -> catalog.channels
                .filter { languageId in it.languages }
                .sortedWith(compareBy({ it.rank }, { it.sortName }))
            else -> catalog.channels
                .filter { languageId in it.languages }
                .sortedWith(compareBy({ it.languages.size }, { it.sortName }))
        }
    }

    val categoryNames = remember(catalog) { catalog.categories.associate { it.id to it.name } }
    val countryNames = remember(catalog) { catalog.countries.associate { it.id to it.name } }

    val availableCategories = remember(channels, categoryNames) {
        channels.flatMap { it.categories }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key to (categoryNames[it.key] ?: it.key) }
    }
    val availableCountries = remember(channels, countryNames) {
        channels.mapNotNull { it.country.takeIf { code -> code.isNotEmpty() } }
            .groupingBy { it }.eachCount()
            .entries.sortedWith(compareBy({ if (it.key == "ru") 0 else 1 }, { -it.value }))
            .map { it.key to (countryNames[it.key] ?: it.key) }
    }
    val visible = remember(channels, category, countryId) {
        channels.filter { channel ->
            (category == null || category in channel.categories) &&
                (countryId == null || channel.country == countryId)
        }
    }

    val restoreFocus = remember { FocusRequester() }

    // OK is handled at activity level: a short press starts the channel,
    // holding it adds/removes the channel from favourites.
    OkKeyHandler(
        enabled = playingId == null,
        focusedId = { focusedId.value },
        onShortPress = {
            focusedId.value
                ?.let { id -> visible.firstOrNull { it.id == id } }
                ?.let { channel ->
                    lastPlayedId = channel.id
                    onPlay(channel)
                }
        },
        onLongPress = { focusedId.value?.let(onToggleFavorite) },
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = languageTitle(catalog, languageId),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (languageId == FAVORITES) {
                    "${formatCount(visible.size)} каналов · удерживайте ОК, чтобы убрать"
                } else {
                    "${formatCount(visible.size)} каналов · удерживайте ОК — в избранное"
                },
                color = Palette.TextDim,
                fontSize = 15.sp,
            )
        }
        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            item(key = "all") {
                FocusableChip("Все жанры", selected = category == null) { category = null }
            }
            items(availableCategories.take(MAX_CHIPS), key = { it.first }) { (id, name) ->
                FocusableChip(name, selected = category == id) {
                    category = if (category == id) null else id
                }
            }
        }

        if (availableCountries.size > 1) {
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                item(key = "any") {
                    FocusableChip("Все страны", selected = countryId == null, small = true) { countryId = null }
                }
                items(availableCountries.take(MAX_CHIPS), key = { it.first }) { (id, name) ->
                    FocusableChip(name, selected = countryId == id, small = true) {
                        countryId = if (countryId == id) null else id
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (visible.isEmpty()) {
            Text(
                text = if (languageId == FAVORITES) "Пусто. Удерживайте ОК на канале, чтобы добавить его сюда."
                else "Здесь нет каналов",
                color = Palette.TextDim,
                modifier = Modifier.padding(top = 40.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(visible, key = { _, channel -> channel.id }) { index, channel ->
                    val modifier = when {
                        channel.id == lastPlayedId -> Modifier.focusRequester(restoreFocus)
                        index == 0 -> Modifier.focusRequester(firstItemFocus)
                        else -> Modifier
                    }.onFocusChanged { state ->
                        if (state.isFocused) {
                            focusedId.value = channel.id
                        } else if (focusedId.value == channel.id) {
                            focusedId.value = null
                        }
                    }
                    ChannelCard(channel, channel.id in favoriteIds, modifier) {
                        lastPlayedId = channel.id
                        onPlay(channel)
                    }
                }
            }
            LaunchedEffect(visible) { runCatching { firstItemFocus.requestFocus() } }
            LaunchedEffect(playingId) {
                if (playingId == null && lastPlayedId != null) {
                    runCatching { restoreFocus.requestFocus() }
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(84.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.LogoBox),
                contentAlignment = Alignment.Center,
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = channel.name.take(2).uppercase(),
                        color = Palette.TextDim,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = channel.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        channel.country.takeIf { it.isNotEmpty() },
                        channel.categories.firstOrNull(),
                    ).joinToString(" · "),
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isFavorite) {
                Text("★", color = Palette.Accent, fontSize = 16.sp)
            }
        }
    }
}
