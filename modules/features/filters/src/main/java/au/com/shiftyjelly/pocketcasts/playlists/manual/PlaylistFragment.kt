package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.text.input.TextFieldState
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import au.com.shiftyjelly.pocketcasts.filters.databinding.PlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderAdapter
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.time.Duration
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlaylistFragment : BaseFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = PlaylistFragmentBinding.inflate(inflater, container, false)
        binding.setupContent()
        return binding.root
    }

    private fun PlaylistFragmentBinding.setupContent() {
        val headerAdapter = PlaylistHeaderAdapter(
            themeType = theme.activeTheme,
            leftButton = PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_add_episodes,
                label = getString(LR.string.add_episodes),
                onClick = { Timber.i("Add episodes") }
            ),
            rightButton = PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_play,
                label = getString(LR.string.playlist_play_all),
                onClick = { Timber.i("Play all") }
            ),
            searchState = TextFieldState(),
            onChangeSearchFocus = { _, _ -> Timber.i("Scroll to content") }
        )
        headerAdapter.submitHeader(
            PlaylistHeaderData(
                title = "Playlist title",
                totalEpisodeCount = 0,
                displayedEpisodeCount = 0,
                playbackDurationLeft = Duration.ZERO,
                artworkPodcastUuids = emptyList(),
            )
        )
        content.adapter = headerAdapter
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "ManualPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = PlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}
