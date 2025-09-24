package au.com.shiftyjelly.pocketcasts.repositories.playlist

import android.content.Context
import androidx.startup.Initializer
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.di.initializerEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

class DefaultPlaylistsStartupInitializer : Initializer<Unit> {
    @Inject lateinit var initializer: DefaultPlaylistsInitializer

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun create(context: Context) {
        context.initializerEntryPoint().inject(this)
        applicationScope.launch(NonCancellable) {
            initializer.initialize()
        }
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
