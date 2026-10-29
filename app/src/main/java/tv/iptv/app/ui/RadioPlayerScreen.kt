package tv.iptv.app.ui

import android.view.KeyEvent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tv.iptv.app.data.RadioStation
import tv.iptv.app.playback.RadioPlayback

@Composable
fun RadioPlayerScreen(
    station: RadioStation,
    country: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    var actionsOpen by remember(station.id) { mutableStateOf(false) }
    val paused = !RadioPlayback.isPlaying
    val buffering = RadioPlayback.isBuffering

    LaunchedEffect(station.id) {
        RadioPlayback.play(context, station)
    }

    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val handler: (Int, KeyEvent) -> Boolean = { code, event ->
            val down = event.action == KeyEvent.ACTION_DOWN
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (down) actionsOpen = true
                    true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (down) actionsOpen = false
                    true
                }

                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (down) {
                        if (actionsOpen) {
                            actionsOpen = false
                            onToggleFavorite()
                        } else {
                            RadioPlayback.toggle()
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

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF26150F), Color(0xFF140D0A)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = station.name,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = listOfNotNull(
                    country.takeIf { it.isNotEmpty() },
                    station.tags.firstOrNull(),
                    station.codec.takeIf { it.isNotEmpty() },
                    station.bitrate.takeIf { it > 0 }?.let { "$it kbps" },
                ).joinToString(" · "),
                color = Palette.Mint,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(36.dp))
            if (buffering) {
                CircularProgressIndicator(color = Palette.Accent)
            } else {
                Text(
                    text = if (paused) "Пауза · ОК — продолжить" else "Играет · ОК — пауза",
                    color = Palette.TextDim,
                    fontSize = 16.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = "Радио",
                color = Palette.Accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = "Назад — к списку станций · ▼ — избранное",
            color = Palette.TextDim,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
        )

        if (actionsOpen) {
            Text(
                text = if (isFavorite) "★ В избранном — убрать" else "☆ В избранное",
                color = Palette.Background,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .background(Palette.Accent)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
