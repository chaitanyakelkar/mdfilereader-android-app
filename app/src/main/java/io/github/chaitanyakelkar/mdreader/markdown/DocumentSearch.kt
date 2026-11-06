package io.github.chaitanyakelkar.mdreader.markdown

data class SearchMatch(val blockIndex: Int, val start: Int)

object DocumentSearch {
    /** Case-insensitive search over per-block text. Matches never span blocks. */
    fun find(texts: List<String>, query: String): List<SearchMatch> {
        if (query.isEmpty()) return emptyList()
        val matches = ArrayList<SearchMatch>()
        texts.forEachIndexed { blockIndex, text ->
            var start = text.indexOf(query, ignoreCase = true)
            while (start >= 0) {
                matches += SearchMatch(blockIndex, start)
                start = text.indexOf(query, start + query.length, ignoreCase = true)
            }
        }
        return matches
    }
}
