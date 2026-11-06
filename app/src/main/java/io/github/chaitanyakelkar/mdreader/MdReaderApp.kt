package io.github.chaitanyakelkar.mdreader

import android.app.Application
import android.content.Context
import io.github.chaitanyakelkar.mdreader.data.ContentDocumentSource
import io.github.chaitanyakelkar.mdreader.data.DataStoreRecents
import io.github.chaitanyakelkar.mdreader.data.DocumentSource
import io.github.chaitanyakelkar.mdreader.data.RecentsStore
import io.github.chaitanyakelkar.mdreader.data.SettingsRepository
import io.github.chaitanyakelkar.mdreader.data.appDataStore

/** Manual dependency injection, which is enough for an app of this size. */
class AppContainer(context: Context) {
    val documents: DocumentSource = ContentDocumentSource(context)
    val recents: RecentsStore = DataStoreRecents(context.appDataStore)
    val settings = SettingsRepository(context.appDataStore)
}

class MdReaderApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
