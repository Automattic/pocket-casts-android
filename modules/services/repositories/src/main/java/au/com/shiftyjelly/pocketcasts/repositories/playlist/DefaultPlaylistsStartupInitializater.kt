package au.com.shiftyjelly.pocketcasts.repositories.playlist

import android.content.Context
import androidx.startup.Initializer
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.di.initialzierEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

class DefaultPlaylistsStartupInitializater : Initializer<Unit> {
    @Inject lateinit var initializater: DefaultPlaylistsInitializater

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun create(context: Context) {
        context.initialzierEntryPoint().inject(this)
        applicationScope.launch(NonCancellable) {
            initializater.initialize()
        }
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
