// WakeWordJni.cpp — openWakeWord-style wake word detection via ONNX Runtime.
//
// Runs a 3-stage ONNX pipeline against libonnxruntime.so (already loaded by
// the Moonshine Voice SDK), using dlsym to resolve OrtGetApiBase at runtime.
//
// Pipeline:
//   1. Mel spectrogram  — raw PCM → mel filterbank features (melspectrogram.onnx)
//   2. Speech embedding — mel frames → 96-dim embedding (embedding_model.onnx)
//   3. Wake word classifier — 16 embeddings → sigmoid score (auris.onnx)
//
// Each detect() call prepends virtual leading context and scores only the
// onset-eligible classifier windows. State is fully reset between calls.

#include <jni.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdint>
#include <cstring>
#include <vector>
#include <mutex>
#include <algorithm>

#include "onnxruntime_c_api.h"

#define LOG_TAG "NativeWakeWord"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// --- ORT function pointer types ---
typedef const OrtApiBase* (*FnOrtGetApiBase)(void);

// --- Model parameters ---
static constexpr int      kSampleRateHz          = 16000;
static constexpr int      kMelWindowSamples      = 1280;   // debug log-Mel minimum
static constexpr int      kMelBins               = 32;
static constexpr int      kEmbeddingDim          = 96;
static constexpr int      kMaxEmbeddings         = 16;     // classifier context window
static constexpr int      kVirtualContextSamples = 32000;  // 2s before VAD onset
static constexpr int      kOnsetCutoffSamples    = 32000;  // last eligible endpoint after onset
static constexpr int      kMelFrameStrideSamples = 160;
static constexpr int      kEmbeddingWindowFrames = 76;
static constexpr int      kEmbeddingStrideFrames = 8;
static constexpr int      kEmbeddingStrideSamples =
    kEmbeddingStrideFrames * kMelFrameStrideSamples;
static constexpr int      kEmbeddingEndpointOffsetSamples =
    kEmbeddingWindowFrames * kMelFrameStrideSamples;

// --- Global ORT state ---
static const OrtApi*      g_ort = nullptr;
static OrtEnv*            g_env = nullptr;
static OrtMemoryInfo*     g_memInfo = nullptr;
static std::mutex         g_mutex;

// --- Per-stage session ---
static OrtSession*        g_melSession = nullptr;
static OrtSession*        g_embedSession = nullptr;
static OrtSession*        g_clsSession = nullptr;

static float              g_threshold = 0.5f;
static bool               g_initialized = false;

// --- Helpers ---

static void releaseSession(OrtSession*& sess) {
    if (sess) { g_ort->ReleaseSession(sess); sess = nullptr; }
}

static void cleanupOrt() {
    std::lock_guard<std::mutex> lock(g_mutex);
    releaseSession(g_clsSession);
    releaseSession(g_embedSession);
    releaseSession(g_melSession);
    if (g_memInfo) { g_ort->ReleaseMemoryInfo(g_memInfo); g_memInfo = nullptr; }
    if (g_env)     { g_ort->ReleaseEnv(g_env); g_env = nullptr; }
    g_ort = nullptr;
    g_initialized = false;
}

static bool resolveOrt() {
    if (g_ort) return true;

    void* ortLib = dlopen("libonnxruntime.so", RTLD_NOLOAD);
    if (!ortLib) { LOGE("libonnxruntime.so not loaded"); return false; }

    auto fnGetApiBase = (FnOrtGetApiBase)dlsym(ortLib, "OrtGetApiBase");
    if (!fnGetApiBase) { LOGE("OrtGetApiBase not found"); return false; }

    g_ort = fnGetApiBase()->GetApi(ORT_API_VERSION);
    if (!g_ort) { LOGE("GetApi failed"); return false; }

    return true;
}

static OrtSession* createSessionFromBytes(const uint8_t* data, size_t size) {
    if (!g_env) {
        OrtStatus* st = g_ort->CreateEnv(ORT_LOGGING_LEVEL_WARNING, "NativeWakeWord", &g_env);
        if (st) { LOGE("CreateEnv: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st); return nullptr; }
    }
    if (!g_memInfo) {
        OrtStatus* st = g_ort->CreateCpuMemoryInfo(OrtArenaAllocator, OrtMemTypeDefault, &g_memInfo);
        if (st) { LOGE("CreateCpuMemoryInfo: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st); return nullptr; }
    }

    OrtSession* sess = nullptr;
    OrtStatus* st = g_ort->CreateSessionFromArray(g_env, data, size, nullptr, &sess);
    if (st) { LOGE("CreateSession: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st); return nullptr; }
    return sess;
}

// Create a float tensor from a pre-filled buffer.
static OrtValue* createFloatTensor(const float* data, const int64_t* shape, size_t rank) {
    OrtValue* tensor = nullptr;
    size_t count = 1;
    for (size_t i = 0; i < rank; i++) count *= (size_t)shape[i];
    OrtStatus* st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, (void*)data, count * sizeof(float), shape, rank,
        ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &tensor);
    if (st) { LOGE("CreateTensor: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st); return nullptr; }
    return tensor;
}

// Run a single-input, single-output ONNX model.
static std::vector<float> runModel(OrtSession* sess, const char* inputName, const char* outputName,
                                   const float* inputData, const int64_t* inputShape, size_t inputRank,
                                   size_t expectedOutputSize, bool& ok) {
    ok = false;
    std::vector<float> result;

    OrtValue* inputTensor = createFloatTensor(inputData, inputShape, inputRank);
    if (!inputTensor) return result;

    OrtValue* outputTensor = nullptr;
    OrtStatus* st = g_ort->Run(sess, nullptr, &inputName, &inputTensor, 1, &outputName, 1, &outputTensor);
    g_ort->ReleaseValue(inputTensor);
    if (st) { LOGE("Run %s: %s", outputName, g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st); return result; }

    // Get output data
    float* outputData = nullptr;
    st = g_ort->GetTensorMutableData(outputTensor, (void**)&outputData);
    if (st) { LOGE("GetTensorData: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st); g_ort->ReleaseValue(outputTensor); return result; }

    result.assign(outputData, outputData + expectedOutputSize);
    g_ort->ReleaseValue(outputTensor);
    ok = true;
    return result;
}

// --- JNI ---

extern "C" {

JNIEXPORT jboolean JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_wakeword_WakeWordJni_nativeInit(
    JNIEnv* env, jclass /*clazz*/,
    jbyteArray jMelModel, jbyteArray jEmbedModel, jbyteArray jClassifierModel,
    jfloat threshold)
{
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_initialized) return JNI_TRUE;

    if (!resolveOrt()) return JNI_FALSE;

    g_threshold = (float)threshold;

    auto loadBytes = [&](jbyteArray arr, const uint8_t*& data, size_t& size) -> bool {
        size = (size_t)env->GetArrayLength(arr);
        data = (const uint8_t*)env->GetByteArrayElements(arr, nullptr);
        return data != nullptr;
    };

    const uint8_t* melData = nullptr;   size_t melSize = 0;
    const uint8_t* embedData = nullptr; size_t embedSize = 0;
    const uint8_t* clsData = nullptr;   size_t clsSize = 0;

    bool ok = loadBytes(jMelModel, melData, melSize) &&
              loadBytes(jEmbedModel, embedData, embedSize) &&
              loadBytes(jClassifierModel, clsData, clsSize);

    if (!ok) {
        LOGE("Failed to get model bytes");
        if (melData) env->ReleaseByteArrayElements(jMelModel, (jbyte*)melData, JNI_ABORT);
        return JNI_FALSE;
    }

    g_melSession   = createSessionFromBytes(melData, melSize);
    g_embedSession = createSessionFromBytes(embedData, embedSize);
    g_clsSession   = createSessionFromBytes(clsData, clsSize);

    env->ReleaseByteArrayElements(jMelModel, (jbyte*)melData, JNI_ABORT);
    env->ReleaseByteArrayElements(jEmbedModel, (jbyte*)embedData, JNI_ABORT);
    env->ReleaseByteArrayElements(jClassifierModel, (jbyte*)clsData, JNI_ABORT);

    if (!g_melSession || !g_embedSession || !g_clsSession) {
        LOGE("Failed to create one or more ONNX sessions");
        cleanupOrt();
        return JNI_FALSE;
    }

    g_initialized = true;
    LOGI("Wake word detector initialized (threshold=%.3f)", g_threshold);
    return JNI_TRUE;
}

JNIEXPORT jfloat JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_wakeword_WakeWordJni_nativeDetect(
    JNIEnv* env, jclass /*clazz*/, jfloatArray jSamples, jint sampleRateHz, jfloatArray jOutOffset)
{
    if (!g_initialized) return -1.0f;
    if (sampleRateHz != kSampleRateHz) return -1.0f;

    // Default: no offset found
    if (jOutOffset) {
        jfloat* outOffset = env->GetFloatArrayElements(jOutOffset, nullptr);
        if (outOffset) outOffset[0] = -1.0f;
        if (outOffset) env->ReleaseFloatArrayElements(jOutOffset, outOffset, 0);
    }

    jsize numSamples = env->GetArrayLength(jSamples);
    if (numSamples <= 0) return 0.0f;

    jfloat* samples = env->GetFloatArrayElements(jSamples, nullptr);
    if (!samples) return -1.0f;

    // --- Stage 1: prepend virtual context and run mel once ---
    // Training uses normalized [-1,1] audio directly — do NOT multiply by 32768.
    // The virtual samples exist only inside the detector and never reach ASR.
    std::vector<float> audioIn(kVirtualContextSamples + numSamples, 0.0f);
    std::copy(samples, samples + numSamples, audioIn.begin() + kVirtualContextSamples);

    // Run mel model on full audio. Output shape: (1, 1, time_frames, 32)
    int64_t melInShape[] = {1, (int64_t)audioIn.size()};
    // We don't know the output size ahead of time — use the variable-size helper.
    OrtValue* melInputTensor = nullptr;
    size_t melCount = audioIn.size();
    OrtStatus* st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, audioIn.data(), melCount * sizeof(float),
        melInShape, 2, ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &melInputTensor);
    if (st) { LOGE("Create mel tensor: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st);
        env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT); return -1.0f; }

    const char* melInputName = "input";
    const char* melOutputName = "output";
    OrtValue* melOutputTensor = nullptr;
    st = g_ort->Run(g_melSession, nullptr, &melInputName, &melInputTensor, 1,
                    &melOutputName, 1, &melOutputTensor);
    g_ort->ReleaseValue(melInputTensor);
    if (st) { LOGE("Run mel: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st);
        env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT); return -1.0f; }

    // Get mel output info
    OrtTensorTypeAndShapeInfo* melInfo = nullptr;
    st = g_ort->GetTensorTypeAndShape(melOutputTensor, &melInfo);
    if (st) { LOGE("Get mel info: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st);
        g_ort->ReleaseValue(melOutputTensor); env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT); return -1.0f; }

    size_t melNumDims = 0;
    if (OrtStatus* st2 = g_ort->GetDimensionsCount(melInfo, &melNumDims)) {
        LOGE("GetDimensionsCount: %s", g_ort->GetErrorMessage(st2)); g_ort->ReleaseStatus(st2);
    }
    std::vector<int64_t> melShape(melNumDims);
    if (OrtStatus* st2 = g_ort->GetDimensions(melInfo, melShape.data(), melNumDims)) {
        LOGE("GetDimensions: %s", g_ort->GetErrorMessage(st2)); g_ort->ReleaseStatus(st2);
    }
    size_t melNumElements = 0;
    if (OrtStatus* st2 = g_ort->GetTensorShapeElementCount(melInfo, &melNumElements)) {
        LOGE("GetTensorShapeElementCount: %s", g_ort->GetErrorMessage(st2)); g_ort->ReleaseStatus(st2);
    }
    g_ort->ReleaseTensorTypeAndShapeInfo(melInfo);

    float* melData = nullptr;
    st = g_ort->GetTensorMutableData(melOutputTensor, (void**)&melData);
    if (st) { LOGE("Get mel data: %s", g_ort->GetErrorMessage(st)); g_ort->ReleaseStatus(st);
        g_ort->ReleaseValue(melOutputTensor); env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT); return -1.0f; }

    // Apply transform: mel = mel / 10 + 2. Output is (1, 1, time_frames, 32).
    int nMelFrames = (melNumDims >= 3) ? (int)melShape[2] : 0;
    std::vector<float> melFrames(melData, melData + melNumElements);
    for (float& v : melFrames) { v = v / 10.0f + 2.0f; }
    g_ort->ReleaseValue(melOutputTensor);

    LOGI("Mel: %d frames, %zu dims, %zu elements from %zu detector samples",
         nMelFrames, melNumDims, melNumElements, audioIn.size());

    // --- Stage 2: Extract embeddings with stride 8, 76-frame window ---
    int nEmbeddings =
        (nMelFrames - kEmbeddingWindowFrames) / kEmbeddingStrideFrames + 1;
    if (nEmbeddings < 0) nEmbeddings = 0;

    std::vector<float> embeddings;  // (nEmbeddings, 96) flattened
    embeddings.reserve(nEmbeddings * kEmbeddingDim);

    for (int i = 0; i < nEmbeddings; i++) {
        int melStart = i * kEmbeddingStrideFrames;
        // Build input: (1, 76, 32, 1) from melFrames[melStart:melStart+76, :]
        std::vector<float> melWindowInput;
        melWindowInput.reserve(kEmbeddingWindowFrames * kMelBins);
        for (int f = 0; f < kEmbeddingWindowFrames; f++) {
            int frameOffset = (melStart + f) * kMelBins;
            melWindowInput.insert(melWindowInput.end(),
                                  melFrames.begin() + frameOffset,
                                  melFrames.begin() + frameOffset + kMelBins);
        }
        int64_t embedInShape[] = {1, kEmbeddingWindowFrames, kMelBins, 1};
        bool embedOk = false;
        std::vector<float> embedOut = runModel(g_embedSession, "input_1", "conv2d_19",
                                                melWindowInput.data(), embedInShape, 4,
                                                (size_t)kEmbeddingDim, embedOk);
        if (!embedOk) { env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT); return -1.0f; }
        embeddings.insert(embeddings.end(), embedOut.begin(), embedOut.end());
    }

    LOGI("Extracted %d embeddings from %d mel frames", nEmbeddings, nMelFrames);

    // --- Stage 3: score dense windows whose endpoints remain inside the onset gate ---
    // For classifier window start w, the final embedding index is w+15 and its
    // waveform endpoint relative to speech onset is:
    //   76*160 + 8*160*(w+15) - virtualContext.
    // With the frozen 2s context/cutoff this admits starts 0...25 (26 windows).
    int totalWindows = nEmbeddings - kMaxEmbeddings + 1;
    if (totalWindows < 1) {
        LOGE("Virtual context produced only %d embeddings", nEmbeddings);
        env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT);
        return -1.0f;
    }
    int maxLastEmbeddingIndex =
        (kVirtualContextSamples + kOnsetCutoffSamples -
         kEmbeddingEndpointOffsetSamples) /
        kEmbeddingStrideSamples;
    int maxEligibleWindowStart = maxLastEmbeddingIndex - (kMaxEmbeddings - 1);
    int numWindows = std::min(totalWindows, maxEligibleWindowStart + 1);
    if (numWindows < 1) {
        env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT);
        return 0.0f;
    }
    int64_t clsInShape[] = {1, kMaxEmbeddings, kEmbeddingDim};

    float maxScore = 0.0f;
    for (int w = 0; w < numWindows; w++) {
        float clsInputData[kMaxEmbeddings * kEmbeddingDim] = {};
        int srcStart = w * kEmbeddingDim;
        for (int i = 0; i < kMaxEmbeddings * kEmbeddingDim; i++) {
            clsInputData[i] = embeddings[srcStart + i];
        }

        bool clsOk = false;
        std::vector<float> clsOut = runModel(g_clsSession, "embeddings", "score",
                                             clsInputData, clsInShape, 3, 1, clsOk);
        if (!clsOk) {
            env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT);
            return -1.0f;
        }
        if (clsOut[0] > maxScore) {
            maxScore = clsOut[0];
        }
    }

    // Onset-eligible classifier windows begin inside virtual context, so there
    // is no valid non-negative window-start offset to return to the VAD segment.
    // Keeping the default -1 makes command extraction use the full segment.
    LOGI("Scored %d/%d onset-eligible classifier windows", numWindows, totalWindows);

    env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT);
    return maxScore;
}

// Debug-only: return the log-Mel matrix for a segment as a flat float array
// of shape (time_frames * kMelBins), same transform as nativeDetect
// (v/10 + 2). Used to render a per-segment log-Mel spectrogram PNG for
// diagnosing wake-word false activations. Returns null on error.
JNIEXPORT jfloatArray JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_wakeword_WakeWordJni_nativeGetLogMel(
    JNIEnv* env, jclass /*clazz*/, jfloatArray jSamples, jint sampleRateHz)
{
    if (!g_initialized) return nullptr;
    if (sampleRateHz != kSampleRateHz) return nullptr;

    jsize numSamples = env->GetArrayLength(jSamples);
    if (numSamples < kMelWindowSamples) return nullptr;

    jfloat* samples = env->GetFloatArrayElements(jSamples, nullptr);
    if (!samples) return nullptr;

    std::vector<float> audioIn(numSamples);
    for (int i = 0; i < numSamples; i++) {
        audioIn[i] = samples[i];
    }
    env->ReleaseFloatArrayElements(jSamples, samples, JNI_ABORT);

    // --- Run mel model on full audio, mirroring nativeDetect ---
    int64_t melInShape[] = {1, (int64_t)numSamples};
    OrtValue* melInputTensor = nullptr;
    OrtStatus* st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, audioIn.data(), numSamples * sizeof(float),
        melInShape, 2, ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &melInputTensor);
    if (st) { g_ort->ReleaseStatus(st); return nullptr; }

    const char* melInputName = "input";
    const char* melOutputName = "output";
    OrtValue* melOutputTensor = nullptr;
    st = g_ort->Run(g_melSession, nullptr, &melInputName, &melInputTensor, 1,
                    &melOutputName, 1, &melOutputTensor);
    g_ort->ReleaseValue(melInputTensor);
    if (st) { g_ort->ReleaseStatus(st); return nullptr; }

    float* melData = nullptr;
    st = g_ort->GetTensorMutableData(melOutputTensor, (void**)&melData);
    if (st) {
        g_ort->ReleaseStatus(st);
        g_ort->ReleaseValue(melOutputTensor);
        return nullptr;
    }

    // Apply the same log-Mel transform as nativeDetect: mel = mel/10 + 2.
    OrtTensorTypeAndShapeInfo* melInfo = nullptr;
    size_t melNumElements = 0;
    if (OrtStatus* infoSt = g_ort->GetTensorTypeAndShape(melOutputTensor, &melInfo)) {
        g_ort->ReleaseStatus(infoSt);
        g_ort->ReleaseValue(melOutputTensor);
        return nullptr;
    }
    if (OrtStatus* countSt = g_ort->GetTensorShapeElementCount(melInfo, &melNumElements)) {
        g_ort->ReleaseStatus(countSt);
        g_ort->ReleaseTensorTypeAndShapeInfo(melInfo);
        g_ort->ReleaseValue(melOutputTensor);
        return nullptr;
    }
    g_ort->ReleaseTensorTypeAndShapeInfo(melInfo);

    jfloatArray result = env->NewFloatArray((jsize)melNumElements);
    if (result) {
        // Copy with the transform applied.
        std::vector<jfloat> transformed(melNumElements);
        for (size_t i = 0; i < melNumElements; i++) {
            transformed[i] = melData[i] / 10.0f + 2.0f;
        }
        env->SetFloatArrayRegion(result, 0, (jsize)melNumElements, transformed.data());
    }
    g_ort->ReleaseValue(melOutputTensor);
    return result;
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_wakeword_WakeWordJni_nativeRelease(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    cleanupOrt();
    LOGI("Wake word detector released");
}

} // extern "C"
