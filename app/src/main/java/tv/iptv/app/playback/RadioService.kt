package tv.iptv.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import tv.iptv.app.MainActivity

/** Keeps radio audio alive when the app is in the background. */
class RadioService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
        RadioPlayback.bindService(this)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> RadioPlayback.toggle()
            ACTION_STOP -> RadioPlayback.stop()
        }
        if (intent?.action != ACTION_STOP) updateNotification()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        RadioPlayback.unbindService(this)
        stopForeground(true)
        super.onDestroy()
    }

    fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val playing = RadioPlayback.isPlaying
        val title = RadioPlayback.station?.name ?: "Avrora TV"
        val toggle = PendingIntent.getService(
            this,
            1,
            Intent(this, RadioService::class.java).setAction(ACTION_TOGGLE),
            pendingFlags(),
        )
        val stop = PendingIntent.getService(
            this,
            2,
            Intent(this, RadioService::class.java).setAction(ACTION_STOP),
            pendingFlags(),
        )
        val open = PendingIntent.getActivity(
            this,
            3,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            pendingFlags(),
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(if (playing) "Радио играет" else "Радио на паузе")
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Пауза" else "Играть",
                toggle,
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Стоп", stop)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Радио",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

    private companion object {
        const val CHANNEL_ID = "radio"
        const val NOTIFICATION_ID = 42
        const val ACTION_TOGGLE = "tv.iptv.app.RADIO_TOGGLE"
        const val ACTION_STOP = "tv.iptv.app.RADIO_STOP"
    }
}
