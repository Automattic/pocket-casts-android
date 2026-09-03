package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LfmProductPathTest {
    @Test
    fun libsVersionsToml_hasNoLiteRtLmDependency() {
        val toml = repoRoot().resolve("gradle/libs.versions.toml").readText()
        assertFalse("litertlm must be removed from libs.versions.toml", toml.contains("litertlm"))
    }

    @Test
    fun voiceControlModule_doesNotBindFunctionGemmaIntentRouter() {
        val module = voiceModuleFile("di/VoiceControlModule.kt").readText()
        assertFalse(module.contains("FunctionGemmaIntentRouter"))
        assertFalse(module.contains("LiteRtFunctionGemmaRuntimeFactory"))
        assertTrue(module.contains("LfmIntentRouter"))
    }

    @Test
    fun voiceModule_hasNoFunctionGemmaOrLiteRtSources() {
        val intentDir = voiceModuleFile("intent")
        val offenders = intentDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.relativeTo(intentDir).path }
            .filter { path ->
                path.contains("FunctionGemma", ignoreCase = true) ||
                    path.contains("LiteRt", ignoreCase = true)
            }
            .toList()
        assertTrue("unexpected legacy sources: $offenders", offenders.isEmpty())
    }

    @Test
    fun androidTest_hasNoFunctionGemmaBenchmark() {
        val benchmark = repoRoot().resolve(
            "app/src/androidTest/java/au/com/shiftyjelly/pocketcasts/voicecontrol/benchmark/FunctionGemmaBenchmark.kt",
        )
        assertFalse("FunctionGemmaBenchmark must be removed", benchmark.exists())
        val script = repoRoot().resolve("scripts/benchmark_functiongemma.sh")
        assertFalse("benchmark_functiongemma.sh must be removed", script.exists())
        val exporter = repoRoot().resolve("scripts/export_functiongemma_benchmark_fixture.py")
        assertFalse("export_functiongemma_benchmark_fixture.py must be removed", exporter.exists())
    }

    private fun repoRoot(): File {
        val start = File(checkNotNull(System.getProperty("user.dir")) { "user.dir is null" })
        var current: File? = start
        while (current != null) {
            if (File(current, "settings.gradle.kts").exists() || File(current, "settings.gradle").exists()) {
                return current
            }
            current = current.parentFile
        }
        error("Could not locate repository root from $start")
    }

    private fun voiceModuleFile(relativePath: String): File = repoRoot().resolve("modules/services/voice/src/main/kotlin/au/com/shiftyjelly/pocketcasts/voicecontrol/$relativePath")
}
