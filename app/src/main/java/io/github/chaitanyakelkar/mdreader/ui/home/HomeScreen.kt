package io.github.chaitanyakelkar.mdreader.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.chaitanyakelkar.mdreader.R
import io.github.chaitanyakelkar.mdreader.data.ReaderSettings
import io.github.chaitanyakelkar.mdreader.data.RecentFile
import io.github.chaitanyakelkar.mdreader.ui.components.AppearanceActions
import io.github.chaitanyakelkar.mdreader.ui.components.AppearanceSheet
import io.github.chaitanyakelkar.mdreader.ui.components.CenteredColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recents: List<RecentFile>,
    settings: ReaderSettings,
    appearance: AppearanceActions,
    onOpenFile: () -> Unit,
    onOpenSample: () -> Unit,
    onOpenRecent: (RecentFile) -> Unit,
    onRemoveRecent: (RecentFile) -> Unit,
    now: Long = System.currentTimeMillis(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showAppearance by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showAppearance = true }) {
                        Icon(Icons.Filled.TextFields, stringResource(R.string.appearance))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (recents.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onOpenFile,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.open_file)) },
                )
            }
        },
    ) { padding ->
        if (recents.isEmpty()) {
            EmptyState(onOpenFile, onOpenSample, Modifier.padding(padding))
        } else {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("recents"),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(recents, key = { it.uri }) { recent ->
                        RecentRow(recent, now, { onOpenRecent(recent) }, { onRemoveRecent(recent) })
                    }
                }
            }
        }
        if (showAppearance) {
            AppearanceSheet(settings, appearance, onDismiss = { showAppearance = false })
        }
    }
}

@Composable
private fun RecentRow(recent: RecentFile, now: Long, onOpen: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Article, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                recent.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                DateUtils.getRelativeTimeSpanString(recent.openedAt, now, DateUtils.MINUTE_IN_MILLIS).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Filled.Close,
                stringResource(R.string.remove_recent, recent.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(onOpenFile: () -> Unit, onOpenSample: () -> Unit, modifier: Modifier = Modifier) {
    CenteredColumn(modifier.testTag("empty_state")) {
        Icon(
            Icons.AutoMirrored.Filled.Article,
            null,
            Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.no_recent_files), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.empty_hint),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onOpenFile) { Text(stringResource(R.string.open_file)) }
        TextButton(onClick = onOpenSample) { Text(stringResource(R.string.try_sample)) }
    }
}
