package io.github.chaitanyakelkar.mdreader

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chaitanyakelkar.mdreader.data.ReaderFont
import io.github.chaitanyakelkar.mdreader.data.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReaderFlowTest {
    @get:Rule val rule = createEmptyComposeRule()

    private val container get() = ApplicationProvider.getApplicationContext<MdReaderApp>().container

    @Before fun resetState() {
        runBlocking {
            container.recents.recents.first().forEach { container.recents.remove(it.uri) }
            container.settings.setTheme(ThemeMode.SYSTEM)
            container.settings.setFont(ReaderFont.SANS)
            container.settings.setTextScale(1f)
            container.settings.setLoadRemoteImages(true)
        }
    }

    private fun openSample(): ActivityScenario<MainActivity> {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        rule.onNodeWithText("Try the sample").performClick()
        rule.waitUntilExactlyOneExists(androidx.compose.ui.test.hasTestTag("document"), 10_000)
        return scenario
    }

    @Test fun firstRunShowsEmptyState() {
        ActivityScenario.launch(MainActivity::class.java)
        rule.onNodeWithTag("empty_state").assertIsDisplayed()
        rule.onNodeWithText("Open file").assertIsDisplayed()
    }

    @Test fun sampleRendersCoreElements() {
        openSample()
        rule.onNodeWithText("Welcome to MD Reader").assertIsDisplayed()
        rule.onNodeWithText("Properties").assertIsDisplayed()
        val document = rule.onNodeWithTag("document")
        document.performScrollToNode(hasContentDescription("Completed task"))
        rule.onAllNodesWithContentDescription("Completed task")[0].assertIsDisplayed()
        // The far right column exists in the tree, and is reachable by scrolling the table.
        document.performScrollToNode(hasText("Notes on behavior"))
        rule.onNodeWithText("Notes on behavior").assertExists()
        document.performScrollToNode(hasContentDescription("Copy code"))
        rule.onAllNodesWithContentDescription("Copy code")[0].assertIsDisplayed()
    }

    @Test fun contentsJumpsToHeading() {
        openSample()
        rule.onNodeWithContentDescription("Contents").performClick()
        rule.onNodeWithTag("toc").performScrollToNode(hasText("Images"))
        rule.onNodeWithText("Images").performClick()
        rule.waitUntilExactlyOneExists(hasText("Remote images load when allowed in Appearance."), 5_000)
        rule.onNodeWithText("Remote images load when allowed in Appearance.").assertIsDisplayed()
    }

    @Test fun findCountsNavigatesAndBackClosesItFirst() {
        openSample()
        rule.onNodeWithContentDescription("Find").performClick()
        rule.onNodeWithTag("find_field").performTextInput("footnote")
        rule.waitUntilExactlyOneExists(hasText("1/", substring = true), 5_000)
        rule.onNodeWithContentDescription("Next match").performClick()
        rule.onNodeWithTag("find_count").assertTextEquals("2/2")

        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        rule.onNodeWithContentDescription("Find").assertIsDisplayed()
        rule.onNodeWithTag("document").assertExists()
    }

    @Test fun findWithNoMatchShowsZero() {
        openSample()
        rule.onNodeWithContentDescription("Find").performClick()
        rule.onNodeWithTag("find_field").performTextInput("zzzqqq")
        rule.waitUntilExactlyOneExists(hasText("0/0"), 5_000)
    }

    @Test fun sourceToggleShowsRawMarkdown() {
        openSample()
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("View source").performClick()
        rule.onNodeWithTag("source").performScrollToNode(hasText("# Welcome to MD Reader"))
        rule.onNodeWithText("# Welcome to MD Reader").assertIsDisplayed()

        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("View formatted").performClick()
        rule.onNodeWithTag("document").assertExists()
    }

    @Test fun appearanceChangesApplyAndPersist() {
        openSample()
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Appearance").performClick()
        rule.onNodeWithText("Sepia").performClick()
        rule.onNodeWithText("Serif").performClick()
        rule.onNodeWithContentDescription("Larger text").performClick()
        rule.waitUntilExactlyOneExists(hasText("110%"), 5_000)
        rule.waitForIdle()

        val saved = runBlocking { container.settings.settings.first() }
        assertEquals(ThemeMode.SEPIA, saved.theme)
        assertEquals(ReaderFont.SERIF, saved.font)
        assertEquals(1.1f, saved.textScale, 0.001f)
    }

    @Test fun documentSurvivesRecreation() {
        val scenario = openSample()
        scenario.recreate()
        rule.waitUntilExactlyOneExists(androidx.compose.ui.test.hasTestTag("document"), 10_000)
        rule.onNodeWithText("Welcome to MD Reader").assertIsDisplayed()
    }
}
