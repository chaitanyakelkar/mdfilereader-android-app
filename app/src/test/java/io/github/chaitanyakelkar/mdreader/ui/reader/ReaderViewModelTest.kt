package io.github.chaitanyakelkar.mdreader.ui.reader

import io.github.chaitanyakelkar.mdreader.data.DocumentLoadException
import io.github.chaitanyakelkar.mdreader.data.DocumentSource
import io.github.chaitanyakelkar.mdreader.data.LoadError
import io.github.chaitanyakelkar.mdreader.data.LoadedFile
import io.github.chaitanyakelkar.mdreader.data.RecentFile
import io.github.chaitanyakelkar.mdreader.data.RecentsCodec
import io.github.chaitanyakelkar.mdreader.data.RecentsStore
import io.github.chaitanyakelkar.mdreader.data.SAMPLE_DOCUMENT_URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeRecents(initial: List<RecentFile> = emptyList()) : RecentsStore {
        override val recents = MutableStateFlow(initial)
        override suspend fun find(uri: String) = recents.value.firstOrNull { it.uri == uri }
        override suspend fun recordOpened(uri: String, name: String, now: Long) {
            recents.value = RecentsCodec.withOpened(recents.value, uri, name, now)
        }
        override suspend fun saveScroll(uri: String, index: Int, offset: Int) {
            recents.value = recents.value.map {
                if (it.uri == uri) it.copy(scrollIndex = index, scrollOffset = offset) else it
            }
        }
        override suspend fun remove(uri: String) {
            recents.value = recents.value.filterNot { it.uri == uri }
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private fun model(
        text: String = DOC,
        recents: RecentsStore = FakeRecents(),
        uri: String = "content://doc",
    ) = ReaderViewModel(
        uri = uri,
        source = { LoadedFile("doc.md", text) },
        recents = recents,
        clock = { 1000L },
        workDispatcher = dispatcher,
    )

    private fun content(model: ReaderViewModel) = model.state.value as ReaderState.Content

    @Test fun `starts loading then shows parsed content`() = runTest(dispatcher) {
        val model = model()
        assertEquals(ReaderState.Loading, model.state.value)
        advanceUntilIdle()
        assertEquals("doc.md", content(model).name)
        assertEquals(2, content(model).document.toc.size)
    }

    @Test fun `load failures map to their error`() = runTest(dispatcher) {
        val model = ReaderViewModel(
            uri = "u",
            source = DocumentSource { throw DocumentLoadException(LoadError.BINARY) },
            recents = FakeRecents(),
            workDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertEquals(ReaderState.Failed(LoadError.BINARY), model.state.value)
    }

    @Test fun `unexpected failure is reported rather than crashing`() = runTest(dispatcher) {
        val model = ReaderViewModel(
            uri = "u",
            source = DocumentSource { throw IllegalStateException("boom") },
            recents = FakeRecents(),
            workDispatcher = dispatcher,
        )
        advanceUntilIdle()
        assertEquals(ReaderState.Failed(LoadError.UNKNOWN), model.state.value)
    }

    @Test fun `successful open is recorded and failed open is not`() = runTest(dispatcher) {
        val recents = FakeRecents()
        model(recents = recents)
        advanceUntilIdle()
        assertEquals(listOf("content://doc"), recents.recents.value.map { it.uri })

        val other = FakeRecents()
        ReaderViewModel("u", { throw DocumentLoadException(LoadError.NOT_FOUND) }, other, workDispatcher = dispatcher)
        advanceUntilIdle()
        assertTrue(other.recents.value.isEmpty())
    }

    @Test fun `sample is not added to recents`() = runTest(dispatcher) {
        val recents = FakeRecents()
        model(recents = recents, uri = SAMPLE_DOCUMENT_URI)
        advanceUntilIdle()
        assertTrue(recents.recents.value.isEmpty())
    }

    @Test fun `saved scroll position is restored and can be updated`() = runTest(dispatcher) {
        val recents = FakeRecents(listOf(RecentFile("content://doc", "doc.md", 1, 3, 40)))
        val model = model(recents = recents)
        advanceUntilIdle()
        assertEquals(3, content(model).initialScrollIndex)
        assertEquals(40, content(model).initialScrollOffset)

        model.saveScroll(9, 1)
        advanceUntilIdle()
        assertEquals(9, recents.recents.value.single().scrollIndex)
    }

    @Test fun `find counts matches and wraps in both directions`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.openFind()
        model.setQuery("apple")
        advanceUntilIdle()
        assertEquals(3, model.find.value.matches.size)
        assertEquals(0, model.find.value.current)

        model.previousMatch()
        assertEquals(2, model.find.value.current)
        model.nextMatch()
        assertEquals(0, model.find.value.current)
    }

    @Test fun `find with no match is safe to navigate`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.openFind()
        model.setQuery("zzz")
        advanceUntilIdle()
        model.nextMatch()
        model.previousMatch()
        assertEquals(-1, model.find.value.current)
        assertNull(model.find.value.currentBlock)
    }

    @Test fun `closing find clears it`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.openFind()
        model.setQuery("apple")
        advanceUntilIdle()
        model.closeFind()
        assertEquals(FindState(), model.find.value)
    }

    @Test fun `closing find cancels a running search`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.openFind()
        model.setQuery("apple")
        model.closeFind()
        advanceUntilIdle()
        assertTrue(model.find.value.matches.isEmpty())
    }

    @Test fun `source view searches raw lines`() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.openFind()
        model.setQuery("#")
        advanceUntilIdle()
        assertTrue("rendered text has no markers", model.find.value.matches.isEmpty())

        model.toggleSource()
        advanceUntilIdle()
        assertTrue(model.showSource.value)
        assertEquals(3, model.find.value.matches.size)

        model.toggleSource()
        advanceUntilIdle()
        assertFalse(model.showSource.value)
    }

    @Test fun `empty file is shown as an empty document`() = runTest(dispatcher) {
        val model = model(text = "")
        advanceUntilIdle()
        assertTrue(content(model).document.blocks.isEmpty())
    }

    private companion object {
        const val DOC = "# Apple\n\napple pie\n\n## Other\n\nApple"
    }
}
