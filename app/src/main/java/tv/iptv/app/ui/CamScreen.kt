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
import tv.iptv.app.data.Cam
import tv.iptv.app.data.CamCatalog

private const val MAX_CAM_COUNTRIES = 12

@Composable
fun CamScreen(
    cams: CamCatalog,
    favoriteIds: Set<String>,
    firstItemFocus: FocusRequester,
    columns: Int = 2,
    onToggleFavorite: (String) -> Unit,
    onPlay: (Cam) -> Unit,
) {
    var sortByName by remember { mutableStateOf(false) }
    var countryId by remember { mutableStateOf<String?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }
    val focusedId = remember { mutableStateOf<String?>(null) }

    val countryNames = remember(cams) { cams.countries.associate { it.id to it.name } }
    val orderedCountries = remember(cams) {
        cams.countries.sortedWith(compareBy({ if (it.id == "ru") 0 else 1 }, { -it.count }))
    }

    val visible = remember(cams, sortByName, countryId, onlyFavorites, favoriteIds, countryNames) {
        val base = cams.cams.filter { cam ->
            (countryId == null || cam.country == countryId) &&
                (!onlyFavorites || cam.id in favoriteIds)
        }
        if (sortByName) {
            base.sortedBy { it.name.lowercase() }
        } else {
            base.sortedWith(
                compareBy({ countryNames[it.country] ?: it.country }, { it.name.lowercase() }),
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

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Веб-камеры",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "${formatCount(visible.size)} камер · ${cams.countries.size} стран · ОК — смотреть",
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
            items(orderedCountries.take(MAX_CAM_COUNTRIES), key = { it.id }) { country ->
                FocusableChip(country.name, selected = countryId == country.id, small = true) {
                    countryId = if (countryId == country.id) null else country.id
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (visible.isEmpty()) {
            Text(
                text = if (onlyFavorites) {
                    "Пока пусто. Длительное нажатие ОК на камере добавит её сюда."
                } else {
                    "Здесь нет камер"
                },
                color = Palette.TextDim,
                modifier = Modifier.padding(top = 40.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(visible, key = { _, cam -> cam.id }) { index, cam ->
                    val modifier = (if (index == 0) Modifier.focusRequester(firstItemFocus) else Modifier)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                focusedId.value = cam.id
                            } else if (focusedId.value == cam.id) {
                                focusedId.value = null
                            }
                        }
                    CamCard(
                        cam = cam,
                        country = countryNames[cam.country] ?: cam.country,
                        isFavorite = cam.id in favoriteIds,
                        modifier = modifier,
                        onClick = { onPlay(cam) },
                    )
                }
            }
            LaunchedEffect(visible) { runCatching { firstItemFocus.requestFocus() } }
        }
    }
}

@Composable
private fun CamCard(
    cam: Cam,
    country: String,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    FocusableCard(onClick = onClick, modifier = modifier.fillMaxWidth().height(132.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = cam.name,
                color = Color.White,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = country,
                    color = Palette.Mint,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isFavorite) {
                    Text("★", color = Palette.Accent, fontSize = 13.sp)
                }
            }
        }
    }
}
