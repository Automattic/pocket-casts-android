package au.com.shiftyjelly.pocketcasts.sharedtest

import android.app.Instrumentation
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.base.DefaultFailureHandler
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import radiography.Radiography
import radiography.ScanScopes
import radiography.ViewStateRenderers
import java.lang.reflect.Field
import org.hamcrest.Matcher
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener

private const val TAG = "RadiographyDump"
private const val ARG_LISTENER = "listener"
private val detailMessageField: Field by lazy { Throwable::class.java.getDeclaredField("detailMessage") }

class PocketCastsTestRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        val updatedArguments = addRadiographyRunListener(arguments)
        super.onCreate(updatedArguments)
        installRadiographyFailureHandler()
    }

    override fun onException(obj: Any?, e: Throwable?): Boolean {
        val throwable = e ?: return super.onException(obj, e)
        runCatching {
            appendRadiographyToFailure(InstrumentationRegistry.getInstrumentation(), throwable)
        }.onFailure { error ->
            Log.w(TAG, "Unable to capture hierarchy for uncaught exception", error)
        }
        return super.onException(obj, throwable)
    }

    private fun addRadiographyRunListener(arguments: Bundle?): Bundle {
        val updatedArguments = Bundle(arguments ?: Bundle())
        val listenerClassName = RadiographyFailureListener::class.java.name
        val existingListeners = updatedArguments.getString(ARG_LISTENER)
        val mergedListeners = if (existingListeners.isNullOrBlank()) {
            listenerClassName
        } else {
            "$existingListeners,$listenerClassName"
        }
        updatedArguments.putString(ARG_LISTENER, mergedListeners)
        return updatedArguments
    }

    private fun installRadiographyFailureHandler() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val defaultFailureHandler = DefaultFailureHandler(instrumentation.targetContext)
        Espresso.setFailureHandler { error: Throwable, viewMatcher: Matcher<View>? ->
            try {
                defaultFailureHandler.handle(error, viewMatcher)
            } catch (decoratedError: Throwable) {
                appendRadiographyToFailure(instrumentation, decoratedError)
                throw decoratedError
            }
        }
    }
}

class RadiographyFailureListener : RunListener() {

    override fun testFailure(failure: Failure) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val description = failure.description
        val dump = runCatching { captureHierarchy(instrumentation) }
            .getOrElse { error ->
                Log.e(TAG, "Unable to capture UI hierarchy for ${description.displayName}", error)
                return
            }

        Log.e(TAG, "UI hierarchy for ${description.displayName} on failure:\n$dump\n")
    }
}

private fun appendRadiographyToFailure(
    instrumentation: Instrumentation,
    decoratedError: Throwable,
) {
    val messageField = detailMessageField
    val wasAccessible = messageField.isAccessible
    runCatching { messageField.isAccessible = true }
    try {
        val existingMessage = (messageField.get(decoratedError) as? String).orEmpty()
        val sanitizedMessage = existingMessage.substringBefore("\nView Hierarchy:")
        val hierarchy = captureHierarchy(instrumentation)
        val updatedMessage = buildString {
            append(sanitizedMessage)
            append("\nView hierarchies:\n")
            append(hierarchy)
        }
        messageField.set(decoratedError, updatedMessage)
    } catch (error: Throwable) {
        Log.w(TAG, "Failed to append Radiography hierarchy to failure", error)
    } finally {
        runCatching { messageField.isAccessible = wasAccessible }
    }
}

private fun captureHierarchy(instrumentation: Instrumentation): String {
    var hierarchy = ""
    instrumentation.runOnMainSync {
        hierarchy = Radiography.scan(
            scanScope = ScanScopes.AllWindowsScope,
            viewStateRenderers = ViewStateRenderers.DefaultsIncludingPii,
        )
    }
    return hierarchy
}
