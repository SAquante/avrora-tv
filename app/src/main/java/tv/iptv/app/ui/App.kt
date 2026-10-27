package tv.iptv.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import tv.iptv.app.BuildConfig
import tv.iptv.app.R
import tv.iptv.app.data.Catalog
import tv.iptv.app.data.CatalogViewModel
import tv.iptv.app.data.Channel
import tv.iptv.app.data.Favorites

@Composable
fun IptvApp(vm: CatalogViewModel = viewModel()) {
    val catalog = vm.catalog
    val cams = vm.cams
    val radio = vm.radio
    val context = LocalContext.current
    val favorites = remember { Favorites(context) }
    var section by remember { mutableStateOf(SECTION_LANGUAGES) }
    var languageId by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var playingCam by remember { mutableStateOf<tv.iptv.app.data.Cam?>(null) }
    var playingRadio by remember { mutableStateOf<tv.iptv.app.data.RadioStation?>(null) }
    val contentFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    // self-update: look for a newer release once per launch
    var update by remember { mutableStateOf<tv.iptv.app.data.Updater.Release?>(null) }
    var downloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        val release = tv.iptv.app.data.Updater.latest() ?: return@LaunchedEffect
        if (tv.iptv.app.data.Updater.isNewer(release.version, BuildConfig.VERSION_NAME)) {
            update = release
        }
    }

    BackHandler(
        enabled = playing != null || playingCam != null || playingRadio != null ||
            languageId != null || section != SECTION_LANGUAGES,
    ) {
        when {
            playingRadio != null -> playingRadio = null
            playingCam != null -> playingCam = null
            playing != null -> playing = null
            languageId != null -> languageId = null
            section != SECTION_LANGUAGES -> section = SECTION_LANGUAGES
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            background = Palette.Background,
            surface = Palette.Card,
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = Palette.Background) {
            Box(Modifier.fillMaxSize()) {
                Backdrop()

                when {
                    catalog == null -> Loading(vm.error)
                    else -> Row(Modifier.fillMaxSize()) {
                        Sidebar(
                            selected = section,
                            onSelect = { picked ->
                                section = picked
                                languageId = null
                            },
                            onEnterContent = { runCatching { contentFocus.requestFocus() } },
                        )
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            when {
                                section == SECTION_CAMS -> {
                                    if (cams == null) {
                                        Loading(null)
                                    } else {
                                        CamScreen(
                                            cams = cams!!,
                                            favoriteIds = favorites.camIds,
                                            firstItemFocus = contentFocus,
                                            onToggleFavorite = favorites::toggleCam,
                                            onPlay = { playingCam = it },
                                        )
                                    }
                                }

                                section == SECTION_RADIO -> {
                                    if (radio == null) {
                                        Loading(null)
                                    } else {
                                        RadioScreen(
                                            catalog = radio!!,
                                            favoriteIds = favorites.radioIds,
                                            firstItemFocus = contentFocus,
                                            onToggleFavorite = favorites::toggleRadio,
                                            onPlay = { playingRadio = it },
                                        )
                                    }
                                }

                                else -> {
                                    val scope = channelScope(section, languageId)
                                    if (scope == null) {
                                        LanguageScreen(catalog, contentFocus) { languageId = it; section = SECTION_LANGUAGES }
                                    } else {
                                        ChannelScreen(
                                            catalog = catalog,
                                            languageId = scope,
                                            playingId = playing?.id,
                                            favoriteIds = favorites.channelIds,
                                            firstItemFocus = contentFocus,
                                            onToggleFavorite = favorites::toggleChannel,
                                            onPlay = { playing = it },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                playing?.let { channel ->
                    PlayerScreen(
                        channel = channel,
                        languageId = languageId?.takeIf { it != ALL_LANGUAGES && it != FAVORITES },
                        isFavorite = channel.id in favorites.channelIds,
                        onToggleFavorite = { favorites.toggleChannel(channel.id) },
                    )
                }

                playingCam?.let { cam ->
                    val countryName = cams?.countries?.firstOrNull { it.id == cam.country }?.name
                        ?: cam.country
                    CamPlayerScreen(cam, countryName)
                }

                playingRadio?.let { station ->
                    val countryName = radio?.countries?.firstOrNull { it.id == station.country }?.name
                        ?: station.country
                    RadioPlayerScreen(
                        station = station,
                        country = countryName,
                        isFavorite = station.id in favorites.radioIds,
                        onToggleFavorite = { favorites.toggleRadio(station.id) },
                    )
                }

                update?.let { release ->
                    UpdateDialog(
                        version = release.version,
                        busy = downloading,
                        onInstall = {
                            if (!downloading) {
                                downloading = true
                                scope.launch {
                                    val apk = tv.iptv.app.data.Updater.download(context, release.apkUrl)
                                    downloading = false
                                    if (apk != null) {
                                        tv.iptv.app.data.Updater.install(context, apk)
                                        update = null
                                    }
                                }
                            }
                        },
                        onLater = { update = null },
                    )
                }
            }
        }
    }
}

private fun channelScope(section: String, languageId: String?): String? = when {
    languageId != null -> languageId
    section == SECTION_ALL -> ALL_LANGUAGES
    section == SECTION_FAVORITES -> FAVORITES
    else -> null
}

@Composable
private fun Backdrop() {
    Image(
        painter = painterResource(R.drawable.bg_aurora),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )

    // slow breathing sun glow
    val transition = rememberInfiniteTransition(label = "dawn")
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    Box(
        modifier = Modifier
            .size(1100.dp)
            .offset(x = (-260).dp, y = (-420).dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Palette.Accent.copy(alpha = 0.26f * glow),
                        Palette.Accent.copy(alpha = 0.08f * glow),
                        Color.Transparent,
                    ),
                ),
            ),
    )

    // warm veil so the photo reads as a sunny dawn rather than grey dusk
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0x26FF8C42),
                    0.45f to Color(0x33FF7A2F),
                    1f to Color(0x59FF6A1F),
                ),
            ),
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0x1F000000),
                    0.30f to Color(0x99160E09),
                    1f to Color(0xEB170E09),
                ),
            ),
    )
}

@Composable
private fun Loading(error: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error == null) {
            CircularProgressIndicator(color = Palette.Accent)
            Spacer(Modifier.height(20.dp))
            Text("Загружаю каталог…", color = Palette.TextDim)
        } else {
            Text("Не удалось загрузить каталог", color = Palette.Accent, fontSize = 24.sp)
            Spacer(Modifier.height(12.dp))
            Text(error, color = Palette.TextDim)
        }
    }
}

@Composable
private fun UpdateDialog(
    version: String,
    busy: Boolean,
    onInstall: () -> Unit,
    onLater: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xD9000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2A1A12))
                .border(2.dp, Palette.Accent, RoundedCornerShape(20.dp))
                .padding(28.dp),
        ) {
            Text(
                text = "Доступно обновление",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (busy) "Скачиваю версию $version…" else "Версия $version — установить сейчас?",
                color = Palette.TextDim,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(22.dp))
            if (!busy) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusableChip("Установить", selected = true) { onInstall() }
                    FocusableChip("Позже", selected = false) { onLater() }
                }
            }
        }
    }
}

@Composable
fun Header(title: String, subtitle: String) {
    Column {
        Text(title, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = Palette.TextDim, fontSize = 15.sp)
    }
}

fun languageTitle(catalog: Catalog, languageId: String): String = when (languageId) {
    ALL_LANGUAGES -> "Все каналы"
    FAVORITES -> "Избранное"
    else -> catalog.languages.firstOrNull { it.id == languageId }?.name ?: languageId
}
