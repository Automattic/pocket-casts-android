package au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext.config

import android.app.Application
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext.ActionHelperAddToUpNext
import au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext.InputAddToUpNext
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as RD

@HiltViewModel
class ViewModelConfigAddToUpNext @Inject constructor(
    application: Application
) : ViewModelBase<InputAddToUpNext, Unit, ActionHelperAddToUpNext>(application), TaskerPluginConfig<InputAddToUpNext> {
    override fun getNewHelper(pluginConfig: TaskerPluginConfig<InputAddToUpNext>) = ActionHelperAddToUpNext(pluginConfig)

    override val inputFields: List<InputFieldBase<*>> = listOf(
        InputFieldString(R.string.episode_ids, RD.drawable.ic_upnext, { episodeIds }, { episodeIds = it }),
        InputFieldEnum<InputAddToUpNext.AddMode>(R.string.add_mode, RD.drawable.ic_upnext_playlast, { addMode }, { addMode = it }, { it?.descriptionResId }),
        InputFieldEnum<InputAddToUpNext.ClearMode>(R.string.clear_before_adding, RD.drawable.ic_upnext_remove, { clearMode }, { clearMode = it }, { it?.descriptionResId }),
    )
}
