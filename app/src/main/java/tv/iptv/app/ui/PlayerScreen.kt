package tv.iptv.app.ui

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import tv.iptv.app.MainActivity
import tv.iptv.app.data.Channel
import tv.iptv.app.data.Stream
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

private const val START_TIMEOUT_MS = 20_000L

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    languageId: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val isTelevision = remember {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK) ||
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEVISION)
    }
    var attempt by remember(channel.id) { mutableIntStateOf(0) }
    var notice by remember(channel.id) { mutableStateOf<String?>(null) }
    var buffering by remember(channel.id) { mutableStateOf(true) }
    var paused by remember(channel.id) { mutableStateOf(false) }

    // the same channel can have feeds in several languages; play the one that matches the list,
    // and among them the best quality available
    val streams = remember(channel.id, languageId) {
        channel.streams.sortedWith(
            compareByDescending<Stream> { languageId != null && it.language == languageId }
                .thenByDescending { qualityRank(it.quality) },
        )
    }

    val stream = streams.getOrNull(attempt)
    val playerRef = remember { mutableStateOf<ExoPlayer?>(null) }
    val scope = rememberCoroutineScope()

    val onError by rememberUpdatedState {
        if (attempt + 1 < streams.size) {
            notice = "Поток не отвечает, пробую следующий…"
            attempt++
        } else {
            attempt = streams.size
        }
    }

    // A visible action bar instead of a hidden long-press: DOWN opens it, LEFT/RIGHT switch,
    // OK acts, UP closes. The title overlay fades out on its own after a few seconds.
    var actionsOpen by remember(channel.id) { mutableStateOf(false) }
    var actionIndex by remember(channel.id) { mutableIntStateOf(0) }
    var infoVisible by remember(channel.id) { mutableStateOf(true) }
    LaunchedEffect(infoVisible) {
        if (infoVisible) {
            delay(4000)
            infoVisible = false
        }
    }
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val handler: (Int, KeyEvent) -> Boolean = { code, event ->
            val down = event.action == KeyEvent.ACTION_DOWN
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (down) {
                        infoVisible = true
                        if (!actionsOpen) {
                            actionsOpen = true
                            actionIndex = 0
                        } else {
                            actionIndex = (actionIndex + 1).coerceAtMost(1)
                        }
                    }
                    true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (down) {
                        actionsOpen = false
                        infoVisible = true
                    }
                    true
                }

                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (down && actionsOpen) actionIndex = 1 - actionIndex
                    true
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                -> {
                    if (down) {
                        infoVisible = true
                        if (actionsOpen) {
                            if (actionIndex == 0) {
                                actionsOpen = false
                                onToggleFavorite()
                                notice = if (isFavorite) "Убрано из избранного" else "Добавлено в избранное ★"
                            } else {
                                // cycle through the channel's streams, best quality first
                                if (streams.size > 1) {
                                    attempt = (attempt + 1) % streams.size
                                    notice = "Качество: ${streams[attempt].quality.ifBlank { "авто" }}"
                                }
                            }
                        } else {
                            playerRef.value?.let { it.playWhenReady = !it.playWhenReady }
                        }
                    }
                    true
                }

                else -> false
            }
        }
        activity?.keyHandler = handler
        onDispose { if (activity?.keyHandler === handler) activity.keyHandler = null }
    }

    // never keep playing in the background
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var wasPlaying by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    playerRef.value?.let {
                        wasPlaying = it.playWhenReady
                        it.pause()
                    }
                }

                androidx.lifecycle.Lifecycle.Event.ON_START -> {
                    if (wasPlaying) playerRef.value?.play()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(stream?.url, stream?.userAgent) {
        val player = stream?.let { buildPlayer(context, it) }
        playerRef.value = player
        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.w("IPTV", "playback failed: ${error.errorCodeName} ${error.message}", error)
                onError()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) notice = null
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                paused = !isPlaying
            }
        })

        // Dead streams often connect and then sit silent forever instead of failing
        val watchdog = scope.launch {
            delay(START_TIMEOUT_MS)
            if (player != null && playerRef.value === player && buffering) onError()
        }

        onDispose {
            watchdog.cancel()
            player?.release()
            if (playerRef.value === player) playerRef.value = null
        }
    }

    Box(
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
    ) {
        if (stream == null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Не удалось воспроизвести", color = Color.White, fontSize = 28.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Все потоки этого канала недоступны. Нажмите «Назад», чтобы выбрать другой.",
                    color = Palette.TextDim,
                )
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        keepScreenOn = true
                        this.player = playerRef.value
                    }
                },
                update = { it.player = playerRef.value },
                modifier = Modifier.fillMaxSize(),
            )

            if (buffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Palette.Accent,
                )
            }

        }

        if (actionsOpen) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(92.dp)
                .clip(CircleShape)
                .background(if (infoVisible) Color(0xCC1B100A) else Color(0x801B100A))
                .clickable { playerRef.value?.let { it.playWhenReady = !it.playWhenReady } },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (paused) "▶" else "❚❚",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
        }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionPill(
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
                )
            }
        }

        notice?.let { text ->
            Text(
                text = text,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun ActionPill(text: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = if (active) Palette.Background else Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Palette.Accent else Color(0xCC241812))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    )
}

@OptIn(UnstableApi::class)
private fun buildPlayer(context: Context, stream: Stream): ExoPlayer {    val http = DefaultHttpDataSource.Factory()
        .setUserAgent(stream.userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_AGENT)
        .setConnectTimeoutMs(8_000)
        .setReadTimeoutMs(15_000)
        .setAllowCrossProtocolRedirects(true)

    stream.referrer?.takeIf { it.isNotBlank() }?.let { referrer ->
        http.setDefaultRequestProperties(mapOf("Referer" to referrer))
    }

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(http))
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(stream.url))
            playWhenReady = true
            prepare()
        }
}
