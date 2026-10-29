package tv.iptv.app.playback

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import tv.iptv.app.data.RadioStation

/** One radio player for the app and its foreground service. */
@UnstableApi
object RadioPlayback {
    private var player: ExoPlayer? = null
    private var service: RadioService? = null

    var station: RadioStation? = null
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set

    fun play(context: Context, radio: RadioStation) {
        val app = context.applicationContext
        if (station?.id == radio.id && player != null) {
            player?.play()
            ensureService(app)
            return
        }
        releasePlayer()
        station = radio
        isBuffering = true
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
        player = ExoPlayer.Builder(app)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        this@RadioPlayback.isBuffering =
                            playbackState == Player.STATE_BUFFERING ||
                                playbackState == Player.STATE_IDLE
                        service?.updateNotification()
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        this@RadioPlayback.isPlaying = playing
                        service?.updateNotification()
                    }
                })
                setMediaItem(MediaItem.fromUri(radio.url))
                playWhenReady = true
                prepare()
            }
        ensureService(app)
    }

    fun toggle() {
        player?.let { it.playWhenReady = !it.playWhenReady }
    }

    fun stop() {
        releasePlayer()
        station = null
        service?.stopSelf()
    }

    internal fun bindService(radioService: RadioService) {
        service = radioService
        radioService.updateNotification()
    }

    internal fun unbindService(radioService: RadioService) {
        if (service === radioService) service = null
    }

    private fun ensureService(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, RadioService::class.java),
        )
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        isPlaying = false
        isBuffering = false
    }
}
