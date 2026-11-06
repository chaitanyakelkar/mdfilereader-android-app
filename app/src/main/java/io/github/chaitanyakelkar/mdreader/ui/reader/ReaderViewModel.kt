package io.github.chaitanyakelkar.mdreader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.chaitanyakelkar.mdreader.data.DocumentLoadException
import io.github.chaitanyakelkar.mdreader.data.DocumentSource
import io.github.chaitanyakelkar.mdreader.data.LoadError
import io.github.chaitanyakelkar.mdreader.data.RecentsStore
import io.github.chaitanyakelkar.mdreader.data.SAMPLE_DOCUMENT_URI
import io.github.chaitanyakelkar.mdreader.markdown.DocumentSearch
import io.github.chaitanyakelkar.mdreader.markdown.MarkdownParser
import io.github.chaitanyakelkar.mdreader.markdown.MdBlock
import io.github.chaitanyakelkar.mdreader.markdown.MdDocument
import io.github.chaitanyakelkar.mdreader.markdown.SearchMatch
import io.github.chaitanyakelkar.mdreader.markdown.searchableText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ReaderState {
    data object Loading : ReaderState
    data class Failed(val error: LoadError) : ReaderState
    data class Content(
        val name: String,
        val document: MdDocument,
        val sourceLines: List<String>,
        val blockTexts: List<String>,
        val initialScrollIndex: Int,
        val initialScrollOffset: Int,
    ) : ReaderState
}

data class FindState(
    val active: Boolean = false,
    val query: String = "",
    val matches: List<SearchMatch> = emptyList(),
    val current: Int = -1,
) {
    val currentBlock: Int? get() = matches.getOrNull(current)?.blockIndex
}

class ReaderViewModel(
    private val uri: String,
    private val source: DocumentSource,
    private val recents: RecentsStore,
    private val parser: MarkdownParser = MarkdownParser(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    private val _showSource = MutableStateFlow(false)
    val showSource: StateFlow<Boolean> = _showSource.asStateFlow()

    private val _find = MutableStateFlow(FindState())
    val find: StateFlow<FindState> = _find.asStateFlow()

    private var searchJob: Job? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                val file = source.load(uri)
                val saved = recents.find(uri)
                val content = withContext(workDispatcher) {
                    val document = parseSafely(file.text)
                    ReaderState.Content(
                        name = file.name,
                        document = document,
                        sourceLines = file.text.split('\n'),
                        blockTexts = document.blocks.map { it.searchableText() },
                        initialScrollIndex = saved?.scrollIndex ?: 0,
                        initialScrollOffset = saved?.scrollOffset ?: 0,
                    )
                }
                if (uri != SAMPLE_DOCUMENT_URI) recents.recordOpened(uri, file.name, clock())
                content
            } catch (e: CancellationException) {
                throw e
            } catch (e: DocumentLoadException) {
                ReaderState.Failed(e.error)
            } catch (_: Exception) {
                ReaderState.Failed(LoadError.UNKNOWN)
            }
        }
    }

    /** A file the parser cannot handle is still readable: show it as one preformatted block. */
    private fun parseSafely(text: String): MdDocument = try {
        parser.parse(text)
    } catch (_: StackOverflowError) {
        MdDocument(listOf(MdBlock.CodeBlock(null, text)))
    } catch (_: RuntimeException) {
        MdDocument(listOf(MdBlock.CodeBlock(null, text)))
    }

    fun toggleSource() {
        _showSource.update { !it }
        if (_find.value.active) runSearch(_find.value.query)
    }

    fun openFind() {
        _find.update { it.copy(active = true) }
    }

    fun closeFind() {
        searchJob?.cancel()
        _find.value = FindState()
    }

    fun setQuery(query: String) {
        _find.update { it.copy(query = query) }
        runSearch(query)
    }

    private fun runSearch(query: String) {
        val content = _state.value as? ReaderState.Content ?: return
        val texts = if (_showSource.value) content.sourceLines else content.blockTexts
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val matches = withContext(workDispatcher) { DocumentSearch.find(texts, query) }
            _find.update { it.copy(matches = matches, current = if (matches.isEmpty()) -1 else 0) }
        }
    }

    fun nextMatch() = moveMatch(1)

    fun previousMatch() = moveMatch(-1)

    private fun moveMatch(delta: Int) {
        _find.update { state ->
            if (state.matches.isEmpty()) state
            else state.copy(current = (state.current + delta).mod(state.matches.size))
        }
    }

    fun saveScroll(index: Int, offset: Int) {
        viewModelScope.launch { recents.saveScroll(uri, index, offset) }
    }
}
