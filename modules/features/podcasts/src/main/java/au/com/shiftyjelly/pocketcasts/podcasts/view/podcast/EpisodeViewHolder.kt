package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions

class EpisodeViewHolder(
    binding: AdapterEpisodeBinding,
    showArtwork: Boolean,
    fromListUuid: String?,
    rowDataProvider: EpisodeRowDataProvider,
    imageRequestFactory: PocketCastsImageRequestFactory,
    swipeRowActionsFactory: SwipeRowActions.Factory,
    playButtonListener: PlayButton.OnClickListener,
    onRowClick: (PodcastEpisode) -> Unit,
    onRowLongClick: (PodcastEpisode) -> Unit,
    onSwipeAction: (PodcastEpisode, SwipeAction) -> Unit,
) : BaseEpisodeViewHolder<PodcastEpisode>(
    binding = binding,
    fromListUuid = fromListUuid,
    showArtwork = showArtwork,
    rowDataProvider = rowDataProvider,
    imageRequestFactory = imageRequestFactory,
    swipeRowActionsFactory = swipeRowActionsFactory,
    playButtonListener = playButtonListener,
    onRowClick = onRowClick,
    onRowLongClick = onRowLongClick,
    onSwipeAction = onSwipeAction,
) {
    override fun toPodcastEpisode(item: PodcastEpisode) = item

    override fun getSwipeActions(item: PodcastEpisode, factory: SwipeRowActions.Factory): SwipeRowActions? {
        return factory.podcastEpisode(item)
    }
}
