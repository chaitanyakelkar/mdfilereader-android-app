package io.github.chaitanyakelkar.mdreader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.chaitanyakelkar.mdreader.R
import io.github.chaitanyakelkar.mdreader.data.ReaderFont
import io.github.chaitanyakelkar.mdreader.data.ReaderSettings
import io.github.chaitanyakelkar.mdreader.data.ThemeMode
import kotlin.math.roundToInt

data class AppearanceActions(
    val onTheme: (ThemeMode) -> Unit,
    val onFont: (ReaderFont) -> Unit,
    val onTextScale: (Float) -> Unit,
    val onRemoteImages: (Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSheet(settings: ReaderSettings, actions: AppearanceActions, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .testTag("appearance_sheet"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium)

            SectionLabel(stringResource(R.string.theme))
            Segmented(
                options = ThemeMode.entries,
                selected = settings.theme,
                label = { stringResource(it.labelRes()) },
                onSelect = actions.onTheme,
            )

            SectionLabel(stringResource(R.string.font))
            Segmented(
                options = ReaderFont.entries,
                selected = settings.font,
                label = { stringResource(if (it == ReaderFont.SANS) R.string.font_sans else R.string.font_serif) },
                onSelect = actions.onFont,
            )

            SectionLabel(stringResource(R.string.text_size))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedIconButton(
                    onClick = { actions.onTextScale(settings.textScale - ReaderSettings.SCALE_STEP) },
                    enabled = settings.textScale > ReaderSettings.MIN_SCALE + 0.01f,
                ) {
                    Icon(Icons.Filled.Remove, stringResource(R.string.text_smaller))
                }
                Text(
                    "${(settings.textScale * 100).roundToInt()}%",
                    Modifier.testTag("text_size_value"),
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedIconButton(
                    onClick = { actions.onTextScale(settings.textScale + ReaderSettings.SCALE_STEP) },
                    enabled = settings.textScale < ReaderSettings.MAX_SCALE - 0.01f,
                ) {
                    Icon(Icons.Filled.Add, stringResource(R.string.text_larger))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.load_remote_images), Modifier.weight(1f))
                Switch(settings.loadRemoteImages, onCheckedChange = actions.onRemoteImages)
            }
        }
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SEPIA -> R.string.theme_sepia
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
                label = { Text(label(option), maxLines = 1) },
            )
        }
    }
}
