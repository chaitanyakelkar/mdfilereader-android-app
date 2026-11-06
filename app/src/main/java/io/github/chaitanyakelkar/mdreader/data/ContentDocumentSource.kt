package io.github.chaitanyakelkar.mdreader.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

class ContentDocumentSource(private val context: Context) : DocumentSource {

    override suspend fun load(uri: String): LoadedFile = withContext(Dispatchers.IO) {
        if (uri == SAMPLE_DOCUMENT_URI) {
            val text = context.assets.open(SAMPLE_ASSET).bufferedReader().use { it.readText() }
            return@withContext LoadedFile(SAMPLE_NAME, text)
        }
        val parsed = uri.toUri()
        val bytes = try {
            val stream = context.contentResolver.openInputStream(parsed)
                ?: throw DocumentLoadException(LoadError.NOT_FOUND)
            stream.use { readLimited(it) }
        } catch (e: FileNotFoundException) {
            throw DocumentLoadException(LoadError.NOT_FOUND, e)
        } catch (e: SecurityException) {
            throw DocumentLoadException(LoadError.NO_ACCESS, e)
        } catch (e: java.io.IOException) {
            throw DocumentLoadException(LoadError.UNKNOWN, e)
        }
        when (val decoded = DocumentDecoder.decode(bytes)) {
            is DecodeResult.Text -> LoadedFile(displayName(parsed), decoded.text)
            DecodeResult.Binary -> throw DocumentLoadException(LoadError.BINARY)
        }
    }

    private fun readLimited(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            if (output.size() > MAX_FILE_BYTES) throw DocumentLoadException(LoadError.TOO_LARGE)
        }
        return output.toByteArray()
    }

    private fun displayName(uri: Uri): String {
        val fromProvider = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (_: RuntimeException) {
            null // Some providers throw on query but still serve the content.
        }
        return fromProvider ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
    }

    companion object {
        const val MAX_FILE_BYTES = 10 * 1024 * 1024
        private const val SAMPLE_ASSET = "sample.md"
        private const val SAMPLE_NAME = "Sample.md"
    }
}
