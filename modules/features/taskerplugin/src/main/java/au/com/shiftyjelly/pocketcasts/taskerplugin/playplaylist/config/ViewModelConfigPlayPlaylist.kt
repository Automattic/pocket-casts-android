package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.config

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.ActionHelperPlayPlaylist
import au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist.InputPlayPlaylist
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as RD

@HiltViewModel
class ViewModelConfigPlayPlaylist @Inject constructor(
    application: Application
) : ViewModelBase<InputPlayPlaylist, Unit, ActionHelperPlayPlaylist>(application), TaskerPluginConfig<InputPlayPlaylist> {
    override fun getNewHelper(pluginConfig: TaskerPluginConfig<InputPlayPlaylist>) = ActionHelperPlayPlaylist(pluginConfig)

    private inner class InputField constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, valueGetter: InputPlayPlaylist.() -> String?, valueSetter: InputPlayPlaylist.(String?) -> Unit) : InputFieldBase<String>(labelResId, iconResId, valueGetter, valueSetter) {
        override val askFor get() = true
        override fun getPossibleValues(): Flow<List<String>>? {
            return context.playlistManager.findAllFlow().map { playlist -> playlist.map { it.title } }
        }
    }

    override val inputFields: List<InputFieldBase<*>> = listOf(
        InputField(R.string.filters_filter_name, RD.drawable.filter_bullet, { title }, { title = it })
    )
}
