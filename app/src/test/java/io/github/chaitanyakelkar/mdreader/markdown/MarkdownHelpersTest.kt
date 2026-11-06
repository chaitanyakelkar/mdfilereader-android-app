package io.github.chaitanyakelkar.mdreader.markdown

import io.github.chaitanyakelkar.mdreader.markdown.LinkPolicy.LinkAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkPolicyTest {
    @Test fun `web and mail links open externally`() {
        listOf("https://a.example", "http://a.example", "HTTPS://A.example", "mailto:x@a.example").forEach {
            assertEquals(LinkAction.External(it), LinkPolicy.classifyLink(it))
        }
    }

    @Test fun `dangerous schemes are blocked`() {
        listOf(
            "javascript:alert(1)", "JaVaScRiPt:alert(1)", "  javascript:alert(1)", "file:///data/secret",
            "content://x/y", "intent://x#Intent;end", "data:text/html,<script>", "tel:123", "//evil.example/x",
        ).forEach {
            assertEquals(it, LinkAction.Blocked, LinkPolicy.classifyLink(it))
        }
    }

    @Test fun `fragments are anchors and paths are relative`() {
        assertEquals(LinkAction.Anchor("intro"), LinkPolicy.classifyLink("#intro"))
        assertEquals(LinkAction.Relative, LinkPolicy.classifyLink("docs/other.md"))
        assertEquals(LinkAction.Relative, LinkPolicy.classifyLink("../up.md#part"))
    }

    @Test fun `only http images load`() {
        assertTrue(LinkPolicy.isLoadableImage("https://x/a.png"))
        assertTrue(LinkPolicy.isLoadableImage("http://x/a.png"))
        listOf("images/a.png", "file:///sdcard/a.png", "content://x/a", "data:image/png;base64,AA", "/a.png").forEach {
            assertFalse(it, LinkPolicy.isLoadableImage(it))
        }
    }
}

class HeadingSluggerTest {
    @Test fun `matches github style`() {
        val slugger = HeadingSlugger()
        assertEquals("hello-world", slugger.slug("Hello World"))
        assertEquals("whats-new-in-v20", slugger.slug("What's new in v2.0?"))
        assertEquals("a--b", slugger.slug("A & B"))
        assertEquals("snake_case-and-dash", slugger.slug("snake_case and-dash"))
        assertEquals("hello-world-1", slugger.slug("Hello World"))
    }
}

class DocumentSearchTest {
    @Test fun `finds all occurrences case-insensitively with block indexes`() {
        val matches = DocumentSearch.find(listOf("Apple apple", "none", "APPLE"), "apple")
        assertEquals(
            listOf(SearchMatch(0, 0), SearchMatch(0, 6), SearchMatch(2, 0)),
            matches,
        )
    }

    @Test fun `empty query and no match return nothing`() {
        assertTrue(DocumentSearch.find(listOf("abc"), "").isEmpty())
        assertTrue(DocumentSearch.find(listOf("abc"), "z").isEmpty())
    }

    @Test fun `overlapping text does not loop or double count`() {
        assertEquals(2, DocumentSearch.find(listOf("aaaa"), "aa").size)
    }

    @Test fun `search text covers tables lists and quotes`() {
        val doc = MarkdownParser().parse(
            "| head |\n|---|\n| cellword |\n\n- listword\n\n> quoteword\n\n```\ncodeword\n```\n\n[linkword](https://a)",
        )
        val texts = doc.blocks.map { it.searchableText() }
        listOf("head", "cellword", "listword", "quoteword", "codeword", "linkword").forEach {
            assertEquals(it, 1, DocumentSearch.find(texts, it).size)
        }
    }
}

class CodeHighlighterTest {
    private fun types(code: String, language: String?) =
        CodeHighlighter.tokenize(code, language).map { it.type to code.substring(it.start, it.end) }

    @Test fun `unknown language is left uncolored`() {
        assertTrue(CodeHighlighter.tokenize("val x = 1", "brainfuck").isEmpty())
        assertTrue(CodeHighlighter.tokenize("val x = 1", null).isEmpty())
    }

    @Test fun `kotlin tokens`() {
        assertEquals(
            listOf(
                TokenType.KEYWORD to "val",
                TokenType.NUMBER to "12",
                TokenType.COMMENT to "// val note",
            ),
            types("val value2 = 12 // val note", "kotlin"),
        )
    }

    @Test fun `keywords inside strings are not colored and escapes are handled`() {
        assertEquals(listOf(TokenType.STRING to "\"if \\\" else\""), types("\"if \\\" else\"", "java"))
    }

    @Test fun `unterminated string stops at the line end`() {
        assertEquals(
            listOf(TokenType.STRING to "\"open", TokenType.KEYWORD to "val"),
            types("\"open\nval", "kotlin"),
        )
    }

    @Test fun `block comment spans lines and may be unterminated`() {
        assertEquals(listOf(TokenType.COMMENT to "/* a\nb */"), types("/* a\nb */", "js"))
        assertEquals(listOf(TokenType.COMMENT to "/* open"), types("/* open", "js"))
    }

    @Test fun `python and shell comments use a hash`() {
        assertEquals(TokenType.COMMENT to "# x", types("# x", "py").single())
        assertEquals(TokenType.COMMENT to "# x", types("# x", "bash").single())
    }

    @Test fun `sql keywords ignore case and language name ignores case`() {
        assertEquals(TokenType.KEYWORD to "SELECT", types("SELECT", "SQL").single())
    }

    @Test fun `tokens never overlap or exceed bounds`() {
        val code = "fun a() { /* c */ val s = \"x\" + 'y' + `z` // end\n}"
        var previousEnd = 0
        CodeHighlighter.tokenize(code, "kotlin").forEach {
            assertTrue(it.start >= previousEnd && it.end <= code.length && it.end > it.start)
            previousEnd = it.end
        }
    }
}
