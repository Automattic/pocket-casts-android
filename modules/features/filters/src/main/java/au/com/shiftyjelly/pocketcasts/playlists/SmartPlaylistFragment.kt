package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.PlaylistEpisodesAdapterFactory
import au.com.shiftyjelly.pocketcasts.filters.databinding.SmartPlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@AndroidEntryPoint
class SmartPlaylistFragment :
    BaseFragment(),
    HasBackstack {

    @Inject
    lateinit var adapterFactory: PlaylistEpisodesAdapterFactory

    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<SmartPlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SmartPlaylistViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = SmartPlaylistFragmentBinding.inflate(inflater, container, false)
        val adapter = adapterFactory.create(
            multiSelectToolbar = binding.multiSelectToolbar,
            onChangeMultiSelect = { isMultiSelecting -> Timber.i("Is multi selecting: $isMultiSelecting") },
            getEpisodes = { viewModel.uiState.value.smartPlaylist?.episodes.orEmpty() },
        )
        binding.content.adapter = adapter
        EpisodeItemTouchHelper().attachToRecyclerView(binding.content)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { uiState ->
                    val episodes = uiState.smartPlaylist?.episodes.orEmpty()
                    adapter.submitList(episodes)
                }
        }
        return binding.root
    }

    override fun onBackPressed(): Boolean {
        return if (adapterFactory.onBackPressed()) {
            true
        } else {
            super.onBackPressed()
        }
    }

    override fun getBackstackCount(): Int {
        return adapterFactory.getBackstackCount() + super.getBackstackCount()
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "SmartPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = SmartPlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}
