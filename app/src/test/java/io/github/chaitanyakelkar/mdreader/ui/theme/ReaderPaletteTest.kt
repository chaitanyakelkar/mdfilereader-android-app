package io.github.chaitanyakelkar.mdreader.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Enforces NFR-5: WCAG AA contrast in every reader theme. */
class ReaderPaletteTest {
    private val aa = 4.5

    private fun assertAa(name: String, foreground: Long, background: Long) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$name is only %.2f:1".format(ratio), ratio >= aa)
    }

    @Test fun `contrast formula matches known values`() {
        assertEquals(21.0, contrastRatio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, contrastRatio(0xFF777777, 0xFF777777), 0.01)
    }

    @Test fun `all text pairs meet AA in every theme`() {
        ReaderPalette.all.forEach { p ->
            val theme = if (p.isDark) "dark" else "light ${p.background.toString(16)}"
            listOf(p.background, p.sheet, p.codeBackground).forEach { surface ->
                assertAa("$theme text on ${surface.toString(16)}", p.text, surface)
                assertAa("$theme muted on ${surface.toString(16)}", p.muted, surface)
            }
            assertAa("$theme link", p.accent, p.background)
            assertAa("$theme button label", p.background, p.accent)
            assertAa("$theme keyword", p.codeKeyword, p.codeBackground)
            assertAa("$theme string", p.codeString, p.codeBackground)
            assertAa("$theme comment", p.codeComment, p.codeBackground)
            assertAa("$theme number", p.codeNumber, p.codeBackground)
            assertAa("$theme find highlight", p.text, p.findHighlight)
            assertAa("$theme current find", p.text, p.findCurrent)
        }
    }
}
