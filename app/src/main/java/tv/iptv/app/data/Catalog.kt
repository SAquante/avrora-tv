package tv.iptv.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Stream(
    val url: String,
    val quality: String = "",
    val language: String = "",
    val userAgent: String? = null,
    val referrer: String? = null,
)

@Serializable
data class Channel(
    val id: String,
    val name: String,
    val nameRu: String = "",
    val logo: String = "",
    val country: String = "",
    val rank: Int = 900,
    val categories: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val streams: List<Stream> = emptyList(),
) {
    /**
     * Russian title. Russian channels need no clarification, so the original name is only
     * appended in brackets for foreign ones.
     */
    val title: String
        get() {
            if (nameRu.isBlank() || nameRu.equals(name, ignoreCase = true)) return name
            if ("rus" in languages || country == "ru") return nameRu
            val base = name.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim().ifEmpty { name }
            return if (nameRu.equals(base, ignoreCase = true)) nameRu else "$nameRu ($base)"
        }
}

@Serializable
data class Language(val id: String, val name: String, val count: Int)

@Serializable
data class Category(val id: String, val name: String, val count: Int)

@Serializable
data class Country(val id: String, val name: String, val count: Int)

@Serializable
data class Catalog(
    val version: Int = 1,
    val languages: List<Language> = emptyList(),
    val categories: List<Category> = emptyList(),
    val countries: List<Country> = emptyList(),
    val channels: List<Channel> = emptyList(),
)
