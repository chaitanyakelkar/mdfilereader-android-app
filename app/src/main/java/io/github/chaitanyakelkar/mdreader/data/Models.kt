package io.github.chaitanyakelkar.mdreader.data

import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

/** URI that loads the bundled sample document instead of a user file. */
const val SAMPLE_DOCUMENT_URI = "mdreader://sample"

data class LoadedFile(val name: String, val text: String)

enum class LoadError { NOT_FOUND, NO_ACCESS, TOO_LARGE, BINARY, UNKNOWN }

class DocumentLoadException(val error: LoadError, cause: Throwable? = null) : Exception(error.name, cause)

/** Reads document text. Throws [DocumentLoadException] on failure. */
fun interface DocumentSource {
    suspend fun load(uri: String): LoadedFile
}

data class RecentFile(
    val uri: String,
    val name: String,
    val openedAt: Long,
    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,
)

interface RecentsStore {
    val recents: Flow<List<RecentFile>>
    suspend fun find(uri: String): RecentFile?
    suspend fun recordOpened(uri: String, name: String, now: Long)
    suspend fun saveScroll(uri: String, index: Int, offset: Int)
    suspend fun remove(uri: String)
}

object RecentsCodec {
    const val MAX_RECENTS = 30

    fun encode(items: List<RecentFile>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("uri", item.uri)
                    .put("name", item.name)
                    .put("openedAt", item.openedAt)
                    .put("scrollIndex", item.scrollIndex)
                    .put("scrollOffset", item.scrollOffset),
            )
        }
        return array.toString()
    }

    /** Corrupt data must not break the home screen, so it decodes to an empty list. */
    fun decode(json: String?): List<RecentFile> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                RecentFile(
                    uri = item.getString("uri"),
                    name = item.getString("name"),
                    openedAt = item.getLong("openedAt"),
                    scrollIndex = item.optInt("scrollIndex"),
                    scrollOffset = item.optInt("scrollOffset"),
                )
            }
        } catch (_: org.json.JSONException) {
            emptyList()
        }
    }

    /** Moves [uri] to the front, keeping its scroll position, and caps the list length. */
    fun withOpened(items: List<RecentFile>, uri: String, name: String, now: Long): List<RecentFile> {
        val existing = items.firstOrNull { it.uri == uri }
        val updated = (existing?.copy(name = name, openedAt = now) ?: RecentFile(uri, name, now))
        return (listOf(updated) + items.filterNot { it.uri == uri }).take(MAX_RECENTS)
    }
}
