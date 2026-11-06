package io.github.chaitanyakelkar.mdreader

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/** Stands in for a file manager's provider, including the awkward cases. */
class TestDocsProvider : ContentProvider() {
    override fun onCreate() = true

    private fun bytesFor(path: String): ByteArray = when (path) {
        "/utf16.md" -> byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + UTF16_TEXT.toByteArray(Charsets.UTF_16LE)
        "/crlf.md" -> "# CRLF title\r\n\r\n| a | b |\r\n|---|---|\r\n| 1 | 2 |\r\n".toByteArray()
        "/binary.md" -> byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0, 0, 0, 0x0D, 0x49, 0x48, 0x44, 0x52)
        "/big.md" -> ByteArray(11 * 1024 * 1024) { 'a'.code.toByte() }
        else -> throw FileNotFoundException(path)
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val file = File.createTempFile("doc", ".md", context!!.cacheDir)
        file.writeBytes(bytesFor(uri.path.orEmpty()))
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            .also { file.delete() }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply {
        addRow(arrayOf<Any>("Named " + uri.lastPathSegment))
    }

    override fun getType(uri: Uri) = "application/octet-stream"
    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

    companion object {
        const val UTF16_TEXT = "# Grüße from UTF-16\n\nBody ✓ text\n"
    }
}
