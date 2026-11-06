package io.github.chaitanyakelkar.mdreader

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var externalUri by mutableStateOf<String?>(null)
    private var pickedUri by mutableStateOf<String?>(null)

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            keepAccess(uri)
            pickedUri = uri.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // After a configuration change the navigation state already holds this document.
        if (savedInstanceState == null) externalUri = documentUri(intent)

        val container = (application as MdReaderApp).container
        setContent {
            AppRoot(
                container = container,
                externalUri = externalUri,
                onExternalUriHandled = { externalUri = null },
                // Any type: Android has no dependable MIME type for .md, and a narrower filter
                // greys out real Markdown files. Non-text files are rejected on load.
                launchPicker = { picker.launch(arrayOf("*/*")) },
                pickedUri = pickedUri,
                onPickedUriHandled = { pickedUri = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalUri = documentUri(intent)
    }

    private fun documentUri(intent: Intent?): String? {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> streamExtra(intent)
            else -> null
        }
        return uri?.toString()
    }

    private fun streamExtra(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    /** Lets recent files reopen after a restart. Not every provider supports this. */
    private fun keepAccess(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // Access still lasts for this session.
        }
    }
}
