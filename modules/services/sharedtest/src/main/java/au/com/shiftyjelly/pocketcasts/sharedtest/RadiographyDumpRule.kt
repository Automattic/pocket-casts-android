package au.com.shiftyjelly.pocketcasts.sharedtest

import android.app.Instrumentation
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import radiography.Radiography
import radiography.ScanScope
import radiography.ScanScopes
import radiography.ViewStateRenderers
import java.lang.reflect.Field

private const val TAG = "RadiographyDump"
private val detailMessageField: Field by lazy { Throwable::class.java.getDeclaredField("detailMessage") }

/**
 * Wraps test execution, and on any failure appends a Radiography dump to the thrown exception
 * message while also logging the hierarchy.
 */
class RadiographyDumpRule(
    private val scanScope: ScanScope = ScanScopes.AllWindowsScope,
) : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (error: Throwable) {
                    val instrumentation = runCatching { InstrumentationRegistry.getInstrumentation() }.getOrNull()
                    if (instrumentation != null) {
                        val dump = runCatching { captureHierarchy(instrumentation) }.getOrNull()
                        if (dump != null) {
                            appendDump(error, dump)
                            Log.e(TAG, "UI hierarchy for ${description.displayName} on failure:\n$dump\n")
                        }
                    }
                    throw error
                }
            }
        }
    }

    private fun captureHierarchy(instrumentation: Instrumentation): String {
        var hierarchy = ""
        instrumentation.runOnMainSync {
            hierarchy = Radiography.scan(
                scanScope = scanScope,
                viewStateRenderers = ViewStateRenderers.DefaultsIncludingPii,
            )
        }
        return hierarchy
    }

    private fun appendDump(error: Throwable, dump: String) {
        val field = detailMessageField
        val wasAccessible = field.isAccessible
        runCatching { field.isAccessible = true }
        try {
            val existingMessage = (field.get(error) as? String).orEmpty()
            val sanitizedMessage = existingMessage.substringBefore("\nView Hierarchy:")
            val updatedMessage = buildString {
                append(sanitizedMessage)
                if (isNotEmpty()) append('\n')
                append("View hierarchies:\n")
                append(dump)
            }
            field.set(error, updatedMessage)
        } catch (appendError: Throwable) {
            Log.w(TAG, "Failed to append Radiography dump to exception", appendError)
        } finally {
            runCatching { field.isAccessible = wasAccessible }
        }
    }
}
