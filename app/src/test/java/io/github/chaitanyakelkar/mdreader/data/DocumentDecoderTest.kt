package io.github.chaitanyakelkar.mdreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class DocumentDecoderTest {
    private val sample = "# Título ✓ 日本語\n"

    private fun text(bytes: ByteArray) = (DocumentDecoder.decode(bytes) as DecodeResult.Text).text

    private fun bom(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }

    @Test fun `plain utf8`() = assertEquals(sample, text(sample.toByteArray()))

    @Test fun `utf8 bom is removed so the first heading still parses`() {
        assertEquals(sample, text(bom(0xEF, 0xBB, 0xBF) + sample.toByteArray()))
    }

    @Test fun `utf16 with bom`() {
        assertEquals(sample, text(bom(0xFF, 0xFE) + sample.toByteArray(Charsets.UTF_16LE)))
        assertEquals(sample, text(bom(0xFE, 0xFF) + sample.toByteArray(Charsets.UTF_16BE)))
    }

    @Test fun `utf32 with bom`() {
        assertEquals(sample, text(bom(0xFF, 0xFE, 0, 0) + sample.toByteArray(Charset.forName("UTF-32LE"))))
    }

    @Test fun `ascii utf16 without bom`() {
        val ascii = "# Plain heading\n\nSome text here.\n"
        assertEquals(ascii, text(ascii.toByteArray(Charsets.UTF_16LE)))
        assertEquals(ascii, text(ascii.toByteArray(Charsets.UTF_16BE)))
    }

    @Test fun `invalid utf8 falls back to windows 1252`() {
        val legacy = "café “quoted”"
        val result = DocumentDecoder.decode(legacy.toByteArray(Charset.forName("windows-1252"))) as DecodeResult.Text
        assertEquals(legacy, result.text)
        assertEquals("windows-1252", result.charset)
    }

    @Test fun `line endings are normalized`() {
        assertEquals("a\nb\nc\n", text("a\r\nb\rc\n".toByteArray()))
    }

    @Test fun `binary data is rejected`() {
        val png = bom(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 0x49, 0x48, 0x44, 0x52)
        assertTrue(DocumentDecoder.decode(png) is DecodeResult.Binary)
    }

    @Test fun `empty file is valid empty text`() = assertEquals("", text(ByteArray(0)))

    @Test fun `single byte and odd sized files do not throw`() {
        assertEquals("a", text("a".toByteArray()))
        assertEquals("abc", text("abc".toByteArray()))
    }
}
