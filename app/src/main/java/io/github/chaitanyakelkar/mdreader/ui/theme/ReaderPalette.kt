package io.github.chaitanyakelkar.mdreader.ui.theme

/**
 * Reader design tokens from docs/02-ui-design.md, as raw ARGB so that a plain JVM test can
 * verify WCAG contrast for every theme.
 */
data class ReaderPalette(
    val isDark: Boolean,
    val background: Long,
    val text: Long,
    val muted: Long,
    val accent: Long,
    val codeBackground: Long,
    val divider: Long,
    val sheet: Long,
    val findHighlight: Long,
    val findCurrent: Long,
    val codeKeyword: Long,
    val codeString: Long,
    val codeComment: Long,
    val codeNumber: Long,
) {
    companion object {
        val Light = ReaderPalette(
            isDark = false,
            background = 0xFFFBFBF9,
            text = 0xFF1C1C1A,
            muted = 0xFF5C5C57,
            accent = 0xFF2A5B8C,
            codeBackground = 0xFFF0F0EC,
            divider = 0xFFDDDDD8,
            sheet = 0xFFF4F4F0,
            findHighlight = 0xFFFFE27A,
            findCurrent = 0xFFFFB74D,
            codeKeyword = 0xFF8A2C8F,
            codeString = 0xFF2C6E3F,
            codeComment = 0xFF5C655C,
            codeNumber = 0xFF8A4B00,
        )

        val Dark = ReaderPalette(
            isDark = true,
            background = 0xFF141517,
            text = 0xFFE6E5E1,
            muted = 0xFFA5A6A2,
            accent = 0xFF9CC3EA,
            codeBackground = 0xFF212327,
            divider = 0xFF34363B,
            sheet = 0xFF1E2023,
            findHighlight = 0xFF6B5A12,
            findCurrent = 0xFF9A5210,
            codeKeyword = 0xFFD7A6DB,
            codeString = 0xFF9AD0A8,
            codeComment = 0xFF9AA39A,
            codeNumber = 0xFFE3B27A,
        )

        val Sepia = ReaderPalette(
            isDark = false,
            background = 0xFFF5ECD9,
            text = 0xFF3B2F20,
            muted = 0xFF6A5A44,
            accent = 0xFF7A4B1E,
            codeBackground = 0xFFEBDFC6,
            divider = 0xFFD8CAAC,
            sheet = 0xFFEFE4CC,
            findHighlight = 0xFFF2CC6B,
            findCurrent = 0xFFE8A33D,
            codeKeyword = 0xFF7A2F6B,
            codeString = 0xFF3F6A2E,
            codeComment = 0xFF645540,
            codeNumber = 0xFF8A4300,
        )

        val all = listOf(Light, Dark, Sepia)
    }
}

/** WCAG 2.1 contrast ratio between two opaque colors. */
fun contrastRatio(a: Long, b: Long): Double {
    val first = relativeLuminance(a)
    val second = relativeLuminance(b)
    return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
}

private fun relativeLuminance(argb: Long): Double {
    fun channel(shift: Int): Double {
        val value = ((argb shr shift) and 0xFF) / 255.0
        return if (value <= 0.03928) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}
