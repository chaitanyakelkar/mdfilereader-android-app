package io.github.chaitanyakelkar.mdreader.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    private val parser = MarkdownParser()

    private fun blocks(markdown: String) = parser.parse(markdown).blocks

    private inline fun <reified T : MdBlock> single(markdown: String): T {
        val all = blocks(markdown)
        assertEquals("expected one block, got $all", 1, all.size)
        return all[0] as T
    }

    @Test fun `empty input has no blocks`() {
        assertTrue(blocks("").isEmpty())
        assertTrue(blocks("\n\n   \n").isEmpty())
    }

    @Test fun `headings keep level and get anchors`() {
        val all = blocks("# One\n\n### Three")
        assertEquals(MdBlock.Heading(1, listOf(MdInline.Text("One")), "one"), all[0])
        assertEquals(3, (all[1] as MdBlock.Heading).level)
    }

    @Test fun `setext headings parse`() {
        assertEquals(2, single<MdBlock.Heading>("Title\n---").level)
    }

    @Test fun `duplicate headings get unique anchors`() {
        val anchors = blocks("# A\n\n# A\n\n# A").map { (it as MdBlock.Heading).anchor }
        assertEquals(listOf("a", "a-1", "a-2"), anchors)
    }

    @Test fun `inline styles nest`() {
        val paragraph = single<MdBlock.Paragraph>("a **b *c*** ~~d~~ `e`")
        assertTrue(paragraph.inlines.any { it is MdInline.Bold })
        assertTrue(paragraph.inlines.any { it is MdInline.Strike })
        assertTrue(paragraph.inlines.contains(MdInline.Code("e")))
    }

    @Test fun `soft breaks become spaces and hard breaks are kept`() {
        assertEquals(listOf(MdInline.Text("a b")), single<MdBlock.Paragraph>("a\nb").inlines)
        assertTrue(single<MdBlock.Paragraph>("a  \nb").inlines.contains(MdInline.LineBreak))
    }

    @Test fun `links and autolinks parse`() {
        val explicit = single<MdBlock.Paragraph>("[text](https://a.example)").inlines[0] as MdInline.Link
        assertEquals("https://a.example", explicit.url)
        val auto = single<MdBlock.Paragraph>("see https://b.example now").inlines[1] as MdInline.Link
        assertEquals("https://b.example", auto.url)
    }

    @Test fun `fenced code keeps language and exact whitespace`() {
        val code = single<MdBlock.CodeBlock>("```kotlin title=x\n  val a = 1\n\n  # not heading\n```")
        assertEquals("kotlin", code.language)
        assertEquals("  val a = 1\n\n  # not heading", code.code)
    }

    @Test fun `indented code has no language`() {
        val code = single<MdBlock.CodeBlock>("    code here")
        assertNull(code.language)
        assertEquals("code here", code.code)
    }

    @Test fun `gfm table keeps alignment and cell formatting`() {
        val table = single<MdBlock.Table>(
            """
            | L | C | R | D |
            |:--|:-:|--:|---|
            | **a** | `b` | c | d |
            | 1 | 2 | 3 | 4 |
            """.trimIndent(),
        )
        assertEquals(listOf(ColumnAlign.START, ColumnAlign.CENTER, ColumnAlign.END, ColumnAlign.START), table.aligns)
        assertEquals(4, table.header.size)
        assertEquals(2, table.rows.size)
        assertTrue(table.rows[0][0][0] is MdInline.Bold)
        assertEquals(MdInline.Code("b"), table.rows[0][1][0])
    }

    @Test fun `table with a pipe inside code in a cell stays in one cell`() {
        val table = single<MdBlock.Table>("| a |\n|---|\n| `x\\|y` |")
        assertEquals(1, table.rows[0].size)
    }

    @Test fun `task lists report state and plain items do not`() {
        val list = single<MdBlock.ListBlock>("- [x] done\n- [ ] todo\n- plain")
        assertEquals(listOf(true, false, null), list.items.map { it.checked })
        assertEquals("done", list.items[0].blocks.single().searchableText().trim())
    }

    @Test fun `ordered lists keep their start number`() {
        val list = single<MdBlock.ListBlock>("7. a\n8. b")
        assertTrue(list.ordered)
        assertEquals(7, list.start)
    }

    @Test fun `lists nest`() {
        val list = single<MdBlock.ListBlock>("- a\n  - b\n    - c")
        val inner = list.items[0].blocks[1] as MdBlock.ListBlock
        assertTrue(inner.items[0].blocks[1] is MdBlock.ListBlock)
    }

    @Test fun `quotes hold blocks`() {
        val quote = single<MdBlock.Quote>("> # h\n>\n> text")
        assertTrue(quote.blocks[0] is MdBlock.Heading)
        assertTrue(quote.blocks[1] is MdBlock.Paragraph)
    }

    @Test fun `thematic break parses`() {
        assertEquals(MdBlock.Rule, blocks("---")[0])
    }

    @Test fun `document starting with a rule keeps its content`() {
        listOf(
            "---\n\nBody text\n\n# Heading",
            "---\nBody text\n\n# Heading",
            "---\nBody text\n---\n\n# Heading",
            "---\n\nBody text\n\n---\n\n# Heading",
        ).forEach { markdown ->
            val text = blocks(markdown).joinToString(" ") { it.searchableText() }
            assertTrue("lost content in: $markdown -> $text", text.contains("Body text") && text.contains("Heading"))
        }
    }

    @Test fun `front matter that is never closed does not hide the document`() {
        val text = blocks("---\ntitle: x\n\nBody text").joinToString(" ") { it.searchableText() }
        assertTrue(text, text.contains("Body text"))
    }

    @Test fun `standalone image becomes an image block`() {
        val images = single<MdBlock.Images>("![alt text](https://x.example/a.png)")
        assertEquals(MdImage("https://x.example/a.png", "alt text"), images.images.single())
    }

    @Test fun `adjacent linked badges group into one block`() {
        val images = single<MdBlock.Images>("[![a](https://i/a)](https://l/a) [![b](https://i/b)](https://l/b)")
        assertEquals(listOf("https://l/a", "https://l/b"), images.images.map { it.linkUrl })
    }

    @Test fun `image inside text splits the paragraph in order`() {
        val all = blocks("before ![i](https://x/i.png) after")
        assertEquals(3, all.size)
        assertTrue(all[0] is MdBlock.Paragraph)
        assertTrue(all[1] is MdBlock.Images)
        assertTrue(all[2] is MdBlock.Paragraph)
    }

    @Test fun `front matter is parsed and not shown as text`() {
        val all = blocks("---\ntitle: Hello\ntags:\n  - a\n  - b\n---\n\nBody")
        val matter = all[0] as MdBlock.FrontMatter
        assertEquals("title" to "Hello", matter.entries[0])
        assertEquals("tags" to "a, b", matter.entries[1])
        assertEquals(listOf(MdInline.Text("Body")), (all[1] as MdBlock.Paragraph).inlines)
    }

    @Test fun `footnotes number by first use and move to the end`() {
        val doc = parser.parse("Text[^b] and[^a].\n\n[^a]: Note A\n[^b]: Note B\n\nMore")
        val paragraph = doc.blocks[0] as MdBlock.Paragraph
        assertEquals(MdInline.FootnoteRef("b", 1), paragraph.inlines[1])
        assertEquals(MdBlock.Rule, doc.blocks[doc.blocks.size - 3])
        val notes = doc.blocks.filterIsInstance<MdBlock.Footnote>()
        assertEquals(listOf(1, 2), notes.map { it.number })
        assertTrue(notes[0].blocks.single().searchableText().contains("Note B"))
        assertNotNull(doc.blockIndexForAnchor("fn-b"))
    }

    @Test fun `unused footnote is not rendered`() {
        val doc = parser.parse("Text\n\n[^a]: Never referenced")
        assertTrue(doc.blocks.none { it is MdBlock.Footnote })
    }

    @Test fun `html block extracts images and text and drops scripts and comments`() {
        val all = blocks(
            """
            <div align="center">
              <img src="https://x/logo.png" alt="Logo &amp; name" width="100">
              <p>Hello &lt;world&gt;</p>
              <!-- hidden note -->
              <script>alert(1)</script>
            </div>
            """.trimIndent(),
        )
        val images = all.filterIsInstance<MdBlock.Images>().single()
        assertEquals("Logo & name", images.images.single().alt)
        val text = all.filterIsInstance<MdBlock.Paragraph>().single().searchableText()
        assertEquals("Hello <world>", text)
    }

    @Test fun `inline br becomes a line break and other tags vanish`() {
        val inlines = single<MdBlock.Paragraph>("a<br>b <kbd>c</kbd>").inlines
        assertTrue(inlines.contains(MdInline.LineBreak))
        assertEquals("a\nb c", inlines.plainText())
    }

    @Test fun `extreme nesting does not overflow the stack`() {
        val quotes = parser.parse(">".repeat(20_000) + " deep")
        assertFalse(quotes.blocks.isEmpty())
        assertTrue(quotes.blocks.single().searchableText().contains("deep"))

        val lists = (0 until 2_000).joinToString("\n") { "  ".repeat(it) + "- item$it" }
        assertFalse(parser.parse(lists).blocks.isEmpty())
    }

    @Test fun `unterminated constructs do not throw`() {
        listOf("```kotlin\nno end", "| a |\n|---", "[link](", "**bold", "<div>", "[^x]", "![](", "- [").forEach {
            parser.parse(it)
        }
    }

    @Test fun `toc lists headings with their block index`() {
        val doc = parser.parse("intro\n\n# **Bold** title\n\ntext\n\n## `code` two")
        assertEquals(
            listOf(TocEntry(1, "Bold title", 1), TocEntry(2, "code two", 3)),
            doc.toc,
        )
    }

    @Test fun `anchors resolve to blocks, including non-ascii headings`() {
        val doc = parser.parse("# Getting Started!\n\ntext\n\n## Überblick & Plan")
        assertEquals(0, doc.blockIndexForAnchor("getting-started"))
        assertEquals(2, doc.blockIndexForAnchor("überblick--plan"))
        assertEquals(0, doc.blockIndexForAnchor("Getting-Started"))
        assertNull(doc.blockIndexForAnchor("missing"))
    }

    @Test fun `no image placeholder leaks out of paragraphs`() {
        val doc = parser.parse("**bold ![x](https://a/b.png)** [t ![y](https://a/c.png)](https://l) ![z](https://a/d.png) e")
        fun check(inlines: List<MdInline>) {
            inlines.forEach { inline ->
                assertFalse(inline is MdInline.ImageRef)
                when (inline) {
                    is MdInline.Bold -> check(inline.children)
                    is MdInline.Link -> check(inline.children)
                    else -> Unit
                }
            }
        }
        doc.blocks.filterIsInstance<MdBlock.Paragraph>().forEach { check(it.inlines) }
    }
}
