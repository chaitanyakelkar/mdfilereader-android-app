package io.github.chaitanyakelkar.mdreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import io.github.chaitanyakelkar.mdreader.R
import io.github.chaitanyakelkar.mdreader.data.ReaderFont
import io.github.chaitanyakelkar.mdreader.data.ReaderSettings
import io.github.chaitanyakelkar.mdreader.markdown.ColumnAlign
import io.github.chaitanyakelkar.mdreader.markdown.LinkPolicy
import io.github.chaitanyakelkar.mdreader.markdown.MdBlock
import io.github.chaitanyakelkar.mdreader.markdown.MdImage
import io.github.chaitanyakelkar.mdreader.markdown.MdInline
import io.github.chaitanyakelkar.mdreader.markdown.MdListItem
import io.github.chaitanyakelkar.mdreader.ui.theme.ReaderPalette
import io.github.chaitanyakelkar.mdreader.ui.theme.toColor

/** Everything block rendering needs that does not change between blocks. */
@Immutable
data class RenderContext(
    val palette: ReaderPalette,
    val settings: ReaderSettings,
    val onLink: (String) -> Unit,
    val onCopy: (String) -> Unit,
) {
    private val family: FontFamily =
        if (settings.font == ReaderFont.SERIF) FontFamily.Serif else FontFamily.Default

    val body = sized(16, 26)

    val code = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp * settings.textScale,
        lineHeight = 21.sp * settings.textScale,
    )

    fun heading(level: Int): TextStyle = when (level) {
        1 -> sized(28, 36, FontWeight.Bold)
        2 -> sized(24, 32, FontWeight.Bold)
        3 -> sized(20, 28, FontWeight.SemiBold)
        4 -> sized(18, 26, FontWeight.SemiBold)
        5 -> sized(16, 24, FontWeight.SemiBold)
        else -> sized(14, 22, FontWeight.SemiBold).copy(color = palette.muted.toColor())
    }

    private fun sized(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = family,
        fontSize = size.sp * settings.textScale,
        lineHeight = line.sp * settings.textScale,
        fontWeight = weight,
    )
}

/** Find-in-document highlight for the block being composed. */
@Immutable
data class FindHighlight(val query: String = "", val color: Color = Color.Unspecified)

val LocalFindHighlight = compositionLocalOf { FindHighlight() }

@Composable
fun MarkdownBlock(block: MdBlock, ctx: RenderContext, modifier: Modifier = Modifier, listDepth: Int = 0) {
    when (block) {
        is MdBlock.Heading -> HeadingBlock(block, ctx, modifier)
        is MdBlock.Paragraph -> InlineTextView(block.inlines, ctx.body, ctx, modifier)
        is MdBlock.Images -> ImagesBlock(block.images, ctx, modifier)
        is MdBlock.CodeBlock -> CodeBlockView(block, ctx, modifier)
        is MdBlock.Quote -> QuoteBlock(block, ctx, modifier, listDepth)
        is MdBlock.ListBlock -> ListBlockView(block, ctx, modifier, listDepth)
        is MdBlock.Table -> TableBlockView(block, ctx, modifier)
        is MdBlock.FrontMatter -> FrontMatterBlock(block, ctx, modifier)
        is MdBlock.Footnote -> FootnoteBlock(block, ctx, modifier)
        MdBlock.Rule -> HorizontalDivider(modifier.padding(vertical = 8.dp), color = ctx.palette.divider.toColor())
    }
}

@Composable
private fun BlockColumn(blocks: List<MdBlock>, ctx: RenderContext, listDepth: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { MarkdownBlock(it, ctx, listDepth = listDepth) }
    }
}

@Composable
private fun InlineTextView(
    inlines: List<MdInline>,
    style: TextStyle,
    ctx: RenderContext,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    val highlight = LocalFindHighlight.current
    val text = remember(inlines, ctx.palette, ctx.onLink, highlight) {
        buildInlineText(inlines, ctx.palette, ctx.onLink).withFindHighlight(highlight.query, highlight.color)
    }
    Text(text, modifier, style = style, textAlign = textAlign ?: TextAlign.Unspecified)
}

@Composable
private fun HeadingBlock(block: MdBlock.Heading, ctx: RenderContext, modifier: Modifier) {
    Column(modifier.padding(top = if (block.level <= 3) 16.dp else 8.dp)) {
        InlineTextView(block.inlines, ctx.heading(block.level), ctx, Modifier.semantics { heading() })
        if (block.level <= 2) {
            HorizontalDivider(Modifier.padding(top = 6.dp), color = ctx.palette.divider.toColor())
        }
    }
}

@Composable
private fun CodeBlockView(block: MdBlock.CodeBlock, ctx: RenderContext, modifier: Modifier) {
    val highlight = LocalFindHighlight.current
    val text = remember(block, ctx.palette, highlight) {
        buildCodeText(block.code, block.language, ctx.palette).withFindHighlight(highlight.query, highlight.color)
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ctx.palette.codeBackground.toColor()),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                block.language.orEmpty(),
                Modifier.weight(1f),
                color = ctx.palette.muted.toColor(),
                fontSize = 12.sp,
            )
            IconButton(onClick = { ctx.onCopy(block.code) }) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.copy_code),
                    Modifier.size(18.dp),
                    tint = ctx.palette.muted.toColor(),
                )
            }
        }
        Text(
            text,
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            style = ctx.code,
            softWrap = false,
        )
    }
}

@Composable
private fun QuoteBlock(block: MdBlock.Quote, ctx: RenderContext, modifier: Modifier, listDepth: Int) {
    val barColor = ctx.palette.accent.toColor()
    CompositionLocalProvider(LocalContentColor provides ctx.palette.muted.toColor()) {
        BlockColumn(
            block.blocks,
            ctx,
            listDepth,
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        barColor,
                        Offset(1.5.dp.toPx(), 0f),
                        Offset(1.5.dp.toPx(), size.height),
                        strokeWidth = 3.dp.toPx(),
                    )
                }
                .padding(start = 15.dp),
        )
    }
}

@Composable
private fun ListBlockView(block: MdBlock.ListBlock, ctx: RenderContext, modifier: Modifier, listDepth: Int) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        block.items.forEachIndexed { index, item ->
            ListItemRow(item, block, index, ctx, listDepth)
        }
    }
}

@Composable
private fun ListItemRow(item: MdListItem, list: MdBlock.ListBlock, index: Int, ctx: RenderContext, listDepth: Int) {
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.widthIn(min = 24.dp).padding(end = 6.dp)) {
            when {
                item.checked != null -> {
                    val description = stringResource(if (item.checked) R.string.task_done else R.string.task_todo)
                    Icon(
                        if (item.checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                        contentDescription = description,
                        Modifier.padding(top = 3.dp).size(20.dp),
                        tint = if (item.checked) ctx.palette.accent.toColor() else ctx.palette.muted.toColor(),
                    )
                }
                list.ordered -> Text("${list.start + index}.", style = ctx.body)
                else -> Text(BULLETS[listDepth.coerceAtMost(BULLETS.lastIndex)], style = ctx.body)
            }
        }
        BlockColumn(item.blocks, ctx, listDepth + 1, Modifier.weight(1f))
    }
}

@Composable
private fun TableBlockView(block: MdBlock.Table, ctx: RenderContext, modifier: Modifier) {
    val columns = block.header.size.coerceAtLeast(1)
    val line = ctx.palette.divider.toColor()
    val headerStyle = ctx.body.copy(fontWeight = FontWeight.SemiBold, fontSize = ctx.body.fontSize * 0.9f)
    val cellStyle = ctx.body.copy(fontSize = ctx.body.fontSize * 0.9f)
    Box(modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        TableGrid(columns) {
            block.header.forEachIndexed { column, cell ->
                TableCellView(cell, headerStyle, ctx, block.aligns.getOrNull(column), line, ctx.palette.codeBackground.toColor())
            }
            block.rows.forEach { row ->
                repeat(columns) { column ->
                    TableCellView(row.getOrElse(column) { emptyList() }, cellStyle, ctx, block.aligns.getOrNull(column), line, Color.Transparent)
                }
            }
        }
    }
}

@Composable
private fun TableCellView(
    inlines: List<MdInline>,
    style: TextStyle,
    ctx: RenderContext,
    align: ColumnAlign?,
    line: Color,
    background: Color,
) {
    Box(
        Modifier
            .background(background)
            .border(0.5.dp, line)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        InlineTextView(
            inlines,
            style,
            ctx,
            Modifier.fillMaxWidth(),
            textAlign = when (align) {
                ColumnAlign.CENTER -> TextAlign.Center
                ColumnAlign.END -> TextAlign.End
                else -> TextAlign.Start
            },
        )
    }
}

/** Grid where each column fits its widest cell, up to a maximum, and each row its tallest. */
@Composable
private fun TableGrid(columns: Int, content: @Composable () -> Unit) {
    Layout(content) { measurables, _ ->
        val minWidth = 56.dp.roundToPx()
        val maxWidth = 280.dp.roundToPx()
        val widths = IntArray(columns) { minWidth }
        measurables.forEachIndexed { index, cell ->
            val column = index % columns
            widths[column] = maxOf(widths[column], cell.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(maxWidth))
        }
        val rowCount = (measurables.size + columns - 1) / columns
        val heights = IntArray(rowCount)
        measurables.forEachIndexed { index, cell ->
            val row = index / columns
            heights[row] = maxOf(heights[row], cell.maxIntrinsicHeight(widths[index % columns]))
        }
        val placeables = measurables.mapIndexed { index, cell ->
            cell.measure(Constraints.fixed(widths[index % columns], heights[index / columns]))
        }
        layout(widths.sum(), heights.sum()) {
            var y = 0
            var x = 0
            placeables.forEachIndexed { index, placeable ->
                val column = index % columns
                if (column == 0) x = 0
                placeable.place(x, y)
                x += widths[column]
                if (column == columns - 1) y += heights[index / columns]
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImagesBlock(images: List<MdImage>, ctx: RenderContext, modifier: Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = maxWidth
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            images.forEach { ImageView(it, ctx, available) }
        }
    }
}

@Composable
private fun ImageView(image: MdImage, ctx: RenderContext, available: Dp) {
    var failed by remember(image.url) { mutableStateOf(false) }
    // Like a browser, one image pixel is one dp, and larger images shrink to fit. This stops
    // badges and icons from being stretched to the full width of the page.
    var natural by remember(image.url) { mutableStateOf<IntSize?>(null) }
    val context = LocalContext.current
    val density = LocalDensity.current
    // The first load discovers the natural size. Once known, load at the size actually shown,
    // so that vector badges stay sharp and very large photos use less memory.
    val request = remember(image.url, natural, available) {
        val shownWidth = natural?.let { with(density) { minOf(it.width.dp, available).roundToPx() } }
        val builder = ImageRequest.Builder(context).data(image.url)
        if (shownWidth != null && shownWidth > 0) {
            val shownHeight = natural!!.height * shownWidth / natural!!.width.coerceAtLeast(1)
            builder.size(shownWidth, shownHeight.coerceAtLeast(1))
        } else {
            builder.size(CoilSize.ORIGINAL)
        }
        builder.build()
    }
    val loadable = LinkPolicy.isLoadableImage(image.url)
    when {
        !loadable -> ImagePlaceholder(image, stringResource(R.string.image_local_unavailable), ctx)
        !ctx.settings.loadRemoteImages -> ImagePlaceholder(image, stringResource(R.string.image_remote_off), ctx)
        failed -> ImagePlaceholder(image, stringResource(R.string.image_failed), ctx)
        else -> {
            val linkModifier = image.linkUrl?.let { url -> Modifier.clickable { ctx.onLink(url) } } ?: Modifier
            val sizeModifier = natural?.takeIf { it.width > 0 && it.height > 0 }?.let { size ->
                Modifier
                    .width(minOf(size.width.dp, available))
                    .aspectRatio(size.width.toFloat() / size.height)
            } ?: Modifier
            AsyncImage(
                model = request,
                contentDescription = image.alt.ifEmpty { null },
                modifier = sizeModifier.then(linkModifier).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Fit,
                onSuccess = {
                    if (natural == null) natural = IntSize(it.result.image.width, it.result.image.height)
                },
                onError = { failed = true },
            )
        }
    }
}

@Composable
private fun ImagePlaceholder(image: MdImage, reason: String, ctx: RenderContext) {
    val linkModifier = image.linkUrl?.let { url -> Modifier.clickable { ctx.onLink(url) } } ?: Modifier
    Row(
        linkModifier
            .clip(RoundedCornerShape(8.dp))
            .background(ctx.palette.codeBackground.toColor())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.BrokenImage, contentDescription = null, Modifier.size(18.dp), tint = ctx.palette.muted.toColor())
        Spacer(Modifier.size(8.dp))
        Column {
            if (image.alt.isNotEmpty()) Text(image.alt, fontSize = 14.sp)
            Text(reason, fontSize = 12.sp, color = ctx.palette.muted.toColor())
        }
    }
}

@Composable
private fun FrontMatterBlock(block: MdBlock.FrontMatter, ctx: RenderContext, modifier: Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ctx.palette.codeBackground.toColor()),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.properties), Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (expanded) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                block.entries.forEach { (key, value) ->
                    Text(key, fontSize = 12.sp, color = ctx.palette.muted.toColor())
                    Text(value, Modifier.padding(bottom = 6.dp), style = ctx.body)
                }
            }
        }
    }
}

@Composable
private fun FootnoteBlock(block: MdBlock.Footnote, ctx: RenderContext, modifier: Modifier) {
    Row(modifier.fillMaxWidth()) {
        Text("${block.number}.", Modifier.widthIn(min = 28.dp), style = ctx.body, color = ctx.palette.muted.toColor())
        BlockColumn(block.blocks, ctx, 0, Modifier.weight(1f))
    }
}

private val BULLETS = listOf("•", "◦", "▪")
