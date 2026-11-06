package io.github.chaitanyakelkar.mdreader.markdown

/** Inline content. Plain Kotlin, independent of Compose and of the parser library. */
sealed interface MdInline {
    data class Text(val text: String) : MdInline
    data class Bold(val children: List<MdInline>) : MdInline
    data class Italic(val children: List<MdInline>) : MdInline
    data class Strike(val children: List<MdInline>) : MdInline
    data class Code(val code: String) : MdInline
    data class Link(val url: String, val children: List<MdInline>) : MdInline
    data class FootnoteRef(val label: String, val number: Int) : MdInline
    data object LineBreak : MdInline

    /** Parser-internal: images are lifted out into [MdBlock.Images] before blocks are emitted. */
    data class ImageRef(val image: MdImage) : MdInline
}

data class MdImage(val url: String, val alt: String, val linkUrl: String? = null)

enum class ColumnAlign { START, CENTER, END }

data class MdListItem(val checked: Boolean?, val blocks: List<MdBlock>)

sealed interface MdBlock {
    data class Heading(val level: Int, val inlines: List<MdInline>, val anchor: String) : MdBlock
    data class Paragraph(val inlines: List<MdInline>) : MdBlock
    data class Images(val images: List<MdImage>) : MdBlock
    data class CodeBlock(val language: String?, val code: String) : MdBlock
    data class Quote(val blocks: List<MdBlock>) : MdBlock
    data class ListBlock(val ordered: Boolean, val start: Int, val items: List<MdListItem>) : MdBlock
    data class Table(
        val aligns: List<ColumnAlign>,
        val header: List<List<MdInline>>,
        val rows: List<List<List<MdInline>>>,
    ) : MdBlock
    data class FrontMatter(val entries: List<Pair<String, String>>) : MdBlock
    data class Footnote(val number: Int, val anchor: String, val blocks: List<MdBlock>) : MdBlock
    data object Rule : MdBlock
}

data class TocEntry(val level: Int, val title: String, val blockIndex: Int)

data class MdDocument(val blocks: List<MdBlock>) {
    val toc: List<TocEntry> = blocks.mapIndexedNotNull { index, block ->
        (block as? MdBlock.Heading)?.let { TocEntry(it.level, it.inlines.plainText(), index) }
    }

    private val anchors: Map<String, Int> = buildMap {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> putIfAbsent(block.anchor, index)
                is MdBlock.Footnote -> putIfAbsent(block.anchor, index)
                else -> Unit
            }
        }
    }

    /** Index of the top-level block that a `#fragment` link points at, if any. */
    fun blockIndexForAnchor(anchor: String): Int? = anchors[anchor] ?: anchors[anchor.lowercase()]
}

fun List<MdInline>.plainText(): String = buildString { appendPlainText(this@plainText) }

private fun StringBuilder.appendPlainText(inlines: List<MdInline>) {
    for (inline in inlines) {
        when (inline) {
            is MdInline.Text -> append(inline.text)
            is MdInline.Code -> append(inline.code)
            is MdInline.Bold -> appendPlainText(inline.children)
            is MdInline.Italic -> appendPlainText(inline.children)
            is MdInline.Strike -> appendPlainText(inline.children)
            is MdInline.Link -> appendPlainText(inline.children)
            is MdInline.FootnoteRef -> append(inline.number)
            is MdInline.ImageRef -> append(inline.image.alt)
            MdInline.LineBreak -> append('\n')
        }
    }
}

/** The text of a block as find-in-document sees it. Pieces are separated by newlines. */
fun MdBlock.searchableText(): String = when (this) {
    is MdBlock.Heading -> inlines.plainText()
    is MdBlock.Paragraph -> inlines.plainText()
    is MdBlock.Images -> images.joinToString("\n") { it.alt }
    is MdBlock.CodeBlock -> code
    is MdBlock.Quote -> blocks.joinToString("\n") { it.searchableText() }
    is MdBlock.ListBlock -> items.joinToString("\n") { item ->
        item.blocks.joinToString("\n") { it.searchableText() }
    }
    is MdBlock.Table -> (listOf(header) + rows).joinToString("\n") { row ->
        row.joinToString("\n") { it.plainText() }
    }
    is MdBlock.FrontMatter -> entries.joinToString("\n") { (key, value) -> "$key\n$value" }
    is MdBlock.Footnote -> blocks.joinToString("\n") { it.searchableText() }
    MdBlock.Rule -> ""
}
