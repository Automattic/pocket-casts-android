// EmbeddingJni.cpp — multilingual-e5-small inference via Moonshine's ORT.
//
// Uses ORT C API via dlsym from onnxruntime-android (loaded via System.loadLibrary).
// CreateSessionFromArray from bytes, no session options.
// No session options, no shape detection — minimal surface area.
//
// Model:  multilingual-e5-small ONNX (118M params, 384-dim).
// Input:  input_ids [1, seq_len] + attention_mask [1, seq_len]
// Output: last_hidden_state [1, seq_len, 384] → CLS token → L2 normalize

#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <cstdint>
#include <cstring>
#include <cmath>
#include <mutex>
#include <vector>

#include "onnxruntime_c_api.h"

#define LOG_TAG "EmbeddingJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef const OrtApiBase* (*FnOrtGetApiBase)(void);

static const OrtApi*   g_ort = nullptr;
static OrtEnv*         g_env = nullptr;
static OrtSession*     g_session = nullptr;
static OrtMemoryInfo*  g_memInfo = nullptr;
static std::once_flag  g_initFlag;
static std::string     g_errorMsg;

static void cleanupOrt() {
    if (g_session)  { g_ort->ReleaseSession(g_session); g_session = nullptr; }
    if (g_memInfo)  { g_ort->ReleaseMemoryInfo(g_memInfo); g_memInfo = nullptr; }
    if (g_env)      { g_ort->ReleaseEnv(g_env); g_env = nullptr; }
    g_ort = nullptr;
}

static bool initOrt(JNIEnv* env, jbyteArray jModelData) {
    if (g_session) return true;

    // Resolve ORT from onnxruntime-android (loaded by System.loadLibrary).
    void* ortLib = dlopen("libonnxruntime.so", RTLD_NOLOAD);
    if (!ortLib) { g_errorMsg = "libonnxruntime.so not loaded"; return false; }

    auto fnGetApiBase = (FnOrtGetApiBase)dlsym(ortLib, "OrtGetApiBase");
    if (!fnGetApiBase) { g_errorMsg = "OrtGetApiBase not found"; dlclose(ortLib); return false; }

    g_ort = fnGetApiBase()->GetApi(ORT_API_VERSION);
    if (!g_ort) { g_errorMsg = "GetApi failed"; dlclose(ortLib); return false; }

    // Create env (exactly like VadJni).
    OrtStatus* st = g_ort->CreateEnv(ORT_LOGGING_LEVEL_WARNING, "EmbeddingJni", &g_env);
    if (st) { g_errorMsg = g_ort->GetErrorMessage(st); g_ort->ReleaseStatus(st); cleanupOrt(); return false; }

    // Create session from byte array (exactly like VadJni's CreateSessionFromArray).
    jsize modelSize = env->GetArrayLength(jModelData);
    jbyte* modelBytes = env->GetByteArrayElements(jModelData, nullptr);
    if (!modelBytes) { g_errorMsg = "Failed to get model bytes"; cleanupOrt(); return false; }

    st = g_ort->CreateSessionFromArray(g_env, modelBytes, (size_t)modelSize, nullptr, &g_session);
    env->ReleaseByteArrayElements(jModelData, modelBytes, JNI_ABORT);

    if (st) {
        const char* errMsg = g_ort->GetErrorMessage(st);
        g_errorMsg = std::string("CreateSession failed: ") + (errMsg ? errMsg : "unknown");
        g_ort->ReleaseStatus(st);
        cleanupOrt();
        return false;
    }

    // Create memory info (exactly like VadJni).
    st = g_ort->CreateCpuMemoryInfo(OrtArenaAllocator, OrtMemTypeDefault, &g_memInfo);
    if (st) { g_errorMsg = g_ort->GetErrorMessage(st); g_ort->ReleaseStatus(st); cleanupOrt(); return false; }

    LOGI("Embedding session created");
    return true;
}

// ---------------------------------------------------------------------------
// JNI
// ---------------------------------------------------------------------------

extern "C" {

JNIEXPORT jboolean JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_embedding_EmbeddingJni_nativeInit(
    JNIEnv* env, jclass /*clazz*/, jbyteArray jModelData)
{
    std::call_once(g_initFlag, [&]() {
        initOrt(env, jModelData);
    });
    if (!g_session) {
        LOGE("Embedding init failed: %s", g_errorMsg.c_str());
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_embedding_EmbeddingJni_nativeError(
    JNIEnv* env, jclass /*clazz*/)
{
    return env->NewStringUTF(g_errorMsg.c_str());
}

JNIEXPORT jint JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_embedding_EmbeddingJni_nativeDim(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    return 384; // multilingual-e5-small
}

JNIEXPORT jfloatArray JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_embedding_EmbeddingJni_nativeEmbed(
    JNIEnv* env, jclass /*clazz*/, jintArray jTokenIds)
{
    if (!g_session) return nullptr;

    jsize seqLen = env->GetArrayLength(jTokenIds);
    if (seqLen <= 0) return nullptr;

    jint* tokenIds = env->GetIntArrayElements(jTokenIds, nullptr);
    if (!tokenIds) return nullptr;

    std::vector<int64_t> inputIds(seqLen);
    std::vector<int64_t> attnMask(seqLen);
    for (jsize i = 0; i < seqLen; i++) {
        inputIds[i] = (int64_t)tokenIds[i];
        attnMask[i] = 1;
    }
    env->ReleaseIntArrayElements(jTokenIds, tokenIds, JNI_ABORT);

    int64_t shape[] = {1, seqLen};

    // input_ids
    OrtValue* inputTensor = nullptr;
    OrtStatus* st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, inputIds.data(), inputIds.size() * sizeof(int64_t),
        shape, 2, ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64, &inputTensor);
    if (st) { g_ort->ReleaseStatus(st); return nullptr; }

    // attention_mask
    OrtValue* maskTensor = nullptr;
    st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, attnMask.data(), attnMask.size() * sizeof(int64_t),
        shape, 2, ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64, &maskTensor);
    if (st) { g_ort->ReleaseStatus(st); g_ort->ReleaseValue(inputTensor); return nullptr; }

    // token_type_ids (all zeros, same shape)
    std::vector<int64_t> typeIds(seqLen, 0);
    OrtValue* typeTensor = nullptr;
    st = g_ort->CreateTensorWithDataAsOrtValue(
        g_memInfo, typeIds.data(), typeIds.size() * sizeof(int64_t),
        shape, 2, ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64, &typeTensor);
    if (st) {
        g_ort->ReleaseStatus(st);
        g_ort->ReleaseValue(inputTensor);
        g_ort->ReleaseValue(maskTensor);
        return nullptr;
    }

    // Run
    const char* inputNames[]  = {"input_ids", "attention_mask", "token_type_ids"};
    OrtValue*   inputs[]      = {inputTensor, maskTensor, typeTensor};
    const char* outputNames[] = {"last_hidden_state"};
    OrtValue*   output        = nullptr;

    st = g_ort->Run(g_session, nullptr, inputNames, inputs, 3, outputNames, 1, &output);
    g_ort->ReleaseValue(inputTensor);
    g_ort->ReleaseValue(maskTensor);
    g_ort->ReleaseValue(typeTensor);

    if (st) {
        LOGE("Run failed: %s", g_ort->GetErrorMessage(st));
        g_ort->ReleaseStatus(st);
        return nullptr;
    }

    // Extract CLS token (first 384 floats) and L2-normalize
    float* outputData = nullptr;
    st = g_ort->GetTensorMutableData(output, (void**)&outputData);
    if (st) { g_ort->ReleaseStatus(st); g_ort->ReleaseValue(output); return nullptr; }

    constexpr int kDim = 384;
    float embedding[kDim];
    memcpy(embedding, outputData, kDim * sizeof(float));
    g_ort->ReleaseValue(output);

    // L2 normalize
    float norm = 0.0f;
    for (int i = 0; i < kDim; i++) norm += embedding[i] * embedding[i];
    float scale = (norm > 0.0f) ? 1.0f / sqrtf(norm) : 1.0f;
    for (int i = 0; i < kDim; i++) embedding[i] *= scale;

    jfloatArray result = env->NewFloatArray(kDim);
    env->SetFloatArrayRegion(result, 0, kDim, embedding);
    return result;
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_embedding_EmbeddingJni_nativeClose(
    JNIEnv* /*env*/, jclass /*clazz*/)
{
    cleanupOrt();
}

} // extern "C"
