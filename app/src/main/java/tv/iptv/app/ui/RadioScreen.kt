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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.iptv.app.data.RadioCatalog
import tv.iptv.app.data.RadioStation

private const val MAX_RADIO_COUNTRIES = 12

@Composable
fun RadioScreen(
    catalog: RadioCatalog,
    favoriteIds: Set<String>,
    firstItemFocus: FocusRequester,
    columns: Int = 2,
    onToggleFavorite: (String) -> Unit,
    onPlay: (RadioStation) -> Unit,
) {
    var sortByName by remember { mutableStateOf(false) }
    var countryId by remember { mutableStateOf<String?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }
    val focusedId = remember { mutableStateOf<String?>(null) }

    val countryNames = remember(catalog) { catalog.countries.associate { it.id to it.name } }
    val orderedCountries = remember(catalog) {
        catalog.countries.sortedWith(compareBy({ if (it.id == "ru") 0 else 1 }, { -it.count }))
    }

    val visible = remember(catalog, sortByName, countryId, onlyFavorites, favoriteIds, countryNames) {
        val base = catalog.stations.filter { station ->
            (countryId == null || station.country == countryId) &&
                (!onlyFavorites || station.id in favoriteIds)
        }
        // Russian and English stations go first, then everything else
        if (sortByName) {
            base.sortedWith(
                compareBy({ languagePriority(it.language, it.country) }, { it.name.lowercase() }),
            )
        } else {
            base.sortedWith(
                compareBy(
                    { languagePriority(it.language, it.country) },
                    { countryNames[it.country] ?: it.country },
                    { it.name.lowercase() },
                ),
            )
        }
    }


    OkKeyHandler(
        enabled = true,
        focusedId = { focusedId.value },
        onShortPress = {
            focusedId.value
                ?.let { id -> visible.firstOrNull { it.id == id } }
                ?.let(onPlay)
        },
        onLongPress = { focusedId.value?.let(onToggleFavorite) },
    )

    Column(
        Modifier.fillMaxSize().padding(
            horizontal = if (columns == 1) 14.dp else 40.dp,
            vertical = 16.dp,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Радио",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "${formatCount(visible.size)} станций · ${catalog.countries.size} стран · ОК — слушать",
                color = Palette.TextDim,
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "fav") {
                FocusableChip("★ Избранные", selected = onlyFavorites) { onlyFavorites = !onlyFavorites }
            }
            item(key = "bycountry") {
                FocusableChip("По стране", selected = !sortByName) { sortByName = false }
            }
            item(key = "byname") {
                FocusableChip("По названию", selected = sortByName) { sortByName = true }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "all") {
                FocusableChip("Все страны", selected = countryId == null, small = true) { countryId = null }
            }
            items(orderedCountries.take(MAX_RADIO_COUNTRIES), key = { it.id }) { country ->
                FocusableChip(country.name, selected = countryId == country.id, small = true) {
                    countryId = if (countryId == country.id) null else country.id
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(visible, key = { _, station -> station.id }) { index, station ->
                val modifier = (if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier)
                    .onFocusChanged { state ->
                        if (state.isFocused) {
                            focusedId.value = station.id
                        } else if (focusedId.value == station.id) {
                            focusedId.value = null
                        }
                    }
                RadioCard(
                    station = station,
                    country = countryNames[station.country] ?: station.country,
                    isFavorite = station.id in favoriteIds,
                    modifier = modifier,
                    onClick = { onPlay(station) },
                )
            }
        }
        LaunchedEffect(visible) { runCatching { firstItemFocus.requestFocus() } }
    }
}

@Composable
private fun RadioCard(
    station: RadioStation,
    country: String,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FocusableCard(onClick = onClick, modifier = modifier.fillMaxWidth().height(64.dp)) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        country.takeIf { it.isNotEmpty() },
                        station.tags.firstOrNull(),
                        station.bitrate.takeIf { it > 0 }?.let { "$it kbps" },
                    ).joinToString(" · "),
                    color = Palette.Mint,
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
