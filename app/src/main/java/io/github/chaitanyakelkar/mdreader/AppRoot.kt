package io.github.chaitanyakelkar.mdreader

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.chaitanyakelkar.mdreader.data.ReaderSettings
import io.github.chaitanyakelkar.mdreader.data.SAMPLE_DOCUMENT_URI
import io.github.chaitanyakelkar.mdreader.ui.components.AppearanceActions
import io.github.chaitanyakelkar.mdreader.ui.home.HomeScreen
import io.github.chaitanyakelkar.mdreader.ui.reader.ReaderActions
import io.github.chaitanyakelkar.mdreader.ui.reader.ReaderScreen
import io.github.chaitanyakelkar.mdreader.ui.reader.ReaderUi
import io.github.chaitanyakelkar.mdreader.ui.reader.ReaderViewModel
import io.github.chaitanyakelkar.mdreader.ui.theme.MdReaderTheme
import kotlinx.coroutines.launch

private const val HOME = "home"
private const val READER = "reader/{uri}"

private fun NavHostController.openDocument(uri: String) {
    navigate("reader/${Uri.encode(uri)}") {
        popUpTo(HOME)
        launchSingleTop = true
    }
}

@Composable
fun AppRoot(
    container: AppContainer,
    externalUri: String?,
    onExternalUriHandled: () -> Unit,
    launchPicker: () -> Unit,
    pickedUri: String?,
    onPickedUriHandled: () -> Unit,
) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(ReaderSettings())
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    val appearance = remember(container) {
        AppearanceActions(
            onTheme = { scope.launch { container.settings.setTheme(it) } },
            onFont = { scope.launch { container.settings.setFont(it) } },
            onTextScale = { scope.launch { container.settings.setTextScale(it) } },
            onRemoteImages = { scope.launch { container.settings.setLoadRemoteImages(it) } },
        )
    }

    MdReaderTheme(settings.theme) {
        NavHost(navController, startDestination = HOME) {
            composable(HOME) {
                val recents by container.recents.recents.collectAsStateWithLifecycle(emptyList())
                HomeScreen(
                    recents = recents,
                    settings = settings,
                    appearance = appearance,
                    onOpenFile = launchPicker,
                    onOpenSample = { navController.openDocument(SAMPLE_DOCUMENT_URI) },
                    onOpenRecent = { navController.openDocument(it.uri) },
                    onRemoveRecent = { recent -> scope.launch { container.recents.remove(recent.uri) } },
                )
            }
            composable(READER, arguments = listOf(navArgument("uri") { type = NavType.StringType })) { entry ->
                // Navigation has already decoded this. Decoding again would corrupt document
                // URIs such as ".../document/msf%3A123", and access would be denied.
                val uri = entry.arguments?.getString("uri").orEmpty()
                val model: ReaderViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { ReaderViewModel(uri, container.documents, container.recents) }
                    },
                )
                val state by model.state.collectAsStateWithLifecycle()
                val showSource by model.showSource.collectAsStateWithLifecycle()
                val find by model.find.collectAsStateWithLifecycle()
                ReaderScreen(
                    uri = uri,
                    ui = ReaderUi(state, showSource, find, settings),
                    actions = ReaderActions(
                        onBack = { navController.popBackStack() },
                        onChooseFile = launchPicker,
                        onToggleSource = model::toggleSource,
                        onOpenFind = model::openFind,
                        onCloseFind = model::closeFind,
                        onQueryChange = model::setQuery,
                        onNextMatch = model::nextMatch,
                        onPreviousMatch = model::previousMatch,
                        onSaveScroll = model::saveScroll,
                        appearance = appearance,
                    ),
                )
            }
        }

        LaunchedEffect(externalUri) {
            externalUri?.let {
                navController.openDocument(it)
                onExternalUriHandled()
            }
        }
        LaunchedEffect(pickedUri) {
            pickedUri?.let {
                navController.openDocument(it)
                onPickedUriHandled()
            }
        }
    }
}
