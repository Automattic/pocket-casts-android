package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters.config

import android.app.Application
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters.ActionHelperQueryFilters
import au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters.InputQueryFilters
import au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters.OutputQueryFilters
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModelConfigQueryFilters @Inject constructor(
    application: Application
) : ViewModelBase<InputQueryFilters, Array<OutputQueryFilters>, ActionHelperQueryFilters>(application), TaskerPluginConfig<InputQueryFilters> {
    override fun getNewHelper(pluginConfig: TaskerPluginConfig<InputQueryFilters>) = ActionHelperQueryFilters(pluginConfig)
}
