package au.com.shiftyjelly.pocketcasts.views.helper

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
