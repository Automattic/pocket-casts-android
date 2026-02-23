package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View

/**
 * Utility for applying predictive back gesture animations to views.
 * Provides standard Material Design animation patterns for back gestures.
 */
object PredictiveBackAnimator {
    /**
     * Applies a scale and fade animation during back gesture progress.
     *
     * @param view The view to animate
     * @param progress Back gesture progress (0.0 to 1.0)
     * @param scaleAmount How much to scale down (e.g., 0.1f scales from 100% to 90%)
     * @param alphaAmount How much to fade (e.g., 0.3f fades from 100% to 70%)
     */
    fun applyProgress(
        view: View,
        progress: Float,
        scaleAmount: Float = 0.1f,
        alphaAmount: Float = 0.3f,
    ) {
        val scale = 1f - (scaleAmount * progress)
        val alpha = 1f - (alphaAmount * progress)

        view.scaleX = scale
        view.scaleY = scale
        view.alpha = alpha
    }

    /**
     * Applies an inverse scale and fade animation (for revealing previous content).
     *
     * @param view The view to animate
     * @param progress Back gesture progress (0.0 to 1.0)
     * @param scaleAmount How much to scale up (e.g., 0.05f scales from 95% to 100%)
     * @param alphaAmount How much to fade in (e.g., 0.5f fades from 50% to 100%)
     */
    fun applyProgressReverse(
        view: View,
        progress: Float,
        scaleAmount: Float = 0.05f,
        alphaAmount: Float = 0.5f,
    ) {
        val baseScale = 1f - scaleAmount
        val baseAlpha = 1f - alphaAmount

        val scale = baseScale + (scaleAmount * progress)
        val alpha = baseAlpha + (alphaAmount * progress)

        view.scaleX = scale
        view.scaleY = scale
        view.alpha = alpha
    }

    /**
     * Animates the view to a final state, then executes a callback.
     *
     * @param view The view to animate
     * @param targetScale Target scale (e.g., 0.8f for 80%)
     * @param targetAlpha Target alpha (e.g., 0f for fully transparent)
     * @param duration Animation duration in milliseconds
     * @param onEnd Callback executed when animation completes
     */
    fun animateToEnd(
        view: View,
        targetScale: Float = 0.85f,
        targetAlpha: Float = 0f,
        duration: Long = 150,
        onEnd: () -> Unit,
    ) {
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .alpha(targetAlpha)
            .setDuration(duration)
            .withEndAction {
                reset(view)
                onEnd()
            }
            .start()
    }

    /**
     * Resets a view to its default state (scale 100%, alpha 100%).
     *
     * @param view The view to reset
     */
    fun reset(view: View) {
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
    }
}
