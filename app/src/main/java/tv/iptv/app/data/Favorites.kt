package tv.iptv.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Favorites(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("favorites", Context.MODE_PRIVATE)

    var channelIds by mutableStateOf(read(CHANNEL_KEY))
        private set

    var camIds by mutableStateOf(read(CAM_KEY))
        private set

    var radioIds by mutableStateOf(read(RADIO_KEY))
        private set

    fun toggleChannel(channelId: String) {
        channelIds = toggle(CHANNEL_KEY, channelIds, channelId)
    }

    fun toggleCam(camId: String) {
        camIds = toggle(CAM_KEY, camIds, camId)
    }

    fun toggleRadio(stationId: String) {
        radioIds = toggle(RADIO_KEY, radioIds, stationId)
    }

    private fun toggle(key: String, current: Set<String>, id: String): Set<String> {
        val next = if (id in current) current - id else current + id
        prefs.edit().putStringSet(key, next).apply()
        return next
    }

    private fun read(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()

    private companion object {
        const val CHANNEL_KEY = "channel_ids"
        const val CAM_KEY = "cam_ids"
        const val RADIO_KEY = "radio_ids"
    }
}
