package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts.config

import android.app.Application
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts.ActionHelperQueryPodcasts
import au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts.InputQueryPodcasts
import au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts.OutputQueryPodcasts
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModelConfigQueryPodcasts @Inject constructor(
    application: Application
) : ViewModelBase<InputQueryPodcasts, Array<OutputQueryPodcasts>, ActionHelperQueryPodcasts>(application), TaskerPluginConfig<InputQueryPodcasts> {
    override fun getNewHelper(pluginConfig: TaskerPluginConfig<InputQueryPodcasts>) = ActionHelperQueryPodcasts(pluginConfig)
}
