package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeUnavailableBinding
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import java.sql.Date
import java.time.Instant

class EpisodeUnavailableViewHolder(
    val binding: AdapterEpisodeUnavailableBinding,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
) : RecyclerView.ViewHolder(binding.root) {
    private inline val context get() = binding.root.context
    private val dateFormatter = RelativeDateFormatter(context)

    private var episodeUuid: String? = null

    fun bind(
        episodeUuid: String,
        podcastUuid: String,
        title: String,
        publishedAt: Instant,
    ) {
        this.episodeUuid = episodeUuid
        binding.titleLabel.text = title
        binding.dateLabel.text = dateFormatter.format(Date.from(publishedAt))
        imageRequestFactory.createForPodcast(podcastUuid).loadInto(binding.artworkImage)
    }
}
