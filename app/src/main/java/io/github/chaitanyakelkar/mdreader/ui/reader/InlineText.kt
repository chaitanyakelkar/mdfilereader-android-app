package io.github.chaitanyakelkar.mdreader.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import io.github.chaitanyakelkar.mdreader.markdown.CodeHighlighter
import io.github.chaitanyakelkar.mdreader.markdown.CodeToken
import io.github.chaitanyakelkar.mdreader.markdown.MarkdownParser
import io.github.chaitanyakelkar.mdreader.markdown.MdInline
import io.github.chaitanyakelkar.mdreader.markdown.TokenType
import io.github.chaitanyakelkar.mdreader.ui.theme.ReaderPalette
import io.github.chaitanyakelkar.mdreader.ui.theme.toColor

/** Builds styled text with tappable links from inline Markdown. */
internal fun buildInlineText(
    inlines: List<MdInline>,
    palette: ReaderPalette,
    onLink: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    fun AnnotatedString.Builder.append(items: List<MdInline>) {
        for (inline in items) {
            when (inline) {
                is MdInline.Text -> append(inline.text)
                is MdInline.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inline.children) }
                is MdInline.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(inline.children) }
                is MdInline.Strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(inline.children)
                }
                is MdInline.Code -> withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 0.88.em,
                        background = palette.codeBackground.toColor(),
                    ),
                ) { append(inline.code) }
                is MdInline.Link -> withLink(linkAnnotation(inline.url, palette, onLink)) { append(inline.children) }
                is MdInline.FootnoteRef -> {
                    val target = "#" + MarkdownParser.footnoteAnchor(inline.label)
                    withLink(linkAnnotation(target, palette, onLink)) {
                        withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em)) {
                            append("[${inline.number}]")
                        }
                    }
                }
                is MdInline.ImageRef -> append(inline.image.alt)
                MdInline.LineBreak -> append('\n')
            }
        }
    }
    append(inlines)
}

private fun linkAnnotation(url: String, palette: ReaderPalette, onLink: (String) -> Unit) =
    LinkAnnotation.Clickable(
        tag = url,
        styles = TextLinkStyles(
            SpanStyle(color = palette.accent.toColor(), textDecoration = TextDecoration.Underline),
        ),
        linkInteractionListener = { onLink(url) },
    )

internal fun buildCodeText(code: String, language: String?, palette: ReaderPalette): AnnotatedString =
    buildAnnotatedString {
        append(code)
        CodeHighlighter.tokenize(code, language).forEach { token: CodeToken ->
            val color = when (token.type) {
                TokenType.KEYWORD -> palette.codeKeyword
                TokenType.STRING -> palette.codeString
                TokenType.COMMENT -> palette.codeComment
                TokenType.NUMBER -> palette.codeNumber
            }.toColor()
            addStyle(SpanStyle(color = color), token.start, token.end)
        }
    }

/** Adds a background behind each occurrence of [query]. */
internal fun AnnotatedString.withFindHighlight(query: String, color: Color): AnnotatedString {
    if (query.isEmpty() || !text.contains(query, ignoreCase = true)) return this
    return buildAnnotatedString {
        append(this@withFindHighlight)
        var start = text.indexOf(query, ignoreCase = true)
        while (start >= 0) {
            addStyle(SpanStyle(background = color), start, start + query.length)
            start = text.indexOf(query, start + query.length, ignoreCase = true)
        }
    }
}
