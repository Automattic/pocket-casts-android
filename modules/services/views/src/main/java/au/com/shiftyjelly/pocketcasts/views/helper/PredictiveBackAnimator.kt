package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View
import kotlin.math.sqrt

/**
 * Utility for applying predictive back gesture animations to views.
 * Provides standard Material Design animation patterns for back gestures.
 *
 * ## API Level Requirements
 * Predictive back gestures are only available on **Android 13 (API 33) and above**.
 * On older versions:
 * - `handleOnBackProgressed()` callbacks are never invoked
 * - `handleOnBackStarted()` callbacks are never invoked
 * - `handleOnBackCancelled()` callbacks are never invoked
 * - Only `handleOnBackPressed()` is called when back is pressed
 *
 * This gracefully degrades - your animation code won't run on older Android versions,
 * but standard back navigation will still work.
 *
 * ## Usage
 * Call [applyProgress] from `handleOnBackProgressed()` callbacks,
 * call [animateToEnd] from `handleOnBackPressed()` callbacks,
 * and call [reset] from `handleOnBackCancelled()` callbacks.
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

        /** Default scale increase for previous view (2% = starts at 0.98 scale) */
        const val SCALE_AMOUNT_REVERSE = 0.02f

        /** Default alpha increase for previous view (15% = starts at 0.85 alpha) */
        const val ALPHA_AMOUNT_REVERSE = 0.15f

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
     * Uses hardware layer for better performance by caching view rendering on GPU.
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
        // Enable hardware layer on first progress update for better performance
        if (progress > 0f && view.layerType != View.LAYER_TYPE_HARDWARE) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        val scale = 1f - (scaleAmount * progress)
        val alpha = 1f - (alphaAmount * progress)

        view.scaleX = scale
        view.scaleY = scale
        view.alpha = alpha
    }

    /**
     * Applies an inverse scale and fade animation (for revealing previous content).
     *
     * Uses hardware layer for better performance by caching view rendering on GPU.
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
        // Enable hardware layer on first progress update for better performance
        if (progress > 0f && view.layerType != View.LAYER_TYPE_HARDWARE) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        val easedProgress = sqrt(progress)

        val baseScale = 1f - scaleAmount
        val baseAlpha = 1f - alphaAmount

        val scale = baseScale + (scaleAmount * easedProgress)
        val alpha = baseAlpha + (alphaAmount * easedProgress)

        view.scaleX = scale
        view.scaleY = scale
        view.alpha = alpha
    }

    /**
     * Animates the view to a final state, then executes a callback.
     *
     * Uses hardware layer during animation for better performance.
     * Automatically resets view state and disables hardware layer when done.
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
            .withLayer() // Enable hardware layer during animation
            .withEndAction {
                reset(view)
                onEnd()
            }
            .start()
    }

    /**
     * Resets a view to its default state (scale 100%, alpha 100%).
     * Also disables hardware layer to free GPU resources.
     *
     * @param view The view to reset
     */
    fun reset(view: View) {
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
        view.setLayerType(View.LAYER_TYPE_NONE, null)
    }
}
