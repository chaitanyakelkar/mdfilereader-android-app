package io.github.chaitanyakelkar.mdreader.markdown

/**
 * Minimal handling for the HTML commonly embedded in READMEs. HTML is never interpreted:
 * images are extracted, and everything else is reduced to its text.
 */
internal object HtmlText {
    private val comment = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val hiddenElement = Regex(
        "<(script|style)\\b.*?</\\1\\s*>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    private val breakTag = Regex("<(br|/p|/div|/h[1-6]|/li|/tr|/summary)\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val tag = Regex("</?[a-zA-Z!][^>]*>")
    private val imgTag = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val spaces = Regex("[ \\t]+")
    private val blankLines = Regex("\\s*\\n\\s*")
    private val lineBreak = Regex("^<br\\s*/?>$", RegexOption.IGNORE_CASE)

    fun isLineBreak(html: String): Boolean = lineBreak.matches(html.trim())

    fun images(html: String): List<MdImage> =
        imgTag.findAll(removeHidden(html)).mapNotNull { match ->
            attribute(match.value, "src")?.let { src ->
                MdImage(url = decodeEntities(src), alt = decodeEntities(attribute(match.value, "alt").orEmpty()))
            }
        }.toList()

    fun strip(html: String): String {
        val withBreaks = breakTag.replace(removeHidden(html)) { "\n" }
        val text = tag.replace(withBreaks, "")
        return decodeEntities(text)
            .replace(spaces, " ")
            .replace(blankLines, "\n")
            .trim()
    }

    private fun removeHidden(html: String): String = hiddenElement.replace(comment.replace(html, ""), "")

    private fun attribute(tagText: String, name: String): String? {
        val pattern = Regex("\\b$name\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE)
        val match = pattern.find(tagText) ?: return null
        return match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }
    }

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&")
}
