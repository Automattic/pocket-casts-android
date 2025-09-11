package au.com.shiftyjelly.pocketcasts.playlists.component

import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.AdapterEpisodeUnavailableBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import java.sql.Date

class EpisodeUnavailableViewHolder(
    val binding: AdapterEpisodeUnavailableBinding,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val onRowClick: (PlaylistEpisode.Unavailable) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private inline val context get() = binding.root.context
    private val dateFormatter = RelativeDateFormatter(context)

    private lateinit var episodeWrapper: PlaylistEpisode.Unavailable

    init {
        binding.episodeRow.setOnClickListener {
            onRowClick(episodeWrapper)
        }
    }

    fun bind(
        episodeWrapper: PlaylistEpisode.Unavailable,
    ) {
        this.episodeWrapper = episodeWrapper

        binding.titleLabel.text = episodeWrapper.episode.title
        binding.dateLabel.text = dateFormatter.format(Date.from(episodeWrapper.episode.publishedAt))
        imageRequestFactory.createForPodcast(episodeWrapper.episode.podcastUuid).loadInto(binding.artworkImage)
    }
}
