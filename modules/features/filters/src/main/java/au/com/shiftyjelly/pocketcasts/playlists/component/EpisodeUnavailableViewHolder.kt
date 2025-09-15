package au.com.shiftyjelly.pocketcasts.playlists.component

import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.AdapterEpisodeUnavailableBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowLayout
import java.sql.Date

class EpisodeUnavailableViewHolder(
    private val binding: AdapterEpisodeUnavailableBinding,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
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
            swipeLayout.settle()
        }
        swipeRowActionsFactory.unavailablePlaylistEpisode().applyTo(swipeLayout)
        swipeLayout.addOnSwipeActionListener { action -> onSwipeAction(requireNotNull(episodeWrapper), action) }
    }

    private var isMultiSelectEnabled = false

    fun bind(
        episodeWrapper: PlaylistEpisode.Unavailable,
        isMultiSelectEnabled: Boolean,
    ) {
        if (episodeWrapper.uuid != this.episodeWrapper?.uuid) {
            swipeLayout.clearTranslation()
        }
        this.episodeWrapper = episodeWrapper

        if (this.isMultiSelectEnabled != isMultiSelectEnabled) {
            this.isMultiSelectEnabled = isMultiSelectEnabled
            if (isMultiSelectEnabled) {
                swipeLayout.clearTranslation()
                swipeLayout.lock()
            } else {
                swipeLayout.unlock()
            }
        }

        binding.titleLabel.text = episodeWrapper.episode.title
        binding.dateLabel.text = dateFormatter.format(Date.from(episodeWrapper.episode.publishedAt))
        imageRequestFactory.createForPodcast(episodeWrapper.episode.podcastUuid).loadInto(binding.artworkImage)
    }
}
