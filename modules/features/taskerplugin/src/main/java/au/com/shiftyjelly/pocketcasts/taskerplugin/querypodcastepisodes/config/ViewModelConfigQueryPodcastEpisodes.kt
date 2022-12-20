package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes.config

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.podcastManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes.ActionHelperQueryPodcastEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes.InputQueryPodcastEpisodes
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as RD

@HiltViewModel
class ViewModelConfigQueryPodcastEpisodes @Inject constructor(
    application: Application
) : ViewModelBase<InputQueryPodcastEpisodes, Array<OutputQueryEpisodes>, ActionHelperQueryPodcastEpisodes>(application), TaskerPluginConfig<InputQueryPodcastEpisodes> {
    override fun getNewHelper(pluginConfig: TaskerPluginConfig<InputQueryPodcastEpisodes>) = ActionHelperQueryPodcastEpisodes(pluginConfig)

    private inner class InputField constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, valueGetter: InputQueryPodcastEpisodes.() -> String?, valueSetter: InputQueryPodcastEpisodes.(String?) -> Unit) : InputFieldBase<String>(labelResId, iconResId, valueGetter, valueSetter) {
        override val askFor get() = true
        override fun getPossibleValues(): Flow<List<String>> {
            return context.podcastManager.findSubscribedFlow().map { podcast -> podcast.map { it.title } }
        }
    }

    override val inputFields: List<InputFieldBase<*>> = listOf(
        InputField(R.string.podcast_id_or_title, RD.drawable.auto_tab_podcasts, { titleOrId }, { titleOrId = it })
    )
}
