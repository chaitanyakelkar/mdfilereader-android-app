package io.github.chaitanyakelkar.mdreader.markdown

/** Decides what may happen with a URL found in an untrusted document. */
object LinkPolicy {
    sealed interface LinkAction {
        data class External(val url: String) : LinkAction
        data class Anchor(val fragment: String) : LinkAction
        data object Relative : LinkAction
        data object Blocked : LinkAction
    }

    private val schemePattern = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*):")
    private val allowedLinkSchemes = setOf("http", "https", "mailto")

    fun classifyLink(rawUrl: String): LinkAction {
        val url = rawUrl.trim()
        if (url.startsWith("#")) return LinkAction.Anchor(url.drop(1))
        val scheme = schemePattern.find(url)?.groupValues?.get(1)?.lowercase()
        return when {
            scheme == null -> if (url.startsWith("//")) LinkAction.Blocked else LinkAction.Relative
            scheme in allowedLinkSchemes -> LinkAction.External(url)
            else -> LinkAction.Blocked
        }
    }

    /** Only remote images load, which stops a document from reading local files by path. */
    fun isLoadableImage(rawUrl: String): Boolean {
        val scheme = schemePattern.find(rawUrl.trim())?.groupValues?.get(1)?.lowercase()
        return scheme == "http" || scheme == "https"
    }
}
