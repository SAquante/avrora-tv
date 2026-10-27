package tv.iptv.app.ui

import android.content.Context
import android.content.ContextWrapper
import tv.iptv.app.MainActivity

const val ALL_LANGUAGES = "all"
const val FAVORITES = "favorites"

fun Context.findActivity(): MainActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is MainActivity) return current
        current = current.baseContext
    }
    return current as? MainActivity
}

fun qualityRank(quality: String): Int = when {    quality.contains("4k", ignoreCase = true) -> 4000
    quality.contains("2160") -> 2160
    quality.contains("1440") -> 1440
    quality.contains("1080") -> 1080
    quality.contains("720") -> 720
    quality.contains("576") -> 576
    quality.contains("540") -> 540
    quality.contains("480") -> 480
    quality.contains("360") -> 360
    else -> 0
}

/** Webcams are YouTube lives; the embedded player is blocked on TV, the YouTube app is not. */
fun openInYouTube(context: Context, videoId: String) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("https://www.youtube.com/watch?v=" + videoId),
    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
