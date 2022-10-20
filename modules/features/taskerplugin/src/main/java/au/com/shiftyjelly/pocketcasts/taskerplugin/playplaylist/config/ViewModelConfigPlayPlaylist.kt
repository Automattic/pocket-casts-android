package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.config

import android.app.Application
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.hilt.playlistManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.ActionHelperPlayPlaylist
import au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.InputPlayPlaylist
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ViewModelConfigPlayPlaylist @Inject constructor(
    application: Application
) : ViewModelBase<InputPlayPlaylist, ActionHelperPlayPlaylist>(application), TaskerPluginConfig<InputPlayPlaylist> {
    override val context get() = getApplication<Application>()

    val titleState by lazy { MutableStateFlow(input?.title) }

    var title: String? = null
        set(value) {
            input?.title = value
            titleState.tryEmit(value)
        }
    override val helperClass: Class<ActionHelperPlayPlaylist>
        get() = ActionHelperPlayPlaylist::class.java

    val playlists get() = context.playlistManager.observeAll()
}
