// VadJni.cpp — Silero VAD via Moonshine's ONNX Runtime C API.
//
// Runs the Silero VAD model (silero_vad.onnx) directly against
// libonnxruntime.so (bundled by Moonshine Voice SDK), avoiding the
// Java ONNX API and its symbol-version conflicts.
//
// Uses dlsym to resolve OrtGetApiBase at runtime since we can't
// link against the prebuilt libonnxruntime.so at build time.
//
// The onnxruntime_c_*.h headers committed in this directory are
// build-time-only C API declarations. The actual .so comes from
// ai.moonshine:moonshine-voice, so these headers must match whatever
// ONNX Runtime version that AAR bundles. We commit them directly
// rather than pulling via Maven/FetchContent to avoid .so conflicts
// and to guarantee ABI compatibility with Moonshine's build.

#include <jni.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdint>
#include <vector>
#include <string>
#include <cstring>
#include <mutex>

#include "onnxruntime_c_api.h"

#define LOG_TAG "NativeVAD"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Function pointer types we resolve from libonnxruntime.so
typedef const OrtApiBase* (*FnOrtGetApiBase)(void);

// ---------------------------------------------------------------------------
// Global ONNX Runtime state — lazy-init, thread-safe
// ---------------------------------------------------------------------------
static const OrtApi*        g_ort = nullptr;
static OrtEnv*              g_env = nullptr;
static OrtSession*          g_session = nullptr;
static OrtMemoryInfo*       g_memInfo = nullptr;
static std::once_flag       g_initFlag;
static std::string          g_errorMsg;

static constexpr int64_t    kInputShape[]  = {1, 1024};  // 64ms @ 16kHz
static constexpr size_t     kInputSize     = 1024;

// Silero VAD LSTM state: [2, 1, 64] — 2 layers, batch 1, 64 units each.
static constexpr int64_t    kStateShape[]  = {2, 1, 64};
static constexpr size_t     kStateSize     = 2 * 1 * 64; // 128 floats
static float                g_hState[kStateSize] = {}; // hidden state, zero-init
static float                g_cState[kStateSize] = {}; // cell state, zero-init

void sileroVadResetState() {
    std::memset(g_hState, 0, sizeof(g_hState));
    std::memset(g_cState, 0, sizeof(g_cState));
}

static void cleanupOrt() {
    if (g_session)  { g_ort->ReleaseSession(g_session); g_session = nullptr; }
    if (g_memInfo)  { g_ort->ReleaseMemoryInfo(g_memInfo); g_memInfo = nullptr; }
    if (g_env)      { g_ort->ReleaseEnv(g_env); g_env = nullptr; }
    g_ort = nullptr;
}

static bool initOrt(JNIEnv* env, jobject assetManager) {
    if (g_session) return true;

    // libonnxruntime.so is already loaded by Moonshine's Transcriber.
    // Resolve ORT from onnxruntime-android (loaded by System.loadLibrary).
    void* ortLib = dlopen("libonnxruntime.so", RTLD_NOLOAD);
    if (!ortLib) { g_errorMsg = "libonnxruntime.so not loaded"; return false; }

    auto fnGetApiBase = (FnOrtGetApiBase)dlsym(ortLib, "OrtGetApiBase");
    if (!fnGetApiBase) { g_errorMsg = "OrtGetApiBase not found"; dlclose(ortLib); return false; }

    g_ort = fnGetApiBase()->GetApi(ORT_API_VERSION);
    if (!g_ort) { g_errorMsg = "GetApi failed"; dlclose(ortLib); return false; }

    // Create env with CPU execution provider.
    OrtStatus* st = g_ort->CreateEnv(ORT_LOGGING_LEVEL_WARNING, "NativeVAD", &g_env);
    if (st) { g_errorMsg = g_ort->GetErrorMessage(st); g_ort->ReleaseStatus(st); cleanupOrt(); return false; }

    // Load model from assets.
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if (!mgr) { g_errorMsg = "AAssetManager_fromJava null"; cleanupOrt(); return false; }

    AAsset* asset = AAssetManager_open(mgr, "silero_vad.onnx", AASSET_MODE_BUFFER);
    if (!asset) { g_errorMsg = "Cannot open silero_vad.onnx from assets"; cleanupOrt(); return false; }

    const void* modelData = AAsset_getBuffer(asset);
    off_t modelSize = AAsset_getLength(asset);

    st = g_ort->CreateSessionFromArray(g_env, modelData, (size_t)modelSize, nullptr, &g_session);
    AAsset_close(asset);

    if (st) { g_errorMsg = g_ort->GetErrorMessage(st); g_ort->ReleaseStatus(st); cleanupOrt(); return false; }

    // Create memory info for CPU allocations.
    st = g_ort->CreateCpuMemoryInfo(OrtArenaAllocator, OrtMemTypeDefault, &g_memInfo);
    if (st) { g_errorMsg = g_ort->GetErrorMessage(st); g_ort->ReleaseStatus(st); cleanupOrt(); return false; }

    LOGI("Silero VAD session created");
    return true;
}

// ---------------------------------------------------------------------------
// Core VAD inference — reusable by NativeVadProcessor (no JNI dependency)
// ---------------------------------------------------------------------------
float sileroVadPredict(const int16_t* samples, int32_t count) {
    if (!g_session || count < static_cast<int32_t>(kInputSize)) return 0.0f;

    // Build float input tensor [1, 1024].
    std::vector<float> inputData(kInputSize);
    for (size_t i = 0; i < kInputSize; i++) {
        inputData[i] = static_cast<float>(samples[i]) / 32768.0f;
    }

    // Create input tensor.
    OrtValue* inputTensor = nullptr;
    OrtStatus* st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo,
        inputData.data(),
        inputData.size() * sizeof(float),
        kInputShape, 2,
        ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT,
        &inputTensor);
    if (st) { g_ort->ReleaseStatus(st); return 0.0f; }

    // Create sample-rate scalar input [1].
    int64_t srData[] = {16000};
    int64_t srShape[] = {1};
    OrtValue* srTensor = nullptr;
    st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, srData, sizeof(srData),
        srShape, 1, ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64, &srTensor);
    if (st) { g_ort->ReleaseStatus(st); g_ort->ReleaseValue(inputTensor); return 0.0f; }

    // Create h-state tensor [2, 1, 64].
    OrtValue* hTensor = nullptr;
    st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, g_hState, kStateSize * sizeof(float),
        kStateShape, 3, ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &hTensor);
    if (st) { g_ort->ReleaseStatus(st); g_ort->ReleaseValue(inputTensor); g_ort->ReleaseValue(srTensor); return 0.0f; }

    // Create c-state tensor [2, 1, 64].
    OrtValue* cTensor = nullptr;
    st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, g_cState, kStateSize * sizeof(float),
        kStateShape, 3, ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT, &cTensor);
    if (st) {
        g_ort->ReleaseStatus(st);
        g_ort->ReleaseValue(inputTensor); g_ort->ReleaseValue(srTensor); g_ort->ReleaseValue(hTensor);
        return 0.0f;
    }

    // Run inference with four inputs, get three outputs.
    const char*  inputNames[]  = {"input", "sr", "h", "c"};
    const char*  outputNames[] = {"output", "hn", "cn"};
    OrtValue*    inputs[]      = {inputTensor, srTensor, hTensor, cTensor};
    OrtValue*    outputs[]     = {nullptr, nullptr, nullptr};

    st = g_ort->Run(g_session, nullptr,
                    inputNames, inputs, 4,
                    outputNames, 3, outputs);
    g_ort->ReleaseValue(srTensor);
    g_ort->ReleaseValue(hTensor);
    g_ort->ReleaseValue(cTensor);
    g_ort->ReleaseValue(inputTensor);
    if (st) {
        const char* errMsg = g_ort->GetErrorMessage(st);
        LOGE("ONNX Run failed: %s", errMsg ? errMsg : "(no message)");
        g_ort->ReleaseStatus(st);
        for (int i = 0; i < 3; i++) if (outputs[i]) g_ort->ReleaseValue(outputs[i]);
        return 0.0f;
    }

    // Extract output float.
    float* outputData = nullptr;
    st = g_ort->GetTensorMutableData(outputs[0], (void**)&outputData);
    if (st) {
        const char* errMsg = g_ort->GetErrorMessage(st);
        LOGE("ONNX GetTensorMutableData failed: %s", errMsg ? errMsg : "(no message)");
        g_ort->ReleaseStatus(st);
        for (int i = 0; i < 3; i++) if (outputs[i]) g_ort->ReleaseValue(outputs[i]);
        return 0.0f;
    }
    float result = outputData[0];

    // Copy new hidden state back for next frame.
    float* hnData = nullptr;
    st = g_ort->GetTensorMutableData(outputs[1], (void**)&hnData);
    if (!st && hnData) {
        memcpy(g_hState, hnData, kStateSize * sizeof(float));
    } else if (st) {
        g_ort->ReleaseStatus(st);
    }

    // Copy new cell state back for next frame.
    float* cnData = nullptr;
    st = g_ort->GetTensorMutableData(outputs[2], (void**)&cnData);
    if (!st && cnData) {
        memcpy(g_cState, cnData, kStateSize * sizeof(float));
    } else if (st) {
        g_ort->ReleaseStatus(st);
    }

    for (int i = 0; i < 3; i++) if (outputs[i]) g_ort->ReleaseValue(outputs[i]);
    return result;
}

// ---------------------------------------------------------------------------
// JNI
// ---------------------------------------------------------------------------

extern "C" {

JNIEXPORT jboolean JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_audio_NativeVad_nativeInit(
    JNIEnv* env, jclass /*clazz*/, jobject assetManager)
{
    std::call_once(g_initFlag, [&]() {
        initOrt(env, assetManager);
    });
    if (!g_session) {
        LOGE("VAD init failed: %s", g_errorMsg.c_str());
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jfloat JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_audio_NativeVad_nativeIsSpeech(
    JNIEnv* env, jclass /*clazz*/, jshortArray jSamples)
{
    if (!g_session) return 0.0f;

    jsize len = env->GetArrayLength(jSamples);
    if (len < static_cast<jsize>(kInputSize)) return 0.0f;

    jshort* samples = env->GetShortArrayElements(jSamples, nullptr);
    if (!samples) return 0.0f;

    float result = sileroVadPredict(reinterpret_cast<const int16_t*>(samples), static_cast<int32_t>(len));

    env->ReleaseShortArrayElements(jSamples, samples, JNI_ABORT);
    return static_cast<jfloat>(result);
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_audio_NativeVad_nativeClose(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    cleanupOrt();
}

// Called from jni_bridge.cpp's nativeStartCaptureAndVad to ensure the
// Silero VAD ONNX session is initialized before the VAD processor starts.
// Uses std::call_once so repeated calls are safe.
bool vadEnsureInitialized(JNIEnv* env, jobject assetManager) {
    std::call_once(g_initFlag, [&]() {
        initOrt(env, assetManager);
    });
    return g_session != nullptr;
}

} // extern "C"
