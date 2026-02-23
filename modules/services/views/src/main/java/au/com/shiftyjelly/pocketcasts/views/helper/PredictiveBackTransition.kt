package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.transition.TransitionValues
import timber.log.Timber

/**
 * Manages AndroidX Transition-based predictive back animations using TransitionSeekController.
 * Use this for more complex animations that require seekable transitions.
 */
class PredictiveBackTransition(
    private val container: ViewGroup,
    private val duration: Long = 300,
) {
    private var seekController: androidx.transition.TransitionSeekController? = null

    /**
     * Starts a seekable transition with scale and fade effects.
     */
    fun start(backEvent: BackEventCompat) {
        val transitionSet = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ScaleTransition())
            addTransition(Fade(Fade.OUT))
            this.duration = this@PredictiveBackTransition.duration
        }

        try {
            seekController = TransitionManager.controlDelayedTransition(container, transitionSet)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start predictive back transition")
        }
    }

    /**
     * Updates the transition progress based on back gesture.
     */
    fun updateProgress(backEvent: BackEventCompat) {
        seekController?.let { controller ->
            if (controller.isReady) {
                controller.currentFraction = backEvent.progress
            }
        }
    }

    /**
     * Completes the transition animation.
     */
    fun finish() {
        try {
            seekController?.animateToEnd()
        } catch (e: Exception) {
            Timber.e(e, "Failed to animate predictive back to end")
        }
    }
}

/**
 * Custom transition that scales down views during predictive back gestures.
 * Scales from 100% to 90% for a subtle preview effect.
 */
private class ScaleTransition : Transition() {
    companion object {
        private const val PROPNAME_SCALE_X = "au.com.shiftyjelly.pocketcasts:scale:scaleX"
        private const val PROPNAME_SCALE_Y = "au.com.shiftyjelly.pocketcasts:scale:scaleY"
        private const val SCALE_END = 0.9f
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues, transitionValues.view.scaleX, transitionValues.view.scaleY)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues, SCALE_END, SCALE_END)
    }

    private fun captureValues(transitionValues: TransitionValues, scaleX: Float, scaleY: Float) {
        transitionValues.values[PROPNAME_SCALE_X] = scaleX
        transitionValues.values[PROPNAME_SCALE_Y] = scaleY
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?,
    ): android.animation.Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val view = endValues.view
        val startScaleX = startValues.values[PROPNAME_SCALE_X] as Float
        val startScaleY = startValues.values[PROPNAME_SCALE_Y] as Float
        val endScaleX = endValues.values[PROPNAME_SCALE_X] as Float
        val endScaleY = endValues.values[PROPNAME_SCALE_Y] as Float

        view.scaleX = startScaleX
        view.scaleY = startScaleY

        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            view.scaleX = startScaleX + (endScaleX - startScaleX) * fraction
            view.scaleY = startScaleY + (endScaleY - startScaleY) * fraction
        }

        return animator
    }
}
