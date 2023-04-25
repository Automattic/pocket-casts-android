package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EpisodeChipViewModel @Inject constructor(
    private val episodeManager: EpisodeManager,
) : ViewModel() {
    fun observeByUuid(episode: BaseEpisode) =
        episodeManager.observeByUuid(episode.uuid)
}
