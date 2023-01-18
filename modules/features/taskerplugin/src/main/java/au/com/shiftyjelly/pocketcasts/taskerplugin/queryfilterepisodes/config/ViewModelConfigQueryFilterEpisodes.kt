package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilterepisodes.config

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilterepisodes.ActionHelperQueryFilterEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilterepisodes.InputQueryFilterEpisodes
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as RD

@HiltViewModel
class ViewModelConfigQueryFilterEpisodes @Inject constructor(
    application: Application
) : ViewModelBase<InputQueryFilterEpisodes, Array<OutputQueryEpisodes>, ActionHelperQueryFilterEpisodes>(application), TaskerPluginConfig<InputQueryFilterEpisodes> {
    override fun getNewHelper(pluginConfig: TaskerPluginConfig<InputQueryFilterEpisodes>) = ActionHelperQueryFilterEpisodes(pluginConfig)

    private inner class InputField constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, valueGetter: InputQueryFilterEpisodes.() -> String?, valueSetter: InputQueryFilterEpisodes.(String?) -> Unit) : InputFieldBase<String>(labelResId, iconResId, valueGetter, valueSetter) {
        override val askFor get() = true
        override fun getPossibleValues(): Flow<List<String>> {
            return context.playlistManager.findAllFlow().map { podcast -> podcast.map { it.title } }
        }
    }

    override val inputFields: List<InputFieldBase<*>> = listOf(
        InputField(R.string.filter_id_or_title, RD.drawable.auto_tab_filter, { titleOrId }, { titleOrId = it })
    )
}
