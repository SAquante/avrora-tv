package tv.iptv.app.data

import kotlinx.serialization.Serializable

@Serializable
data class RadioStation(
    val id: String,
    val name: String,
    val country: String = "",
    val url: String,
    val tags: List<String> = emptyList(),
    val codec: String = "",
    val bitrate: Int = 0,
    val language: String = "",
)

@Serializable
data class RadioCountry(val id: String, val name: String, val count: Int)

@Serializable
data class RadioCatalog(
    val countries: List<RadioCountry> = emptyList(),
    val stations: List<RadioStation> = emptyList(),
)
