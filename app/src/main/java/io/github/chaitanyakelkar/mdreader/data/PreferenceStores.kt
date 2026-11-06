package io.github.chaitanyakelkar.mdreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "mdreader")

enum class ThemeMode { SYSTEM, LIGHT, DARK, SEPIA }

enum class ReaderFont { SANS, SERIF }

data class ReaderSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val font: ReaderFont = ReaderFont.SANS,
    val textScale: Float = 1f,
    val loadRemoteImages: Boolean = true,
) {
    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.6f
        const val SCALE_STEP = 0.1f
    }
}

class SettingsRepository(private val store: DataStore<Preferences>) {
    val settings: Flow<ReaderSettings> = store.data.map { prefs ->
        ReaderSettings(
            theme = enumOrDefault(prefs[THEME], ThemeMode.SYSTEM),
            font = enumOrDefault(prefs[FONT], ReaderFont.SANS),
            textScale = (prefs[SCALE] ?: 1f).coerceIn(ReaderSettings.MIN_SCALE, ReaderSettings.MAX_SCALE),
            loadRemoteImages = prefs[REMOTE_IMAGES] ?: true,
        )
    }

    suspend fun setTheme(value: ThemeMode) = store.edit { it[THEME] = value.name }
    suspend fun setFont(value: ReaderFont) = store.edit { it[FONT] = value.name }
    suspend fun setTextScale(value: Float) = store.edit {
        it[SCALE] = value.coerceIn(ReaderSettings.MIN_SCALE, ReaderSettings.MAX_SCALE)
    }
    suspend fun setLoadRemoteImages(value: Boolean) = store.edit { it[REMOTE_IMAGES] = value }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val FONT = stringPreferencesKey("font")
        val SCALE = floatPreferencesKey("text_scale")
        val REMOTE_IMAGES = booleanPreferencesKey("remote_images")
    }
}

class DataStoreRecents(private val store: DataStore<Preferences>) : RecentsStore {
    override val recents: Flow<List<RecentFile>> = store.data.map { RecentsCodec.decode(it[RECENTS]) }

    override suspend fun find(uri: String): RecentFile? = recents.first().firstOrNull { it.uri == uri }

    override suspend fun recordOpened(uri: String, name: String, now: Long) = update {
        RecentsCodec.withOpened(it, uri, name, now)
    }

    override suspend fun saveScroll(uri: String, index: Int, offset: Int) = update { items ->
        items.map { if (it.uri == uri) it.copy(scrollIndex = index, scrollOffset = offset) else it }
    }

    override suspend fun remove(uri: String) = update { items -> items.filterNot { it.uri == uri } }

    private suspend fun update(transform: (List<RecentFile>) -> List<RecentFile>) {
        store.edit { prefs -> prefs[RECENTS] = RecentsCodec.encode(transform(RecentsCodec.decode(prefs[RECENTS]))) }
    }

    private companion object {
        val RECENTS = stringPreferencesKey("recents")
    }
}
