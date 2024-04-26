package au.com.shiftyjelly.pocketcasts.player.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentSleepBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.combineLatest
import au.com.shiftyjelly.pocketcasts.utils.minutes
import au.com.shiftyjelly.pocketcasts.views.extensions.applyColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class SleepFragment : BaseDialogFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    override val statusBarColor: StatusBarColor? = null

    private val viewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentSleepBinding? = null
    private var disposable: Disposable? = null

    override fun onResume() {
        super.onResume()

        // refresh the sleep time every second
        disposable = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { Timber.e(it) },
                onNext = { viewModel.updateSleepTimer() },
            )
    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSleepBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.buttonMins15.setOnClickListener { startTimer(mins = 15) }
        binding.buttonMins30.setOnClickListener { startTimer(mins = 30) }
        binding.buttonMins60.setOnClickListener { startTimer(mins = 60) }
        binding.customMinusButton.setOnClickListener { minusButtonClicked() }
        binding.endOfEpisodeMinusButton.setOnClickListener { minusEndOfEpisodeButtonClicked() }
        binding.endOfChapterMinusButton.setOnClickListener { minusEndOfChapterButtonClicked() }
        binding.customPlusButton.setOnClickListener { plusButtonClicked() }
        binding.endOfEpisodePlusButton.setOnClickListener { plusEndOfEpisodeButtonClicked() }
        binding.endOfChapterPlusButton.setOnClickListener { plusEndOfChapterButtonClicked() }
        binding.buttonCustom.setOnClickListener { startCustomTimer() }
        binding.buttonEndOfEpisode.setOnClickListener {
            val episodes = viewModel.getSleepEndOfEpisodes()
            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(TIME_KEY to END_OF_EPISODE, NUMBER_OF_EPISODES_KEY to episodes))
            startTimerEndOfEpisode(episodes = episodes)
        }
        binding.buttonEndOfChapter.setOnClickListener {
            val chapters = viewModel.getSleepEndOfChapters()
            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(TIME_KEY to END_OF_CHAPTER, NUMBER_OF_CHAPTERS_KEY to chapters))
            startTimerEndOfChapter(chapters = chapters)
        }
        binding.buttonAdd5Minute.setOnClickListener { addExtra5minute() }
        binding.buttonAdd1Minute.setOnClickListener { addExtra1minute() }
        binding.buttonEndOfEpisode2.setOnClickListener {
            val episodesAmountToExtend = 1
            analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_EXTENDED, mapOf(AMOUNT_KEY to END_OF_EPISODE, NUMBER_OF_EPISODES_KEY to episodesAmountToExtend))
            startTimerEndOfEpisode(episodes = episodesAmountToExtend)
        }
        binding.buttonCancelTime.setOnClickListener { cancelTimer() }
        binding.buttonCancelEndOfEpisodeOrChapter.setOnClickListener { cancelTimer() }

        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.sleepTimeLeftText.observe(viewLifecycleOwner) { sleepTime ->
            binding?.sleepTime?.text = sleepTime
        }

        viewModel.sleepCustomTimeText.observe(viewLifecycleOwner) { customTimeText ->
            binding?.labelCustom?.text = customTimeText
        }

        viewModel.sleepEndOfEpisodesText.observe(viewLifecycleOwner) { text ->
            binding?.labelEndOfEpisode?.text = text
        }

        viewModel.sleepEndOfChaptersText.observe(viewLifecycleOwner) { text ->
            binding?.labelEndOfChapter?.text = text
        }

        viewModel.sleepingInText.observe(viewLifecycleOwner) { text ->
            binding?.sleepingInText?.text = text
        }

        viewModel.isSleepRunning.observe(viewLifecycleOwner) { isSleepRunning ->
            binding?.sleepSetup?.isVisible = !isSleepRunning
            binding?.sleepRunning?.isVisible = isSleepRunning
        }

        viewModel.isSleepRunning.combineLatest(viewModel.isSleepAtEndOfEpisodeOrChapter)
            .observe(viewLifecycleOwner) { (isSleepRunning, isSleepAtEndOfEpisode) ->
                binding?.sleepRunningTime?.isVisible = isSleepRunning && !isSleepAtEndOfEpisode
                binding?.sleepRunningEndOfEpisodeOrChapter?.isVisible = isSleepRunning && isSleepAtEndOfEpisode
            }

        viewModel.playingEpisodeLive.observe(
            viewLifecycleOwner,
            Observer { (_, backgroundColor) ->
                applyColor(theme, backgroundColor)

                val tintColor = theme.playerHighlightColor(viewModel.podcast)
                val tintColorStateList = ColorStateList.valueOf(tintColor)
                val binding = binding ?: return@Observer
                binding.buttonAdd5Minute.strokeColor = tintColorStateList
                binding.buttonAdd5Minute.setTextColor(tintColorStateList)
                binding.buttonAdd1Minute.strokeColor = tintColorStateList
                binding.buttonAdd1Minute.setTextColor(tintColorStateList)
                binding.buttonCancelEndOfEpisodeOrChapter.strokeColor = tintColorStateList
                binding.buttonCancelEndOfEpisodeOrChapter.setTextColor(tintColorStateList)
                binding.buttonCancelTime.strokeColor = tintColorStateList
                binding.buttonCancelTime.setTextColor(tintColorStateList)
                binding.buttonEndOfEpisode2.strokeColor = tintColorStateList
                binding.buttonEndOfEpisode2.setTextColor(tintColorStateList)

                binding.sleepAnimation.post { // this only works the second time it's called unless it's in a post
                    binding.sleepAnimation.addValueCallback(KeyPath("**"), LottieProperty.COLOR) { tintColor }
                }
            },
        )

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun addExtra5minute() {
        viewModel.sleepTimerAddExtraMins(mins = 5)
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_EXTENDED, mapOf(AMOUNT_KEY to TimeUnit.MILLISECONDS.toSeconds(5.minutes())))
        viewModel.timeLeftInSeconds()?.let { timeLeft ->
            binding?.root?.announceForAccessibility("5 minutes added to sleep timer. ${timeLeft / 60} minutes ${timeLeft % 60} seconds remaining")
        }
    }

    private fun addExtra1minute() {
        viewModel.sleepTimerAddExtraMins(mins = 1)
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_EXTENDED, mapOf(AMOUNT_KEY to TimeUnit.MILLISECONDS.toSeconds(1.minutes())))
        viewModel.timeLeftInSeconds()?.let { timeLeft ->
            binding?.root?.announceForAccessibility("1 minute added to sleep timer. ${timeLeft / 60} minutes ${timeLeft % 60} seconds remaining")
        }
    }

    private fun startCustomTimer() {
        viewModel.sleepTimerAfter(mins = viewModel.sleepCustomTimeMins)
        binding?.root?.announceForAccessibility("Sleep timer set for ${viewModel.sleepCustomTimeMins} minutes")
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(TIME_KEY to TimeUnit.MILLISECONDS.toSeconds(viewModel.sleepCustomTimeMins.minutes())))
        close()
    }

    private fun plusButtonClicked() {
        if (viewModel.sleepCustomTimeMins < 5) {
            viewModel.sleepCustomTimeMins += 1
        } else {
            viewModel.sleepCustomTimeMins += 5
        }
        binding?.root?.announceForAccessibility("Custom sleep time ${viewModel.sleepCustomTimeMins}")
    }

    private fun plusEndOfEpisodeButtonClicked() {
        viewModel.setSleepEndOfEpisodes(viewModel.getSleepEndOfEpisodes() + 1)
        binding?.root?.announceForAccessibility("Sleep time end of episode ${viewModel.getSleepEndOfEpisodes()}")
    }

    private fun plusEndOfChapterButtonClicked() {
        viewModel.setSleepEndOfChapters(viewModel.getSleepEndOfChapters() + 1)
        binding?.root?.announceForAccessibility("Sleep time chapter ${viewModel.getSleepEndOfChapters()}")
    }

    private fun minusButtonClicked() {
        if (viewModel.sleepCustomTimeMins <= 5) {
            viewModel.sleepCustomTimeMins -= 1
        } else {
            viewModel.sleepCustomTimeMins -= 5
        }
        binding?.root?.announceForAccessibility("Custom sleep time ${viewModel.sleepCustomTimeMins}")
    }

    private fun minusEndOfEpisodeButtonClicked() {
        val endOfEpisodes = viewModel.getSleepEndOfEpisodes()
        if (endOfEpisodes > 1) {
            viewModel.setSleepEndOfEpisodes(endOfEpisodes - 1)
        }
        binding?.root?.announceForAccessibility("Sleep time end of episode ${viewModel.getSleepEndOfEpisodes() }")
    }

    private fun minusEndOfChapterButtonClicked() {
        val endOfChapters = viewModel.getSleepEndOfChapters()
        if (endOfChapters > 1) {
            viewModel.setSleepEndOfChapters(endOfChapters - 1)
        }
        binding?.root?.announceForAccessibility("Sleep time end of chapter ${viewModel.getSleepEndOfChapters() }")
    }

    private fun startTimerEndOfEpisode(episodes: Int) {
        viewModel.sleepTimerAfterEpisode(episodes)
        binding?.root?.announceForAccessibility("Sleep timer set for end of episode")
        close()
    }

    private fun startTimerEndOfChapter(chapters: Int) {
        viewModel.sleepTimerAfterChapter(chapters)
        binding?.root?.announceForAccessibility("Sleep timer set for end of chapter")
        close()
    }

    private fun startTimer(mins: Int) {
        viewModel.sleepTimerAfter(mins = mins)
        binding?.root?.announceForAccessibility("Sleep timer set for $mins minutes")
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_ENABLED, mapOf(TIME_KEY to TimeUnit.MILLISECONDS.toSeconds(mins.minutes())))
        close()
    }

    private fun cancelTimer() {
        viewModel.cancelSleepTimer()
        binding?.root?.announceForAccessibility("Sleep timer cancelled")
        analyticsTracker.track(AnalyticsEvent.PLAYER_SLEEP_TIMER_CANCELLED)
        close()
    }

    private fun close() {
        dismiss()
    }

    companion object {
        private const val TIME_KEY = "time" // in seconds
        private const val AMOUNT_KEY = "amount"
        private const val NUMBER_OF_EPISODES_KEY = "number_of_episodes"
        private const val END_OF_EPISODE = "end_of_episode"
        private const val NUMBER_OF_CHAPTERS_KEY = "number_of_chapters"
        private const val END_OF_CHAPTER = "end_of_chapter"
    }
}
