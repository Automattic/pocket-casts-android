package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.text.rememberThemeAttributeColor
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.settings.PlaybackSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.PlaybackSettingsFragment.Companion.SCROLL_TO_SLEEP_TIMER
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlayerSleepTimerCancelledEvent
import com.automattic.eventhorizon.PlayerSleepTimerEnabledEvent
import com.automattic.eventhorizon.PlayerSleepTimerExtendedEvent
import com.automattic.eventhorizon.PlayerSleepTimerSettingsTappedEvent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class SleepFragment :
    BaseDialogFragment(),
    SleepListener {

    @Inject
    lateinit var eventHorizon: EventHorizon

    override val statusBarIconColor = StatusBarIconColor.Light

    override val includeNavigationBarPadding = false

    private val viewModel: PlayerViewModel by activityViewModels()
    private var timerJob: Job? = null

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARG)

    override fun onResume() {
        super.onResume()

        // refresh the sleep time every second
        timerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                viewModel.updateSleepTimer()
                delay(1.seconds)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        timerJob?.cancel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        val isSleepRunning by viewModel.isSleepRunning.observeAsState(false)
        val isAtEndOfEpisodeOrChapter by viewModel.isSleepAtEndOfEpisodeOrChapter.observeAsState(false)
        val timeLeftText by viewModel.sleepTimeLeftText.observeAsState("")
        val customTimeText by viewModel.sleepCustomTimeText.observeAsState("")
        val endOfEpisodesText by viewModel.sleepEndOfEpisodesText.observeAsState("")
        val endOfChaptersText by viewModel.sleepEndOfChaptersText.observeAsState("")
        val sleepingInText by viewModel.sleepingInText.observeAsState("")
        val playingEpisode by viewModel.playingEpisodeLive.observeAsState()

        val state = when {
            !isSleepRunning -> SleepState.Setup(
                customTimeText = customTimeText,
                endOfEpisodesText = endOfEpisodesText,
                endOfChaptersText = endOfChaptersText.takeIf { args.hasChapters },
            )

            isAtEndOfEpisodeOrChapter -> SleepState.RunningEndOfEpisodeOrChapter(sleepingInText = sleepingInText)

            else -> SleepState.RunningTime(timeLeftText = timeLeftText, hasChapters = args.hasChapters)
        }
        AppTheme(theme.activeTheme) {
            SleepPage(
                state = state,
                colors = SleepColors(
                    contrast01 = rememberThemeAttributeColor(UR.attr.player_contrast_01),
                    contrast05 = rememberThemeAttributeColor(UR.attr.player_contrast_05),
                    highlight = remember(playingEpisode) { Color(theme.playerHighlightColor(viewModel.podcast)) },
                ),
                listener = this,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom))
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(rememberScrollState())
                    .onSizeChanged { view?.post(::resettleExpanded) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (_, backgroundColor) ->
            setDialogTint(backgroundColor)
        }

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun resettleExpanded() {
        val behavior = (dialog as? BottomSheetDialog)?.behavior ?: return
        if (behavior.state == BottomSheetBehavior.STATE_SETTLING) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onMinutesClick(mins: Int) = startTimer(mins)

    override fun onCustomClick() = startCustomTimer()

    override fun onCustomMinusClick() = minusButtonClicked()

    override fun onCustomPlusClick() = plusButtonClicked()

    override fun onEndOfEpisodeClick() {
        val episodes = viewModel.getSleepEndOfEpisodes()
        eventHorizon.track(
            PlayerSleepTimerEnabledEvent(
                numberOfEpisodes = episodes.toLong(),
            ),
        )
        startTimerEndOfEpisode(episodes = episodes)
    }

    override fun onEndOfEpisodeMinusClick() = minusEndOfEpisodeButtonClicked()

    override fun onEndOfEpisodePlusClick() = plusEndOfEpisodeButtonClicked()

    override fun onEndOfChapterClick() {
        val chapters = viewModel.getSleepEndOfChapters()
        eventHorizon.track(
            PlayerSleepTimerEnabledEvent(
                numberOfChapters = chapters.toLong(),
            ),
        )
        startTimerEndOfChapter(chapters = chapters)
    }

    override fun onEndOfChapterMinusClick() = minusEndOfChapterButtonClicked()

    override fun onEndOfChapterPlusClick() = plusEndOfChapterButtonClicked()

    override fun onSettingsClick() {
        close()
        eventHorizon.track(PlayerSleepTimerSettingsTappedEvent)
        val fragment = PlaybackSettingsFragment().apply {
            arguments = Bundle().apply {
                putBoolean(SCROLL_TO_SLEEP_TIMER, true)
            }
        }
        val fragmentHostListener = (activity as? FragmentHostListener)
        fragmentHostListener?.apply {
            closePlayer() // Closes player if open
            openTab(VR.id.navigation_profile)
            addFragment(SettingsFragment())
            addFragment(fragment)
        }
    }

    override fun onAddMinutesClick(mins: Int) {
        if (mins == 5) addExtra5minute() else addExtra1minute()
    }

    override fun onExtendEndOfEpisodeClick() {
        val episodesAmountToExtend = 1
        eventHorizon.track(
            PlayerSleepTimerExtendedEvent(
                numberOfEpisodes = episodesAmountToExtend.toLong(),
            ),
        )
        startTimerEndOfEpisode(episodes = episodesAmountToExtend)
    }

    override fun onExtendEndOfChapterClick() {
        val chaptersAmountToExtend = 1
        eventHorizon.track(
            PlayerSleepTimerExtendedEvent(
                numberOfChapters = chaptersAmountToExtend.toLong(),
            ),
        )
        startTimerEndOfChapter(chapters = chaptersAmountToExtend)
    }

    override fun onCancelClick() = cancelTimer()

    private fun addExtra5minute() {
        viewModel.sleepTimerAddExtraMins(mins = 5)
        eventHorizon.track(
            PlayerSleepTimerExtendedEvent(
                seconds = 5.minutes.inWholeSeconds,
            ),
        )
    }

    private fun addExtra1minute() {
        viewModel.sleepTimerAddExtraMins(mins = 1)
        eventHorizon.track(
            PlayerSleepTimerExtendedEvent(
                seconds = 1.minutes.inWholeSeconds,
            ),
        )
    }

    private fun startCustomTimer() {
        viewModel.sleepTimerAfter(mins = viewModel.sleepCustomTimeInMinutes)
        eventHorizon.track(
            PlayerSleepTimerEnabledEvent(
                seconds = viewModel.sleepCustomTimeInMinutes.minutes.inWholeSeconds,
            ),
        )
        close()
    }

    private fun plusButtonClicked() {
        if (viewModel.sleepCustomTimeInMinutes < 5) {
            viewModel.sleepCustomTimeInMinutes += 1
        } else {
            viewModel.sleepCustomTimeInMinutes += 5
        }
    }

    private fun plusEndOfEpisodeButtonClicked() {
        viewModel.setSleepEndOfEpisodes(viewModel.getSleepEndOfEpisodes() + 1)
    }

    private fun plusEndOfChapterButtonClicked() {
        viewModel.setSleepEndOfChapters(viewModel.getSleepEndOfChapters() + 1)
    }

    private fun minusButtonClicked() {
        if (viewModel.sleepCustomTimeInMinutes <= 5) {
            viewModel.sleepCustomTimeInMinutes -= 1
        } else {
            viewModel.sleepCustomTimeInMinutes -= 5
        }
    }

    private fun minusEndOfEpisodeButtonClicked() {
        val endOfEpisodes = viewModel.getSleepEndOfEpisodes()
        if (endOfEpisodes > 1) {
            viewModel.setSleepEndOfEpisodes(endOfEpisodes - 1)
        }
    }

    private fun minusEndOfChapterButtonClicked() {
        val endOfChapters = viewModel.getSleepEndOfChapters()
        if (endOfChapters > 1) {
            viewModel.setSleepEndOfChapters(endOfChapters - 1)
        }
    }

    private fun startTimerEndOfEpisode(episodes: Int) {
        viewModel.sleepTimerAfterEpisode(episodes)
        close()
    }

    private fun startTimerEndOfChapter(chapters: Int) {
        viewModel.sleepTimerAfterChapter(chapters)
        close()
    }

    private fun startTimer(mins: Int) {
        viewModel.sleepTimerAfter(mins = mins)
        eventHorizon.track(
            PlayerSleepTimerEnabledEvent(
                seconds = mins.minutes.inWholeSeconds,
            ),
        )
        close()
    }

    private fun cancelTimer() {
        viewModel.cancelSleepTimer()
        eventHorizon.track(PlayerSleepTimerCancelledEvent)
        close()
    }

    private fun close() {
        dismiss()
    }

    @Parcelize
    private class Args(
        val hasChapters: Boolean,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "SleepFragmentArgs"

        fun newInstance(
            hasChapters: Boolean,
        ) = SleepFragment().apply {
            arguments = Bundle().apply {
                putParcelable(
                    NEW_INSTANCE_ARG,
                    Args(
                        hasChapters = hasChapters,
                    ),
                )
            }
        }
    }
}
