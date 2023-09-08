package au.com.shiftyjelly.pocketcasts.filters

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import au.com.shiftyjelly.pocketcasts.filters.databinding.DurationOptionsFragmentBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistProperty
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistUpdateSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserPlaylistUpdate
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.updateTint
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ARG_PLAYLIST_UUID = "playlist_uuid"

@AndroidEntryPoint
class DurationOptionsFragment : BaseFragment() {
    companion object {
        fun newInstance(playlist: Playlist): DurationOptionsFragment {
            val bundle = Bundle()
            bundle.putString(ARG_PLAYLIST_UUID, playlist.uuid)
            val fragment = DurationOptionsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject lateinit var playlistManager: PlaylistManager
    var playlist: Playlist? = null

    private var binding: DurationOptionsFragmentBinding? = null
    private var userChanged = false
    private var switchDurationInitialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DurationOptionsFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        binding.lblTitle.text = getString(LR.string.filters_episode_duration)
        binding.btnClose.setOnClickListener {
            @Suppress("DEPRECATION")
            activity?.onBackPressed()
        }

        val stepperLongerThan = binding.stepperLongerThan
        val formatter: (Int) -> String = {
            TimeHelper.getTimeDurationShortString(
                timeMs = (it.toDouble() * 60000).toLong(),
                context = context,
                emptyString = context?.getString(LR.string.time_short_seconds, 0) ?: ""
            )
        }
        val voiceOverFormatter: (Int) -> String = {
            TimeHelper.getTimeDurationString(
                timeMs = (it.toDouble() * 60000).toLong(),
                context = context,
                emptyString = context?.getString(LR.string.seconds_plural, 0) ?: ""
            )
        }
        stepperLongerThan.formatter = formatter
        stepperLongerThan.voiceOverFormatter = voiceOverFormatter
        stepperLongerThan.voiceOverPrefix = getString(LR.string.filters_duration_longer_than)
        val stepperShorterThan = binding.stepperShorterThan
        stepperShorterThan.formatter = formatter
        stepperShorterThan.voiceOverFormatter = voiceOverFormatter
        stepperShorterThan.voiceOverPrefix = getString(LR.string.filters_duration_shorter_than)

        val switchDuration = binding.switchDuration
        switchDuration.setOnCheckedChangeListener { _, isChecked ->
            playlist?.filterDuration = isChecked
            enableDurations(isChecked)
            if (switchDurationInitialized) {
                userChanged = true
            }
        }
        val btnSave = binding.btnSave
        val btnClose = binding.btnClose

        launch {
            val playlist = playlistManager.findByUuid(requireArguments().getString(ARG_PLAYLIST_UUID)!!) ?: return@launch
            this@DurationOptionsFragment.playlist = playlist

            enableDurations(playlist.filterDuration)
            switchDurationInitialized = false
            switchDuration.isChecked = playlist.filterDuration
            switchDurationInitialized = true

            val onStepperValueChanged = { _: Int -> userChanged = true }

            // Do not want the onStepperValueChanged callback to fire during initialization because that is not a user initiated change
            stepperLongerThan.onValueChanged = null
            stepperLongerThan.value = playlist.longerThan
            stepperLongerThan.onValueChanged = onStepperValueChanged

            stepperShorterThan.onValueChanged = null
            stepperShorterThan.value = playlist.shorterThan
            stepperShorterThan.onValueChanged = onStepperValueChanged

            val color = playlist.getColor(context)
            val filterTintColor = ThemeColor.filterInteractive01(theme.activeTheme, color)
            val filterTintList = ColorStateList.valueOf(filterTintColor)
            btnSave.setBackgroundColor(filterTintColor)
            btnSave.setTextColor(ThemeColor.filterInteractive02(theme.activeTheme, color))

            btnClose.imageTintList = filterTintList

            switchDuration.updateTint(filterTintColor, ThemeColor.contrast01(theme.activeTheme))
            stepperShorterThan.tintColor = filterTintList
            stepperLongerThan.tintColor = filterTintList
        }

        btnSave.setOnClickListener {
            val shorterValue: Int = stepperShorterThan.value
            val longerValue: Int = stepperLongerThan.value
            if (switchDuration.isChecked && shorterValue <= longerValue) {
                val message = getString(LR.string.filters_duration_error_body, longerValue, shorterValue)
                UiUtil.displayAlertError(btnSave.context, getString(LR.string.filters_duration_error_title), message, null)
                return@setOnClickListener
            }

            playlist?.let { playlist ->
                launch(Dispatchers.Default) {
                    playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
                    playlist.shorterThan = shorterValue
                    playlist.longerThan = longerValue
                    val userPlaylistUpdate = if (userChanged) {
                        UserPlaylistUpdate(
                            listOf(PlaylistProperty.Duration),
                            PlaylistUpdateSource.FILTER_EPISODE_LIST
                        )
                    } else null
                    playlistManager.update(playlist, userPlaylistUpdate)
                    launch(Dispatchers.Main) { (activity as FragmentHostListener).closeModal(this@DurationOptionsFragment) }
                }
            }
        }
    }

    private fun enableDurations(enabled: Boolean) {
        val binding = binding ?: return

        binding.stepperLongerThan.isEnabled = enabled
        binding.stepperShorterThan.isEnabled = enabled

        binding.lblShorterThan.alpha = if (enabled) 1.0f else 0.5f
        binding.lblLongerThan.alpha = binding.lblShorterThan.alpha
    }
}
