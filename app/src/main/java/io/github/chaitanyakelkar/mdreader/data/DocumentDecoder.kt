package io.github.chaitanyakelkar.mdreader.data

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

sealed interface DecodeResult {
    data class Text(val text: String, val charset: String) : DecodeResult
    data object Binary : DecodeResult
}

/**
 * Turns raw bytes into text. Handles the encodings that make Markdown files look broken on
 * phones: byte order marks, UTF-16 from Windows tools, legacy Windows-1252, and CRLF.
 */
object DocumentDecoder {
    private val UTF_8 = Charsets.UTF_8
    private val WINDOWS_1252 = Charset.forName("windows-1252")
    private const val SNIFF_LENGTH = 8 * 1024

    fun decode(bytes: ByteArray): DecodeResult {
        val (charset, bomLength) = detectBom(bytes) ?: detectBomlessUtf16(bytes) ?: (null to 0)
        if (charset != null) {
            val text = String(bytes, bomLength, bytes.size - bomLength, charset)
            return DecodeResult.Text(normalizeLineEndings(text), charset.name())
        }
        if (looksBinary(bytes)) return DecodeResult.Binary

        val utf8 = decodeStrictUtf8(bytes)
        return if (utf8 != null) {
            DecodeResult.Text(normalizeLineEndings(utf8), UTF_8.name())
        } else {
            DecodeResult.Text(normalizeLineEndings(String(bytes, WINDOWS_1252)), WINDOWS_1252.name())
        }
    }

    private fun detectBom(b: ByteArray): Pair<Charset, Int>? = when {
        b.startsWith(0xFF, 0xFE, 0x00, 0x00) -> Charset.forName("UTF-32LE") to 4
        b.startsWith(0x00, 0x00, 0xFE, 0xFF) -> Charset.forName("UTF-32BE") to 4
        b.startsWith(0xEF, 0xBB, 0xBF) -> UTF_8 to 3
        b.startsWith(0xFF, 0xFE) -> Charsets.UTF_16LE to 2
        b.startsWith(0xFE, 0xFF) -> Charsets.UTF_16BE to 2
        else -> null
    }

    /** ASCII-heavy UTF-16 without a BOM has a NUL in nearly every other byte. */
    private fun detectBomlessUtf16(b: ByteArray): Pair<Charset, Int>? {
        val length = minOf(b.size, SNIFF_LENGTH)
        if (length < 4 || length % 2 != 0) return null
        var evenNulls = 0
        var oddNulls = 0
        for (i in 0 until length) {
            if (b[i] == 0.toByte()) if (i % 2 == 0) evenNulls++ else oddNulls++
        }
        val pairs = length / 2
        return when {
            oddNulls * 10 >= pairs * 9 && evenNulls == 0 -> Charsets.UTF_16LE to 0
            evenNulls * 10 >= pairs * 9 && oddNulls == 0 -> Charsets.UTF_16BE to 0
            else -> null
        }
    }

    private fun looksBinary(b: ByteArray): Boolean {
        val length = minOf(b.size, SNIFF_LENGTH)
        for (i in 0 until length) if (b[i] == 0.toByte()) return true
        return false
    }

    private fun decodeStrictUtf8(bytes: ByteArray): String? = try {
        UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

    private fun normalizeLineEndings(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n')

    private fun ByteArray.startsWith(vararg prefix: Int): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it].toByte() }
}
