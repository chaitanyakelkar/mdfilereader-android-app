package io.github.chaitanyakelkar.mdreader.ui.reader

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.chaitanyakelkar.mdreader.R
import io.github.chaitanyakelkar.mdreader.data.LoadError
import io.github.chaitanyakelkar.mdreader.data.ReaderSettings
import io.github.chaitanyakelkar.mdreader.data.SAMPLE_DOCUMENT_URI
import io.github.chaitanyakelkar.mdreader.markdown.LinkPolicy
import io.github.chaitanyakelkar.mdreader.ui.components.AppearanceActions
import io.github.chaitanyakelkar.mdreader.ui.components.AppearanceSheet
import io.github.chaitanyakelkar.mdreader.ui.components.CenteredColumn
import io.github.chaitanyakelkar.mdreader.ui.theme.LocalReaderPalette
import io.github.chaitanyakelkar.mdreader.ui.theme.toColor
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Everything the screen renders, gathered so that the UI can be tested without a ViewModel. */
data class ReaderUi(
    val state: ReaderState,
    val showSource: Boolean,
    val find: FindState,
    val settings: ReaderSettings,
)

data class ReaderActions(
    val onBack: () -> Unit,
    val onChooseFile: () -> Unit,
    val onToggleSource: () -> Unit,
    val onOpenFind: () -> Unit,
    val onCloseFind: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onNextMatch: () -> Unit,
    val onPreviousMatch: () -> Unit,
    val onSaveScroll: (Int, Int) -> Unit,
    val appearance: AppearanceActions,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(uri: String, ui: ReaderUi, actions: ReaderActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val content = ui.state as? ReaderState.Content

    var showContents by rememberSaveable { mutableStateOf(false) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = ui.find.active) { actions.onCloseFind() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (ui.find.active) {
                FindBar(ui.find, actions)
            } else {
                TopAppBar(
                    title = {
                        Text(
                            content?.name.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = actions.onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (content != null) {
                            IconButton(onClick = actions.onOpenFind) {
                                Icon(Icons.Filled.Search, stringResource(R.string.find))
                            }
                            IconButton(onClick = { showContents = true }) {
                                Icon(Icons.AutoMirrored.Filled.List, stringResource(R.string.contents))
                            }
                            OverflowMenu(
                                showSource = ui.showSource,
                                canShare = uri != SAMPLE_DOCUMENT_URI,
                                onToggleSource = actions.onToggleSource,
                                onAppearance = { showAppearance = true },
                                onShare = { shareFile(context, uri, content.name) },
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { padding ->
        when (val state = ui.state) {
            ReaderState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag("loading"))
            }
            is ReaderState.Failed -> ErrorContent(state.error, actions.onChooseFile, Modifier.padding(padding))
            is ReaderState.Content -> {
                val renderedList = rememberLazyListState(state.initialScrollIndex, state.initialScrollOffset)
                val sourceList = rememberLazyListState()
                val listState = if (ui.showSource) sourceList else renderedList
                val messages = rememberLinkMessages()

                Column(Modifier.fillMaxSize().padding(padding)) {
                    ReadingProgress(listState)
                    if (ui.showSource) {
                        SourceView(state.sourceLines, ui, listState)
                    } else {
                        RenderedView(
                            state = state,
                            ui = ui,
                            listState = listState,
                            onLink = { url ->
                                when (val action = LinkPolicy.classifyLink(url)) {
                                    is LinkPolicy.LinkAction.Anchor -> {
                                        val target = state.document.blockIndexForAnchor(Uri.decode(action.fragment))
                                        scope.launch {
                                            if (target != null) renderedList.animateScrollToItem(target)
                                            else snackbar.showSnackbar(messages.anchorMissing)
                                        }
                                    }
                                    is LinkPolicy.LinkAction.External -> if (!openExternal(context, action.url)) {
                                        scope.launch { snackbar.showSnackbar(messages.noApp) }
                                    }
                                    LinkPolicy.LinkAction.Relative ->
                                        scope.launch { snackbar.showSnackbar(messages.relative) }
                                    LinkPolicy.LinkAction.Blocked ->
                                        scope.launch { snackbar.showSnackbar(messages.blocked) }
                                }
                            },
                        )
                    }
                }

                LaunchedEffect(ui.find.current, ui.find.matches) {
                    ui.find.currentBlock?.let { listState.scrollToItem(it) }
                }
                SaveScrollEffect(renderedList, actions.onSaveScroll)

                if (showContents) {
                    ContentsSheet(
                        state = state,
                        onDismiss = { showContents = false },
                        onSelect = { blockIndex ->
                            showContents = false
                            if (ui.showSource) actions.onToggleSource()
                            scope.launch { renderedList.scrollToItem(blockIndex) }
                        },
                    )
                }
            }
        }
        if (showAppearance) {
            AppearanceSheet(ui.settings, actions.appearance, onDismiss = { showAppearance = false })
        }
    }
}

private data class LinkMessageStrings(
    val anchorMissing: String,
    val noApp: String,
    val relative: String,
    val blocked: String,
)

@Composable
private fun rememberLinkMessages() = LinkMessageStrings(
    anchorMissing = stringResource(R.string.link_anchor_missing),
    noApp = stringResource(R.string.link_no_app),
    relative = stringResource(R.string.link_relative),
    blocked = stringResource(R.string.link_blocked),
)

@OptIn(FlowPreview::class)
@Composable
private fun SaveScrollEffect(listState: LazyListState, onSave: (Int, Int) -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(500)
            .collect { (index, offset) -> onSave(index, offset) }
    }
}

@Composable
private fun ReadingProgress(listState: LazyListState) {
    val progress by remember(listState) {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val first = info.visibleItemsInfo.firstOrNull()
            when {
                total == 0 || first == null -> 0f
                !listState.canScrollForward -> 1f
                else -> {
                    val within = if (first.size > 0) -first.offset.toFloat() / first.size else 0f
                    ((first.index + within) / total).coerceIn(0f, 1f)
                }
            }
        }
    }
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(2.dp).testTag("reading_progress"),
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        drawStopIndicator = {},
        gapSize = 0.dp,
    )
}

@Composable
private fun RenderedView(
    state: ReaderState.Content,
    ui: ReaderUi,
    listState: LazyListState,
    onLink: (String) -> Unit,
) {
    val palette = LocalReaderPalette.current
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.code_copied)
    val currentOnLink by rememberUpdatedState(onLink)
    val ctx = remember(palette, ui.settings) {
        RenderContext(
            palette = palette,
            settings = ui.settings,
            onLink = { currentOnLink(it) },
            onCopy = { copyToClipboard(context, it, copiedMessage) },
        )
    }

    val blocks = state.document.blocks
    if (blocks.isEmpty()) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(stringResource(R.string.empty_document), color = palette.muted.toColor())
        }
        return
    }
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("document"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(blocks, contentType = { _, block -> block::class }) { index, block ->
                val highlight = FindHighlight(
                    ui.find.query.takeIf { ui.find.active }.orEmpty(),
                    (if (index == ui.find.currentBlock) palette.findCurrent else palette.findHighlight).toColor(),
                )
                CompositionLocalProvider(LocalFindHighlight provides highlight) {
                    ReadingColumn { MarkdownBlock(block, ctx) }
                }
            }
        }
    }
}

/** Keeps a comfortable line length on tablets and in landscape. */
@Composable
private fun ReadingColumn(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp)) { content() }
    }
}

@Composable
private fun SourceView(lines: List<String>, ui: ReaderUi, listState: LazyListState) {
    val palette = LocalReaderPalette.current
    val scale = ui.settings.textScale
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().testTag("source"),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        ) {
            itemsIndexed(lines) { index, line ->
                val query = ui.find.query.takeIf { ui.find.active }.orEmpty()
                val color = (if (index == ui.find.currentBlock) palette.findCurrent else palette.findHighlight).toColor()
                val text = remember(line, query, color) {
                    androidx.compose.ui.text.AnnotatedString(line).withFindHighlight(query, color)
                }
                ReadingColumn {
                    Text(
                        text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * scale,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindBar(find: FindState, actions: ReaderActions) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = actions.onCloseFind) {
                Icon(Icons.Filled.Close, stringResource(R.string.close_find))
            }
        },
        title = {
            TextField(
                value = find.query,
                onValueChange = actions.onQueryChange,
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("find_field"),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.find_in_document)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { actions.onNextMatch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
            )
        },
        actions = {
            if (find.query.isNotEmpty()) {
                Text(
                    if (find.matches.isEmpty()) "0/0" else "${find.current + 1}/${find.matches.size}",
                    Modifier.padding(horizontal = 4.dp).testTag("find_count"),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = actions.onPreviousMatch, enabled = find.matches.isNotEmpty()) {
                Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.previous_match))
            }
            IconButton(onClick = actions.onNextMatch, enabled = find.matches.isNotEmpty()) {
                Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.next_match))
            }
        },
    )
}

@Composable
private fun OverflowMenu(
    showSource: Boolean,
    canShare: Boolean,
    onToggleSource: () -> Unit,
    onAppearance: () -> Unit,
    onShare: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (showSource) R.string.view_formatted else R.string.view_source)) },
                leadingIcon = { Icon(if (showSource) Icons.AutoMirrored.Filled.Notes else Icons.Filled.Code, null) },
                onClick = { open = false; onToggleSource() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.appearance)) },
                leadingIcon = { Icon(Icons.Filled.TextFields, null) },
                onClick = { open = false; onAppearance() },
            )
            if (canShare) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    leadingIcon = { Icon(Icons.Filled.Share, null) },
                    onClick = { open = false; onShare() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentsSheet(state: ReaderState.Content, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.contents),
            Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        if (state.document.toc.isEmpty()) {
            Text(
                stringResource(R.string.no_headings),
                Modifier.padding(24.dp).navigationBarsPadding(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val minLevel = state.document.toc.minOf { it.level }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.testTag("toc")) {
                itemsIndexed(state.document.toc) { _, entry ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onSelect(entry.blockIndex) }
                            .padding(start = 24.dp + 16.dp * (entry.level - minLevel), end = 24.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(error: LoadError, onChooseFile: () -> Unit, modifier: Modifier = Modifier) {
    val (title, detail) = when (error) {
        LoadError.NOT_FOUND -> R.string.error_not_found_title to R.string.error_not_found_detail
        LoadError.NO_ACCESS -> R.string.error_no_access_title to R.string.error_no_access_detail
        LoadError.TOO_LARGE -> R.string.error_too_large_title to R.string.error_too_large_detail
        LoadError.BINARY -> R.string.error_binary_title to R.string.error_binary_detail
        LoadError.UNKNOWN -> R.string.error_unknown_title to R.string.error_unknown_detail
    }
    CenteredColumn(modifier.testTag("error")) {
        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onChooseFile) { Text(stringResource(R.string.choose_another_file)) }
    }
}

private fun openExternal(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE))
    true
} catch (_: ActivityNotFoundException) {
    false
}

private fun shareFile(context: Context, uri: String, name: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/markdown")
        .putExtra(Intent.EXTRA_STREAM, uri.toUri())
        .putExtra(Intent.EXTRA_TITLE, name)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, name))
}

private fun copyToClipboard(context: Context, text: String, confirmation: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("code", text))
    // Android 13 and later show their own confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
    }
}
