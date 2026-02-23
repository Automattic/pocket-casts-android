package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View

/**
 * Utility for applying predictive back gesture animations to views.
 * Provides standard Material Design animation patterns for back gestures.
 */
object PredictiveBackAnimator {
    /**
     * Default animation constants for predictive back gestures.
     * These values follow Material Design guidelines for smooth, intuitive back navigation.
     */
    object Defaults {
        /** Default scale reduction for current view (10% = 0.9 scale) */
        const val SCALE_AMOUNT = 0.1f

        /** Default alpha reduction for current view (30% = 0.7 alpha) */
        const val ALPHA_AMOUNT = 0.3f

        /** Default scale increase for previous view (5% = starts at 0.95 scale) */
        const val SCALE_AMOUNT_REVERSE = 0.05f

        /** Default alpha increase for previous view (50% = starts at 0.5 alpha) */
        const val ALPHA_AMOUNT_REVERSE = 0.5f

        /** Default target scale for animation end (85%) */
        const val TARGET_SCALE = 0.85f

        /** Default target alpha for animation end (fully transparent) */
        const val TARGET_ALPHA = 0f

        /** Default animation duration in milliseconds */
        const val ANIMATION_DURATION_MS = 150L

        /** Short animation duration for quick transitions */
        const val SHORT_ANIMATION_DURATION_MS = 100L
    }

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
        scaleAmount: Float = Defaults.SCALE_AMOUNT,
        alphaAmount: Float = Defaults.ALPHA_AMOUNT,
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
        scaleAmount: Float = Defaults.SCALE_AMOUNT_REVERSE,
        alphaAmount: Float = Defaults.ALPHA_AMOUNT_REVERSE,
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
        targetScale: Float = Defaults.TARGET_SCALE,
        targetAlpha: Float = Defaults.TARGET_ALPHA,
        duration: Long = Defaults.ANIMATION_DURATION_MS,
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
