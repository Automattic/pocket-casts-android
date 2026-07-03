package au.com.shiftyjelly.pocketcasts.voicecontrol.benchmark

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.voicecontrol.dialog.PendingVoiceDialog
import au.com.shiftyjelly.pocketcasts.voicecontrol.dialog.VoiceDialogManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.DialogPromptTurn
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.FunctionGemmaInferenceMetrics
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.FunctionGemmaIntentRouter
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.FunctionGemmaMetrics
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.FunctionGemmaPrompt
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.ToolCallMapper
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.LiteRtFunctionGemmaRuntimeFactory
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.MonotonicClock
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FunctionGemmaBenchmark {

    private lateinit var router: FunctionGemmaIntentRouter
    private lateinit var dialogManager: VoiceDialogManager
    private lateinit var modelManager: ModelManager
    private lateinit var metrics: RecordingBenchmarkMetrics
    private lateinit var fixture: List<FixtureCase>
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var routerCreated = false

    private val records = mutableListOf<BenchmarkRecord>()
    private val burstRecords = mutableListOf<BenchmarkRecord>()
    private var actualBackend: FunctionGemmaBackend? = null
    private var modelRelease: String? = null
    private var engineInitMs = 0L
    private var sessionCreateMs = 0L
    private var staticPrefillMs = 0L
    private val memorySamplesMb = mutableListOf<Int>()
    private var resultFile: File? = null

    @Before
    fun setUp() {
        Log.i(TAG, "setUp: starting...")
        val context = ApplicationProvider.getApplicationContext<Context>()
        modelManager = ModelManager(context)

        fixture = loadFixture()
        Log.i(TAG, "setUp: loaded ${fixture.size} fixture cases")

        metrics = RecordingBenchmarkMetrics()
        dialogManager = VoiceDialogManager(ToolCallMapper())
        router = FunctionGemmaIntentRouter(
            dialogManager = dialogManager,
            modelManager = modelManager,
            runtimeFactory = LiteRtFunctionGemmaRuntimeFactory(),
            applicationScope = applicationScope,
            clock = MonotonicClock(SystemClock::elapsedRealtime),
            metrics = metrics,
        )
        routerCreated = true

        val ready = runBlocking {
            modelManager.ensureFunctionGemmaModel()
            router.ensureReady()
        }
        Log.i(TAG, "setUp: router ensureReady=$ready")
        assertTrue("FunctionGemma model must be ready", ready.isSuccess)

        modelRelease = modelManager.functionGemmaReleaseVersion()
        actualBackend = if (metrics.fallbacks.isEmpty()) FunctionGemmaBackend.GPU else FunctionGemmaBackend.CPU
        metrics.preparations.firstOrNull()?.let { prep ->
            engineInitMs = prep.engineInitMs
            sessionCreateMs = prep.sessionCreateMs
            staticPrefillMs = prep.staticPrefillMs
        }
        Log.i(TAG, "setUp: done. backend=$actualBackend release=$modelRelease engineInit=${engineInitMs}ms")
    }

    @After
    fun tearDown() {
        Log.i(TAG, "tearDown")
        if (routerCreated) {
            router.release()
        }
    }

    @Test
    fun runBenchmark() {
        runBlocking {
            Log.i(TAG, "runBenchmark: starting")
            val context = ApplicationProvider.getApplicationContext<Context>()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

            resultFile = File(context.filesDir, "functiongemma_benchmark_result.json")

            logHeader()

            // Phase 1: Warmup (3 requests)
            Log.i(TAG, "phase=warmup count=3")
            for (idx in listOf(0, 3, 7)) {
                Log.i(TAG, "warmup=$idx starting")
                executeRequest(fixture[idx])
                Log.i(TAG, "warmup=$idx done")
                delay(300)
            }
            Log.i(TAG, "phase=warmup done")

            // Phase 2: Benchmark
            val benchmarkCount = minOf(fixture.size, 20)
            Log.i(TAG, "phase=benchmark count=$benchmarkCount")
            for (idx in 0 until benchmarkCount) {
                val case = fixture[idx]
                Log.i(TAG, "req=$idx starting")
                val record = executeMeasuredRequest(idx, case, powerManager)
                records += record
                Log.i(TAG, "req=$idx done totalMs=${record.totalMs} wait=${record.sessionWaitMs} prefill=${record.requestPrefillMs} decode=${record.decodeMs} parse=${record.parseResolveMs} chars=${record.outputChars} backend=${record.backend}")

                if ((idx + 1) % 5 == 0) {
                    sampleMemory()
                    Log.i(TAG, "progress=${idx + 1}/$benchmarkCount")
                }
                delay(500)
            }
            Log.i(TAG, "phase=benchmark done")

            // Phase 3: Burst diagnostic (3 rapid-fire requests)
            Log.i(TAG, "phase=burst count=3")
            for (i in 3..5) {
                val case = fixture[i * 5]
                Log.i(TAG, "burst=$i starting")
                burstRecords += executeMeasuredRequest(-1, case, powerManager)
                Log.i(TAG, "burst=$i done")
            }
            Log.i(TAG, "phase=burst done")

            sampleMemory()
            logResults()
            Log.i(TAG, "runBenchmark: complete")
        }
    }

    private suspend fun executeMeasuredRequest(
        index: Int,
        case: FixtureCase,
        powerManager: PowerManager,
    ): BenchmarkRecord {
        preloadHistory(case.history)
        val result = router.recognize(case.transcript, BENCHMARK_CONTEXT)
        val latestInference = metrics.inferences.lastOrNull()
        val correct = if (latestInference != null) result != null && case.expectedName != null else null

        return BenchmarkRecord(
            index = index,
            transcript = case.transcript,
            expectedName = case.expectedName,
            backend = latestInference?.backend?.name ?: actualBackend?.name ?: "unknown",
            sessionWaitMs = latestInference?.sessionWaitMs ?: 0,
            requestPrefillMs = latestInference?.requestPrefillMs ?: 0,
            decodeMs = latestInference?.decodeMs ?: 0,
            parseResolveMs = latestInference?.parseResolveMs ?: 0,
            totalMs = latestInference?.totalMs ?: 0,
            correct = correct,
            fallbackReason = latestInference?.fallbackReason,
            inputChars = latestInference?.inputCharacters ?: 0,
            outputChars = latestInference?.outputCharacters ?: 0,
            thermalStatus = getThermalStatus(powerManager),
            usedMemoryMb = sampleMemory(),
        )
    }

    private suspend fun executeRequest(case: FixtureCase) {
        preloadHistory(case.history)
        metrics.inferences.clear()
        router.recognize(case.transcript, BENCHMARK_CONTEXT)
    }

    private fun preloadHistory(history: List<DialogPromptTurn>) {
        try {
            val field = VoiceDialogManager::class.java.getDeclaredField("pending")
            field.isAccessible = true
            if (history.isEmpty()) {
                field.set(dialogManager, null)
            } else {
                val pending = PendingVoiceDialog(
                    targetTool = "unknown",
                    targetAction = "unknown",
                    promptHistory = history,
                )
                field.set(dialogManager, pending)
            }
        } catch (_: Exception) {
            Log.w(TAG, "Failed to preload dialog history")
        }
    }

    private fun getThermalStatus(powerManager: PowerManager): Int {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            -1
        }
    }

    private fun sampleMemory(): Int {
        val runtime = Runtime.getRuntime()
        val used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        memorySamplesMb += used.toInt()
        return used.toInt()
    }

    private fun logHeader() {
        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val sdk = android.os.Build.VERSION.SDK_INT
        Log.i(TAG, "meta device=$device sdk=$sdk litertlm=${au.com.shiftyjelly.pocketcasts.voicecontrol.BuildConfig.LITERTLM_VERSION}")
        Log.i(TAG, "meta model_release=$modelRelease backend=$actualBackend")
        Log.i(TAG, "meta engine_init_ms=$engineInitMs session_create_ms=$sessionCreateMs static_prefill_ms=$staticPrefillMs")
    }

    private fun logResults() {
        val sorted = records.map { it.totalMs }.filter { it > 0 }.sorted()
        val burstSorted = burstRecords.map { it.totalMs }.filter { it > 0 }.sorted()

        if (sorted.isEmpty()) {
            Log.e(TAG, "result error=no_valid_inferences")
            return
        }

        val p50 = percentile(sorted, 50)
        val p95 = percentile(sorted, 95)
        val p99 = percentile(sorted, 99)
        val max = sorted.last()
        val min = sorted.first()
        val mean = sorted.average()

        val burstP50 = if (burstSorted.isNotEmpty()) percentile(burstSorted, 50) else -1L
        val burstP95 = if (burstSorted.isNotEmpty()) percentile(burstSorted, 95) else -1L
        val burstMax = if (burstSorted.isNotEmpty()) burstSorted.last() else -1L

        val totalFallbacks = metrics.fallbacks.size
        val gpuInitFallbacks = metrics.fallbacks.count { it == "gpu_init" }
        val gpuInferenceFallbacks = metrics.fallbacks.count { it == "gpu_inference" }
        val totalCorrect = records.count { it.correct == true }
        val totalWithExpected = records.count { it.expectedName != null }
        val accuracy = if (totalWithExpected > 0) totalCorrect.toDouble() / totalWithExpected else -1.0
        val avgMemory = if (memorySamplesMb.isNotEmpty()) memorySamplesMb.average().toInt() else -1
        val peakMemory = if (memorySamplesMb.isNotEmpty()) memorySamplesMb.max() else -1

        Log.i(TAG, "result p50_ms=$p50 p95_ms=$p95 p99_ms=$p99 max_ms=$max min_ms=$min mean_ms=${Math.round(mean)}")
        Log.i(TAG, "burst p50_ms=$burstP50 p95_ms=$burstP95 max_ms=$burstMax")
        Log.i(TAG, "accuracy correct=$totalCorrect/$totalWithExpected ratio=${"%.3f".format(accuracy)}")
        Log.i(TAG, "fallbacks total=$totalFallbacks gpu_init=$gpuInitFallbacks gpu_inference=$gpuInferenceFallbacks")
        Log.i(TAG, "memory avg_mb=$avgMemory peak_mb=$peakMemory")

        val json = buildResultJson(
            device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            modelRelease = modelRelease ?: "unknown",
            actualBackend = actualBackend?.name ?: "unknown",
            p50 = p50, p95 = p95, p99 = p99, max = max, min = min, mean = Math.round(mean),
            burstP50 = burstP50, burstP95 = burstP95, burstMax = burstMax,
            accuracy = accuracy, totalCorrect = totalCorrect, totalWithExpected = totalWithExpected,
            totalFallbacks = totalFallbacks, gpuInitFallbacks = gpuInitFallbacks,
            gpuInferenceFallbacks = gpuInferenceFallbacks,
            avgMemory = avgMemory, peakMemory = peakMemory,
            engineInitMs = engineInitMs, sessionCreateMs = sessionCreateMs,
            staticPrefillMs = staticPrefillMs,
        )
        Log.i(TAG, "result_json=$json")

        // Write to file so we can retrieve it reliably
        resultFile?.let { file ->
            try {
                file.writeText(json)
                Log.i(TAG, "result_file=${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write result file: ${e.message}")
            }
        }
    }

    private fun buildResultJson(
        device: String,
        modelRelease: String,
        actualBackend: String,
        p50: Long,
        p95: Long,
        p99: Long,
        max: Long,
        min: Long,
        mean: Long,
        burstP50: Long,
        burstP95: Long,
        burstMax: Long,
        accuracy: Double,
        totalCorrect: Int,
        totalWithExpected: Int,
        totalFallbacks: Int,
        gpuInitFallbacks: Int,
        gpuInferenceFallbacks: Int,
        avgMemory: Int,
        peakMemory: Int,
        engineInitMs: Long,
        sessionCreateMs: Long,
        staticPrefillMs: Long,
    ): String {
        val json = org.json.JSONObject()
        json.put("device", device)
        json.put("model_release", modelRelease)
        json.put("actual_backend", actualBackend)
        json.put("benchmark_count", records.size)
        json.put("p50_ms", p50)
        json.put("p95_ms", p95)
        json.put("p99_ms", p99)
        json.put("max_ms", max)
        json.put("min_ms", min)
        json.put("mean_ms", mean)
        json.put("burst_p50_ms", burstP50)
        json.put("burst_p95_ms", burstP95)
        json.put("burst_max_ms", burstMax)
        json.put("accuracy", Math.round(accuracy * 1000.0) / 1000.0)
        json.put("correct_count", totalCorrect)
        json.put("expected_count", totalWithExpected)
        json.put("total_fallbacks", totalFallbacks)
        json.put("gpu_init_fallbacks", gpuInitFallbacks)
        json.put("gpu_inference_fallbacks", gpuInferenceFallbacks)
        json.put("avg_memory_mb", avgMemory)
        json.put("peak_memory_mb", peakMemory)
        json.put("engine_init_ms", engineInitMs)
        json.put("session_create_ms", sessionCreateMs)
        json.put("static_prefill_ms", staticPrefillMs)

        val recordsArr = org.json.JSONArray()
        for (r in records) {
            val obj = org.json.JSONObject()
            obj.put("i", r.index)
            obj.put("total_ms", r.totalMs)
            obj.put("backend", r.backend)
            obj.put("correct", r.correct)
            recordsArr.put(obj)
        }
        json.put("per_request", recordsArr)

        return json.toString(2)
    }

    private fun percentile(sorted: List<Long>, p: Int): Long {
        if (sorted.isEmpty()) return -1
        val idx = ((p / 100.0) * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
        return sorted[idx]
    }

    private fun loadFixture(): List<FixtureCase> {
        val context = InstrumentationRegistry.getInstrumentation().context
        val input = context.assets.open(FIXTURE_ASSET)
        val json = InputStreamReader(input).readText()
        input.close()

        val arr = JSONArray(json)
        val cases = mutableListOf<FixtureCase>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val historyArr = obj.optJSONArray("history") ?: JSONArray()
            val history = mutableListOf<DialogPromptTurn>()
            for (j in 0 until historyArr.length()) {
                val turn = historyArr.getJSONObject(j)
                history += DialogPromptTurn(turn.getString("role"), turn.getString("content"))
            }
            cases += FixtureCase(
                history = history,
                transcript = obj.getString("transcript"),
                expectedName = if (obj.has("expected_name")) obj.getString("expected_name") else null,
                expectedAction = if (obj.has("expected_action")) obj.getString("expected_action") else null,
            )
        }
        return cases
    }

    data class FixtureCase(
        val history: List<DialogPromptTurn>,
        val transcript: String,
        val expectedName: String?,
        val expectedAction: String?,
    )

    data class BenchmarkRecord(
        val index: Int,
        val transcript: String,
        val expectedName: String?,
        val backend: String,
        val sessionWaitMs: Long,
        val requestPrefillMs: Long,
        val decodeMs: Long,
        val parseResolveMs: Long,
        val totalMs: Long,
        val correct: Boolean?,
        val fallbackReason: String?,
        val inputChars: Int,
        val outputChars: Int,
        val thermalStatus: Int,
        val usedMemoryMb: Int,
    )

    private class RecordingBenchmarkMetrics : FunctionGemmaMetrics {
        val preparations = mutableListOf<PreparationRecord>()
        val inferences = mutableListOf<FunctionGemmaInferenceMetrics>()
        val fallbacks = mutableListOf<String>()

        override fun prepared(
            backend: FunctionGemmaBackend,
            modelRelease: String,
            engineInitMs: Long,
            sessionCreateMs: Long,
            staticPrefillMs: Long,
        ) {
            preparations += PreparationRecord(backend, modelRelease, engineInitMs, sessionCreateMs, staticPrefillMs)
        }

        override fun inference(metrics: FunctionGemmaInferenceMetrics) {
            inferences += metrics
        }

        override fun backendFallback(reason: String, error: Throwable) {
            fallbacks += reason
        }
    }

    data class PreparationRecord(
        val backend: FunctionGemmaBackend,
        val modelRelease: String,
        val engineInitMs: Long,
        val sessionCreateMs: Long,
        val staticPrefillMs: Long,
    )

    companion object {
        private const val TAG = "FunctionGemmaBenchmark"
        private const val FIXTURE_ASSET = "functiongemma_intent_eval.json"
        private val BENCHMARK_CONTEXT = VoiceRecognitionContext(
            listeningMode = ListeningMode.Continuous,
            micExposure = MicExposure.Isolated,
        )
    }
}
