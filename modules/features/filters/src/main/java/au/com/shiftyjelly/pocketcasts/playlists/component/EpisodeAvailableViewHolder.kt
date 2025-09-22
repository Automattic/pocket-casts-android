package au.com.shiftyjelly.pocketcasts.playlists.component

import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.BaseEpisodeViewHolder
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions

class EpisodeAvailableViewHolder(
    binding: AdapterEpisodeBinding,
    private val playlistType: Playlist.Type,
    rowDataProvider: EpisodeRowDataProvider,
    imageRequestFactory: PocketCastsImageRequestFactory,
    swipeRowActionsFactory: SwipeRowActions.Factory,
    playButtonListener: PlayButton.OnClickListener,
    onRowClick: (PlaylistEpisode.Available) -> Unit,
    onRowLongClick: (PlaylistEpisode.Available) -> Unit,
    onSwipeAction: (PlaylistEpisode.Available, SwipeAction) -> Unit,
) : BaseEpisodeViewHolder<PlaylistEpisode.Available>(
    binding = binding,
    fromListUuid = null,
    showArtwork = true,
    rowDataProvider = rowDataProvider,
    imageRequestFactory = imageRequestFactory,
    swipeRowActionsFactory = swipeRowActionsFactory,
    playButtonListener = playButtonListener,
    onRowClick = onRowClick,
    onRowLongClick = onRowLongClick,
    onSwipeAction = onSwipeAction,
) {
    override fun toPodcastEpisode(item: PlaylistEpisode.Available) = item.episode

    override fun getSwipeActions(item: PlaylistEpisode.Available, factory: SwipeRowActions.Factory): SwipeRowActions? {
        return factory.availablePlaylistEpisode(playlistType, item.episode)
    }
}
