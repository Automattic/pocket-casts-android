package au.com.shiftyjelly.pocketcasts.playlists.component

import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.AdapterEpisodeUnavailableBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.playlists.SwipeAction
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.views.component.SwipeRowLayout
import java.sql.Date
import timber.log.Timber

class EpisodeUnavailableViewHolder(
    val binding: AdapterEpisodeUnavailableBinding,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val onRowClick: (PlaylistEpisode.Unavailable) -> Unit,
    private val onSwipeAction: (PlaylistEpisode.Unavailable, SwipeAction) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private inline val context get() = binding.root.context

    @Suppress("UNCHECKED_CAST")
    private val swipeLayout = binding.root as SwipeRowLayout<SwipeAction>

    private val dateFormatter = RelativeDateFormatter(context)

    private var episodeWrapper: PlaylistEpisode.Unavailable? = null

    init {
        binding.episodeRow.setOnClickListener {
            onRowClick(requireNotNull(episodeWrapper))
        }
        swipeLayout.setRtl1State(SwipeAction.Remove)
        swipeLayout.addOnSwipeActionListener { action -> onSwipeAction(requireNotNull(episodeWrapper), action) }
    }

    fun bind(
        episodeWrapper: PlaylistEpisode.Unavailable,
    ) {
        if (episodeWrapper.uuid != this.episodeWrapper?.uuid) {
            swipeLayout.clearTranslation()
        }
        this.episodeWrapper = episodeWrapper

        binding.titleLabel.text = episodeWrapper.episode.title
        binding.dateLabel.text = dateFormatter.format(Date.from(episodeWrapper.episode.publishedAt))
        imageRequestFactory.createForPodcast(episodeWrapper.episode.podcastUuid).loadInto(binding.artworkImage)
    }
}
