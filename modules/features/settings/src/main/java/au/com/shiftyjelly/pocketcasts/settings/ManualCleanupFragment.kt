package au.com.shiftyjelly.pocketcasts.settings

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralEpisodes
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.databinding.FragmentManualcleanupBinding
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val ARG_TOOLBAR = "showtoolbar"

@AndroidEntryPoint
class ManualCleanupFragment : BaseFragment(), CoroutineScope {
    companion object {
        fun newInstance(showToolbar: Boolean = false): ManualCleanupFragment {
            val fragment = ManualCleanupFragment()
            fragment.arguments = bundleOf(
                ARG_TOOLBAR to showToolbar
            )
            return fragment
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var playbackManager: PlaybackManager

    private var binding: FragmentManualcleanupBinding? = null
    private val switchState: BehaviorRelay<Boolean> = BehaviorRelay.createDefault(false)
    private val episodesToDelete: MutableList<Episode> = mutableListOf()
    private val showToolbar: Boolean
        get() = arguments?.getBoolean(ARG_TOOLBAR) ?: false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentManualcleanupBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.unplayed.setup(LR.string.unplayed, 0, "")
        binding.inProgress.setup(LR.string.in_progress, 0, "")
        binding.played.setup(LR.string.played, 0, "")

        val toolbar = binding.toolbar
        if (showToolbar) {
            toolbar.setup(title = getString(LR.string.settings_title_manage_downloads), navigationIcon = BackArrow, activity = activity, theme = theme)
        } else {
            toolbar.isVisible = false
            (activity as AppCompatActivity).supportActionBar?.title = getString(LR.string.settings_title_manage_downloads)
        }

        val downloadedEpisodes = LiveDataReactiveStreams.fromPublisher(
            episodeManager.observeDownloadedEpisodes()
                .combineLatest(switchState.toFlowable(BackpressureStrategy.LATEST))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
        )
        downloadedEpisodes.observe(
            viewLifecycleOwner,
            Observer { (downloadedEpisodes, _) ->
                val fragmentBinding = this.binding ?: return@Observer
                val context = this.context ?: return@Observer
                val downloadedAdjustedForStarred = downloadedEpisodes.filter { !it.isStarred || fragmentBinding.switchStarred.isChecked }
                val unplayedEpisodes = downloadedAdjustedForStarred.filter { it.playingStatus == EpisodePlayingStatus.NOT_PLAYED }
                val inProgressEpisodes = downloadedAdjustedForStarred.filter { it.playingStatus == EpisodePlayingStatus.IN_PROGRESS }
                val playedEpisodes = downloadedAdjustedForStarred.filter { it.playingStatus == EpisodePlayingStatus.COMPLETED }

                val downloadSize = downloadedAdjustedForStarred.map { it.sizeInBytes }.sum()

                episodesToDelete.clear()

                updateDiskSpaceView(unplayedEpisodes, downloadSize, fragmentBinding.unplayed)
                updateDiskSpaceView(inProgressEpisodes, downloadSize, fragmentBinding.inProgress)
                updateDiskSpaceView(playedEpisodes, downloadSize, fragmentBinding.played)

                fragmentBinding.lblTotal.text = Util.formattedBytes(bytes = downloadSize, context = context)

                updateButton()
            }
        )
        updateButton()

        binding.btnDelete.setOnClickListener {
            if (episodesToDelete.isNotEmpty()) {
                launch { episodeManager.deleteEpisodeFiles(episodesToDelete, playbackManager) }
                Toast.makeText(binding.btnDelete.context, LR.string.settings_manage_downloads_deleting, Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchStarred.setOnCheckedChangeListener { _, isChecked -> switchState.accept(isChecked) }
    }

    private fun updateDiskSpaceView(episodes: List<Episode>, totalSize: Long, view: DiskSpaceSizeView) {
        val context = context ?: return
        val size = episodes.map { it.sizeInBytes }.sum()
        val percentage = size.toDouble() / totalSize.toDouble() * 100.0
        val byteString = Util.formattedBytes(bytes = size, context = context)
        val subtitle = "${resources.getStringPluralEpisodes(episodes.size)} · $byteString"
        view.update(percentage.toInt(), subtitle)
        updateDeleteList(view.isChecked, episodes)
        view.onCheckedChanged = { isChecked ->
            updateDeleteList(isChecked, episodes)
            updateButton()
        }
    }

    private fun updateDeleteList(isChecked: Boolean, episodes: List<Episode>) {
        if (isChecked) {
            episodesToDelete.addAll(episodes)
        } else {
            episodesToDelete.removeAll(episodes)
        }
    }

    private fun updateButton() {
        val binding = binding ?: return
        val btnDelete = binding.btnDelete
        if (episodesToDelete.isEmpty()) {
            btnDelete.setText(LR.string.settings_select_episodes_to_delete)
            btnDelete.isEnabled = false
            btnDelete.backgroundTintList = ColorStateList.valueOf(btnDelete.context.getThemeColor(UR.attr.primary_interactive_01_disabled))
        } else {
            btnDelete.text = getString(LR.string.settings_delete_episodes, episodesToDelete.size)
            btnDelete.isEnabled = true
            btnDelete.backgroundTintList = ColorStateList.valueOf(btnDelete.context.getThemeColor(UR.attr.primary_interactive_01))
        }
    }
}
