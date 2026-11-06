package io.github.chaitanyakelkar.mdreader.markdown

/** Generates GitHub-compatible heading anchors, so `[x](#some-heading)` links resolve. */
class HeadingSlugger {
    private val seen = HashMap<String, Int>()

    fun slug(title: String): String {
        val base = title.trim().lowercase()
            .filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
            .replace(' ', '-')
        val count = seen[base]
        seen[base] = (count ?: 0) + 1
        return if (count == null) base else "$base-$count"
    }
}
