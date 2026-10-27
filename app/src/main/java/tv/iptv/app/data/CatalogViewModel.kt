package tv.iptv.app.data

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

class CatalogViewModel(app: Application) : AndroidViewModel(app) {

    var catalog by mutableStateOf<Catalog?>(null)
        private set
    var cams by mutableStateOf<CamCatalog?>(null)
        private set
    var radio by mutableStateOf<RadioCatalog?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            try {
                catalog = withContext(Dispatchers.IO) {
                    read(app, "catalog.json", Catalog.serializer())
                }
                if (Features.cams) {
                    cams = withContext(Dispatchers.IO) {
                        read(app, "cams.json", CamCatalog.serializer())
                    }
                }
                radio = withContext(Dispatchers.IO) {
                    read(app, "radio.json", RadioCatalog.serializer())
                }
            } catch (t: Throwable) {
                error = t.message ?: t.javaClass.simpleName
            }
        }
    }

    private fun <T> read(context: Context, name: String, serializer: DeserializationStrategy<T>): T =
        context.assets.open(name).use { input ->
            json.decodeFromString(serializer, input.readBytes().decodeToString())
        }
}
