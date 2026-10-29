package tv.iptv.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Self-update from the project's GitHub releases: the app ships only links, never APKs. */
object Updater {

    private const val OWNER = "SAquante"
    private const val REPO = "avrora-tv"

    data class Release(val version: String, val apkUrl: String)

    suspend fun latest(): Release? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
                .openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AvroraTV")
            }
            if (connection.responseCode != 200) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val version = json.optString("tag_name").removePrefix("v").ifBlank { return@withContext null }
            val assets = json.optJSONArray("assets") ?: return@withContext null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.optString("name")
                if (name.endsWith(".apk")) {
                    return@withContext Release(version, asset.optString("browser_download_url"))
                }
            }
            null
        } catch (t: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun isNewer(latest: String, current: String): Boolean {
        val remote = latest.split('.').mapNotNull { it.trim().toIntOrNull() }
        val local = current.split('.').mapNotNull { it.trim().toIntOrNull() }
        for (index in 0 until maxOf(remote.size, local.size)) {
            val r = remote.getOrElse(index) { 0 }
            val l = local.getOrElse(index) { 0 }
            if (r != l) return r > l
        }
        return false
    }

    suspend fun download(context: Context, url: String): File? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            val apk = File(directory, "avrora-tv.apk")
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "AvroraTV")
            }
            connection.inputStream.use { input ->
                apk.outputStream().use { output -> input.copyTo(output) }
            }
            connection.disconnect()
            if (apk.length() > 0) apk else null
        } catch (t: Throwable) {
            null
        }
    }

    fun install(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
