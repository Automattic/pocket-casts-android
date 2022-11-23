package au.com.shiftyjelly.pocketcasts.taskerplugin.queryUpNext.config

import android.app.Application
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.queryUpNext.ActionHelperQueryUpNext
import au.com.shiftyjelly.pocketcasts.taskerplugin.queryUpNext.InputQueryUpNext
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModelConfigQueryUpNext @Inject constructor(
    application: Application
) : ViewModelBase<InputQueryUpNext, Array<OutputQueryEpisodes>, ActionHelperQueryUpNext>(application), TaskerPluginConfig<InputQueryUpNext> {
    override fun getNewHelper(pluginConfig: TaskerPluginConfig<InputQueryUpNext>) = ActionHelperQueryUpNext(pluginConfig)
    override val inputFields: List<InputFieldBase<*>> = listOf()
}
