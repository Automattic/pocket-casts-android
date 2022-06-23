package au.com.shiftyjelly.pocketcasts.player.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentSleepBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
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
import timber.log.Timber
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SleepFragment : BaseDialogFragment() {
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
                onNext = { viewModel.updateSleepTimer() }
            )
    }

    override fun onPause() {
        super.onPause()

        disposable?.dispose()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSleepBinding.inflate(inflater, container, false)
        this.binding = binding

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        binding.buttonMins5.setOnClickListener { startTimer(mins = 5) }
        binding.buttonMins15.setOnClickListener { startTimer(mins = 15) }
        binding.buttonMins30.setOnClickListener { startTimer(mins = 30) }
        binding.buttonMins60.setOnClickListener { startTimer(mins = 60) }
        binding.buttonEndOfEpisode.setOnClickListener { startTimerEndOfEpisode() }
        binding.customMinusButton.setOnClickListener { minusButtonClicked() }
        binding.customPlusButton.setOnClickListener { plusButtonClicked() }
        binding.buttonCustom.setOnClickListener { startCustomTimer() }
        binding.buttonAddTime.setOnClickListener { addExtraMins() }
        binding.buttonEndOfEpisode2.setOnClickListener { startTimerEndOfEpisode() }
        binding.buttonCancelTime.setOnClickListener { cancelTimer() }
        binding.buttonCancelEndOfEpisode.setOnClickListener { cancelTimer() }

        return binding.root
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel.playingEpisodeLive.observe(
            viewLifecycleOwner,
            Observer { (_, backgroundColor) ->
                applyColor(theme, backgroundColor)

                val tintColor = theme.playerHighlightColor(viewModel.podcast)
                val tintColorStateList = ColorStateList.valueOf(tintColor)
                val binding = binding ?: return@Observer
                binding.buttonAddTime.strokeColor = tintColorStateList
                binding.buttonAddTime.setTextColor(tintColorStateList)
                binding.buttonCancelEndOfEpisode.strokeColor = tintColorStateList
                binding.buttonCancelEndOfEpisode.setTextColor(tintColorStateList)
                binding.buttonCancelTime.strokeColor = tintColorStateList
                binding.buttonCancelTime.setTextColor(tintColorStateList)
                binding.buttonEndOfEpisode2.strokeColor = tintColorStateList
                binding.buttonEndOfEpisode2.setTextColor(tintColorStateList)

                binding.sleepAnimation.post { // this only works the second time it's called unless it's in a post
                    binding.sleepAnimation.addValueCallback(KeyPath("**"), LottieProperty.COLOR) { tintColor }
                }
            }
        )

        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun addExtraMins() {
        viewModel.sleepTimerAddExtraMins(mins = 5)
        viewModel.timeLeftInSeconds()?.let { timeLeft ->
            binding?.root?.announceForAccessibility("5 minutes added to sleep timer. ${timeLeft / 60} minutes ${timeLeft % 60} seconds remaining")
        }
    }

    private fun startCustomTimer() {
        viewModel.sleepTimerAfter(mins = viewModel.sleepCustomTimeMins)
        binding?.root?.announceForAccessibility("Sleep timer set for ${viewModel.sleepCustomTimeMins} minutes")
        close()
    }

    private fun plusButtonClicked() {
        viewModel.sleepCustomTimeMins = viewModel.sleepCustomTimeMins + 5
        binding?.root?.announceForAccessibility("Custom sleep time ${viewModel.sleepCustomTimeMins}")
    }

    private fun minusButtonClicked() {
        viewModel.sleepCustomTimeMins = viewModel.sleepCustomTimeMins - 5
        binding?.root?.announceForAccessibility("Custom sleep time ${viewModel.sleepCustomTimeMins}")
    }

    private fun startTimerEndOfEpisode() {
        viewModel.sleepTimerAfterEpisode()
        binding?.root?.announceForAccessibility("Sleep timer set for end of episode")
        close()
    }

    private fun startTimer(mins: Int) {
        viewModel.sleepTimerAfter(mins = mins)
        binding?.root?.announceForAccessibility("Sleep timer set for $mins minutes")
        close()
    }

    private fun cancelTimer() {
        viewModel.cancelSleepTimer()
        binding?.root?.announceForAccessibility("Sleep timer cancelled")
        close()
    }

    private fun close() {
        dismiss()
    }
}
