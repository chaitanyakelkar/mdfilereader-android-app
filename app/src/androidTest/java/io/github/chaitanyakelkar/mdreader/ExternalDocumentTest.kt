package io.github.chaitanyakelkar.mdreader

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chaitanyakelkar.mdreader.data.ContentDocumentSource
import io.github.chaitanyakelkar.mdreader.data.DocumentLoadException
import io.github.chaitanyakelkar.mdreader.data.LoadError
import io.github.chaitanyakelkar.mdreader.data.SAMPLE_DOCUMENT_URI
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Covers how files arrive from other apps: intent matching, decoding, and failure states. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ExternalDocumentTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val container get() = (context as MdReaderApp).container
    private val source get() = ContentDocumentSource(context)

    private fun doc(name: String) = "content://${context.packageName}.testdocs/$name"

    @Before fun clearRecents() = runBlocking {
        container.recents.recents.first().forEach { container.recents.remove(it.uri) }
    }

    private fun handlesView(uri: String, type: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(uri), type)
            .setPackage(context.packageName)
        return context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
    }

    @Test fun openWithMatchesMarkdownFromFileManagers() {
        assertTrue(handlesView("content://a/README.md", "text/markdown"))
        assertTrue(handlesView("content://a/README.md", "text/x-markdown"))
        assertTrue(handlesView("content://a/notes.txt", "text/plain"))
        assertTrue("generic type, md extension", handlesView("content://a/README.md", "application/octet-stream"))
        assertTrue("generic type, dotted name", handlesView("content://a/v1.2.notes.md", "application/octet-stream"))
        assertTrue(handlesView("content://a/guide.markdown", "application/octet-stream"))
    }

    @Test fun openWithIgnoresOtherFiles() {
        assertFalse(handlesView("content://a/photo.jpg", "image/jpeg"))
        assertFalse(handlesView("content://a/data.bin", "application/octet-stream"))
    }

    @Test fun loadsUtf16AndUsesProviderName() = runBlocking {
        val file = source.load(doc("utf16.md"))
        assertEquals("Named utf16.md", file.name)
        assertEquals(TestDocsProvider.UTF16_TEXT, file.text)
    }

    @Test fun loadsSampleAsset() = runBlocking {
        assertTrue(source.load(SAMPLE_DOCUMENT_URI).text.contains("# Welcome to MD Reader"))
    }

    private fun assertLoadFails(expected: LoadError, name: String) = runBlocking {
        try {
            source.load(doc(name))
            fail("expected $expected")
        } catch (e: DocumentLoadException) {
            assertEquals(expected, e.error)
        }
    }

    @Test fun binaryIsRejected() = assertLoadFails(LoadError.BINARY, "binary.md")

    @Test fun oversizedIsRejected() = assertLoadFails(LoadError.TOO_LARGE, "big.md")

    @Test fun missingFileIsReported() = assertLoadFails(LoadError.NOT_FOUND, "missing.md")

    private fun launchView(name: String) {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(doc(name)))
        ActivityScenario.launch<MainActivity>(intent)
    }

    @Test fun viewIntentOpensAndRendersAndIsRemembered() = runBlocking {
        launchView("crlf.md")
        rule.waitUntilExactlyOneExists(hasText("CRLF title"), 10_000)
        rule.onNodeWithText("CRLF title").assertIsDisplayed()
        rule.onNodeWithText("Named crlf.md").assertIsDisplayed()
        rule.waitForIdle()
        assertEquals(listOf(doc("crlf.md")), container.recents.recents.first().map { it.uri })
    }

    /** Regression: real document URIs contain escapes, and permission grants match them exactly. */
    @Test fun encodedUriReachesTheLoaderUnchanged() = runBlocking {
        val encoded = doc("crlf.md") + "?id=msf%3A123%2Fa"
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(Uri.parse(encoded))
        ActivityScenario.launch<MainActivity>(intent)
        rule.waitUntilExactlyOneExists(hasText("CRLF title"), 10_000)
        rule.waitForIdle()
        assertEquals(listOf(encoded), container.recents.recents.first().map { it.uri })
    }

    @Test fun viewIntentForBinaryShowsErrorState() {
        launchView("binary.md")
        rule.waitUntilExactlyOneExists(hasTestTag("error"), 10_000)
        rule.onNodeWithText("This isn't a text file").assertIsDisplayed()
        rule.onNodeWithTag("error").assertIsDisplayed()
    }
}
