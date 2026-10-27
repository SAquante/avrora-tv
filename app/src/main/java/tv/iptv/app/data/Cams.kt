package tv.iptv.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Cam(
    val id: String,
    val name: String,
    val country: String = "",
    val video: String,
)

@Serializable
data class CamCountry(val id: String, val name: String, val count: Int)

@Serializable
data class CamCatalog(
    val countries: List<CamCountry> = emptyList(),
    val cams: List<Cam> = emptyList(),
)
