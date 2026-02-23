package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController

object ActivityBackHelper {

    fun setupNavControllerBack(
        activity: ComponentActivity,
        navController: NavController,
        onAtRoot: (() -> Unit)? = null,
    ) {
        activity.onBackPressedDispatcher.addCallback(
            activity,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!navController.popBackStack()) {
                        onAtRoot?.invoke() ?: activity.finish()
                    }
                }
            },
        )
    }

    fun setupCustomBack(
        activity: ComponentActivity,
        enabled: Boolean = true,
        handler: () -> Boolean,
    ): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                if (!handler()) {
                    activity.finish()
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        return callback
    }

    fun ComponentActivity.setupPredictiveBack(
        navController: NavController,
        onAtRoot: (() -> Unit)? = null,
    ) {
        setupNavControllerBack(
            activity = this,
            navController = navController,
            onAtRoot = onAtRoot,
        )
    }

    fun ComponentActivity.setupPredictiveBack(
        enabled: Boolean = true,
        handler: () -> Boolean,
    ): OnBackPressedCallback {
        return setupCustomBack(
            activity = this,
            enabled = enabled,
            handler = handler,
        )
    }

    /**
     * Sets up predictive back gesture with default animation for the activity's root view.
     *
     * @param handler Called when back is pressed. Return true if handled internally, false to finish activity.
     */
    fun setupPredictiveBackWithAnimation(
        activity: ComponentActivity,
        handler: () -> Boolean = { false },
    ): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                activity.findViewById<View>(android.R.id.content)?.let { rootView ->
                    PredictiveBackAnimator.applyProgress(rootView, backEvent.progress)
                }
            }

            override fun handleOnBackPressed() {
                activity.findViewById<View>(android.R.id.content)?.let { rootView ->
                    PredictiveBackAnimator.animateToEnd(rootView) {
                        if (!handler()) {
                            activity.finish()
                        }
                    }
                } ?: run {
                    if (!handler()) {
                        activity.finish()
                    }
                }
            }

            override fun handleOnBackCancelled() {
                activity.findViewById<View>(android.R.id.content)?.let { rootView ->
                    PredictiveBackAnimator.reset(rootView)
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        return callback
    }

    /**
     * Sets up predictive back gesture with animation for a specific view.
     *
     * @param view The view to animate during back gesture
     * @param handler Called when back is pressed. Return true if handled internally, false to finish activity.
     */
    fun setupPredictiveBackWithAnimation(
        activity: ComponentActivity,
        view: View,
        handler: () -> Boolean = { false },
    ): OnBackPressedCallback {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                PredictiveBackAnimator.applyProgress(view, backEvent.progress)
            }

            override fun handleOnBackPressed() {
                PredictiveBackAnimator.animateToEnd(view) {
                    if (!handler()) {
                        activity.finish()
                    }
                }
            }

            override fun handleOnBackCancelled() {
                PredictiveBackAnimator.reset(view)
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, callback)
        return callback
    }
}

fun ComponentActivity.addNavControllerBackCallback(navController: NavController) {
    ActivityBackHelper.setupNavControllerBack(this, navController)
}

fun ComponentActivity.addBackCallback(
    enabled: Boolean = true,
    handler: () -> Boolean,
): OnBackPressedCallback {
    return ActivityBackHelper.setupCustomBack(this, enabled, handler)
}

/**
 * Extension function to set up predictive back with animation on the activity's root view.
 *
 * @param handler Called when back is pressed. Return true if handled internally, false to finish activity.
 */
fun ComponentActivity.setupPredictiveBackWithAnimation(
    handler: () -> Boolean = { false },
): OnBackPressedCallback {
    return ActivityBackHelper.setupPredictiveBackWithAnimation(this, handler)
}

/**
 * Extension function to set up predictive back with animation on a specific view.
 *
 * @param view The view to animate during back gesture
 * @param handler Called when back is pressed. Return true if handled internally, false to finish activity.
 */
fun ComponentActivity.setupPredictiveBackWithAnimation(
    view: View,
    handler: () -> Boolean = { false },
): OnBackPressedCallback {
    return ActivityBackHelper.setupPredictiveBackWithAnimation(this, view, handler)
}
