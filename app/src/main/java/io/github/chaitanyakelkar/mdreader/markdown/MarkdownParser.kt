package io.github.chaitanyakelkar.mdreader.markdown

import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.front.matter.YamlFrontMatterNode
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

/**
 * Converts Markdown text into [MdDocument].
 *
 * Safe to call off the main thread. Nesting is capped at [MAX_DEPTH], because a file made of
 * thousands of `>` characters would otherwise overflow the stack in the recursive conversion
 * here and later in Compose.
 */
class MarkdownParser {

    private val baseExtensions = listOf(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        AutolinkExtension.create(),
        TaskListItemsExtension.create(),
        FootnotesExtension.create(),
    )

    private val parser: Parser = Parser.builder().extensions(baseExtensions).build()

    private val frontMatterParser: Parser = Parser.builder()
        .extensions(baseExtensions + YamlFrontMatterExtension.create())
        .build()

    fun parse(markdown: String): MdDocument {
        val chosen = if (hasFrontMatter(markdown)) frontMatterParser else parser
        return Conversion(chosen.parse(markdown)).run()
    }

    /**
     * The front matter extension claims any document whose first line is `---`, and swallows
     * the content when that line is really a horizontal rule. So it only runs on documents
     * with a closed block that starts with a `key:` line.
     */
    private fun hasFrontMatter(markdown: String): Boolean {
        val lines = markdown.lineSequence().toList()
        if (lines.first().trimEnd() != "---") return false
        val close = lines.drop(1).indexOfFirst { it.trimEnd() == "---" || it.trimEnd() == "..." }
        if (close < 0) return false
        return lines.subList(1, close + 1).firstOrNull { it.isNotBlank() }?.let(YAML_KEY::containsMatchIn) == true
    }

    private class Conversion(private val root: Node) {
        private val slugger = HeadingSlugger()
        private val footnoteNumbers = LinkedHashMap<String, Int>()
        private val footnoteDefinitions = HashMap<String, FootnoteDefinition>()

        fun run(): MdDocument {
            collectFootnoteDefinitions(root)
            val blocks = convertChildren(root, depth = 0).toMutableList()
            appendFootnotes(blocks)
            return MdDocument(blocks)
        }

        private fun collectFootnoteDefinitions(node: Node) {
            val stack = ArrayDeque<Node>()
            stack.addLast(node)
            while (stack.isNotEmpty()) {
                for (child in stack.removeLast().children()) {
                    if (child is FootnoteDefinition) footnoteDefinitions.putIfAbsent(child.label, child)
                    if (child is BlockQuote || child is BulletList || child is OrderedList || child is ListItem) {
                        stack.addLast(child)
                    }
                }
            }
        }

        private fun appendFootnotes(blocks: MutableList<MdBlock>) {
            val converted = mutableListOf<MdBlock.Footnote>()
            // Converting a footnote body can reference further footnotes, so walk by index.
            var position = 0
            while (position < footnoteNumbers.size) {
                val (label, number) = footnoteNumbers.entries.elementAt(position)
                footnoteDefinitions[label]?.let { definition ->
                    converted += MdBlock.Footnote(
                        number = number,
                        anchor = footnoteAnchor(label),
                        blocks = convertChildren(definition, depth = 1),
                    )
                }
                position++
            }
            if (converted.isNotEmpty()) {
                blocks += MdBlock.Rule
                blocks += converted
            }
        }

        private fun convertChildren(parent: Node, depth: Int): List<MdBlock> =
            parent.children().flatMap { convertBlock(it, depth) }

        private fun convertBlock(node: Node, depth: Int): List<MdBlock> {
            if (depth > MAX_DEPTH) {
                return listOf(MdBlock.Paragraph(listOf(MdInline.Text(flattenText(node)))))
            }
            return when (node) {
                is Heading -> {
                    val inlines = convertInlines(node)
                    listOf(MdBlock.Heading(node.level, inlines, slugger.slug(inlines.plainText())))
                }
                is Paragraph -> paragraphBlocks(node)
                is FencedCodeBlock -> listOf(
                    MdBlock.CodeBlock(
                        language = node.info?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() },
                        code = node.literal.orEmpty().removeSuffix("\n"),
                    ),
                )
                is IndentedCodeBlock -> listOf(
                    MdBlock.CodeBlock(null, node.literal.orEmpty().removeSuffix("\n")),
                )
                is BlockQuote -> listOf(MdBlock.Quote(convertChildren(node, depth + 1)))
                is BulletList -> listOf(MdBlock.ListBlock(false, 1, listItems(node, depth)))
                is OrderedList -> listOf(
                    MdBlock.ListBlock(true, node.markerStartNumber ?: 1, listItems(node, depth)),
                )
                is ThematicBreak -> listOf(MdBlock.Rule)
                is HtmlBlock -> htmlBlocks(node.literal.orEmpty())
                is TableBlock -> listOf(table(node))
                is YamlFrontMatterBlock -> frontMatter(node)
                else -> emptyList() // Includes footnote definitions, which are rendered at the end.
            }
        }

        private fun listItems(list: Node, depth: Int): List<MdListItem> =
            list.children().filterIsInstance<ListItem>().map { item ->
                MdListItem(checked = taskState(item), blocks = convertChildren(item, depth + 1))
            }

        private fun taskState(item: ListItem): Boolean? {
            for (child in item.children()) {
                if (child is TaskListItemMarker) return child.isChecked
                if (child is Paragraph) return (child.firstChild as? TaskListItemMarker)?.isChecked
            }
            return null
        }

        /** Pulls images out of the text flow so that they can be laid out as blocks. */
        private fun paragraphBlocks(paragraph: Paragraph): List<MdBlock> {
            val result = mutableListOf<MdBlock>()
            val text = mutableListOf<MdInline>()
            val images = mutableListOf<MdImage>()

            fun flushText() {
                val trimmed = text.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
                if (trimmed.isNotEmpty()) result += MdBlock.Paragraph(trimmed)
                text.clear()
            }

            fun flushImages() {
                if (images.isNotEmpty()) result += MdBlock.Images(images.toList())
                images.clear()
            }

            for (inline in convertInlines(paragraph, includeImages = true)) {
                when {
                    inline is MdInline.ImageRef -> {
                        flushText()
                        images += inline.image
                    }
                    images.isNotEmpty() && inline.isBlank() -> Unit // Space between badges.
                    else -> {
                        flushImages()
                        text += inline
                    }
                }
            }
            flushImages()
            flushText()
            return result
        }

        private fun MdInline.isBlank(): Boolean =
            this == MdInline.LineBreak || (this is MdInline.Text && text.isBlank())

        private fun htmlBlocks(html: String): List<MdBlock> {
            val result = mutableListOf<MdBlock>()
            val images = HtmlText.images(html)
            if (images.isNotEmpty()) result += MdBlock.Images(images)
            val text = HtmlText.strip(html)
            if (text.isNotEmpty()) result += MdBlock.Paragraph(listOf(MdInline.Text(text)))
            return result
        }

        private fun table(node: TableBlock): MdBlock.Table {
            var aligns = emptyList<ColumnAlign>()
            var header = emptyList<List<MdInline>>()
            val rows = mutableListOf<List<List<MdInline>>>()
            for (section in node.children()) {
                val sectionRows = section.children().filterIsInstance<TableRow>()
                when (section) {
                    is TableHead -> sectionRows.firstOrNull()?.let { row ->
                        val cells = row.children().filterIsInstance<TableCell>()
                        aligns = cells.map { it.alignment.toColumnAlign() }
                        header = cells.map { convertInlines(it) }
                    }
                    is TableBody -> sectionRows.forEach { row ->
                        rows += row.children().filterIsInstance<TableCell>().map { convertInlines(it) }
                    }
                }
            }
            return MdBlock.Table(aligns, header, rows)
        }

        private fun TableCell.Alignment?.toColumnAlign(): ColumnAlign = when (this) {
            TableCell.Alignment.CENTER -> ColumnAlign.CENTER
            TableCell.Alignment.RIGHT -> ColumnAlign.END
            else -> ColumnAlign.START
        }

        private fun frontMatter(block: YamlFrontMatterBlock): List<MdBlock> {
            val entries = block.children().filterIsInstance<YamlFrontMatterNode>().map { node ->
                node.key to node.values.joinToString(", ")
            }
            return if (entries.isEmpty()) emptyList() else listOf(MdBlock.FrontMatter(entries))
        }

        private fun convertInlines(parent: Node, includeImages: Boolean = false): List<MdInline> =
            mergeText(parent.children().flatMap { convertInline(it, includeImages) })

        private fun convertInline(node: Node, includeImages: Boolean): List<MdInline> = when (node) {
            is Text -> listOf(MdInline.Text(node.literal))
            is SoftLineBreak -> listOf(MdInline.Text(" "))
            is HardLineBreak -> listOf(MdInline.LineBreak)
            is Code -> listOf(MdInline.Code(node.literal))
            is Emphasis -> listOf(MdInline.Italic(convertInlines(node)))
            is StrongEmphasis -> listOf(MdInline.Bold(convertInlines(node)))
            is Strikethrough -> listOf(MdInline.Strike(convertInlines(node)))
            is Link -> convertLink(node, includeImages)
            is Image -> {
                val alt = flattenText(node)
                if (includeImages) listOf(MdInline.ImageRef(MdImage(node.destination, alt)))
                else listOf(MdInline.Text(alt))
            }
            is HtmlInline -> convertHtmlInline(node.literal, includeImages)
            is FootnoteReference -> {
                val number = footnoteNumbers.getOrPut(node.label) { footnoteNumbers.size + 1 }
                listOf(MdInline.FootnoteRef(node.label, number))
            }
            else -> emptyList() // Includes task list markers, handled by taskState().
        }

        private fun convertLink(link: Link, includeImages: Boolean): List<MdInline> {
            val children = link.children()
            val images = children.filterIsInstance<Image>()
            // A linked image, which is how README badges are written.
            if (includeImages && images.isNotEmpty() && images.size == children.size) {
                return images.map {
                    MdInline.ImageRef(MdImage(it.destination, flattenText(it), linkUrl = link.destination))
                }
            }
            return listOf(MdInline.Link(link.destination, convertInlines(link)))
        }

        private fun convertHtmlInline(html: String, includeImages: Boolean): List<MdInline> {
            if (HtmlText.isLineBreak(html)) return listOf(MdInline.LineBreak)
            val image = HtmlText.images(html).firstOrNull() ?: return emptyList()
            return if (includeImages) listOf(MdInline.ImageRef(image)) else listOf(MdInline.Text(image.alt))
        }

        private fun mergeText(inlines: List<MdInline>): List<MdInline> {
            val merged = ArrayList<MdInline>(inlines.size)
            for (inline in inlines) {
                val last = merged.lastOrNull()
                if (inline is MdInline.Text && last is MdInline.Text) {
                    merged[merged.lastIndex] = MdInline.Text(last.text + inline.text)
                } else {
                    merged += inline
                }
            }
            return merged
        }

        /** Iterative, because this is the fallback for over-deep nesting. */
        private fun flattenText(node: Node): String = buildString {
            val stack = ArrayDeque<Node>()
            stack.addLast(node)
            while (stack.isNotEmpty()) {
                when (val current = stack.removeLast()) {
                    is Text -> append(current.literal)
                    is Code -> append(current.literal)
                    is SoftLineBreak, is HardLineBreak -> append(' ')
                    else -> current.children().asReversed().forEach(stack::addLast)
                }
            }
        }
    }

    companion object {
        const val MAX_DEPTH = 12
        private val YAML_KEY = Regex("^[\\w.-]+\\s*:")

        fun footnoteAnchor(label: String): String = "fn-${label.lowercase()}"
    }
}

private fun Node.children(): List<Node> {
    val result = ArrayList<Node>()
    var child = firstChild
    while (child != null) {
        result += child
        child = child.next
    }
    return result
}
