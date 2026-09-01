#include "LfmRuntime.h"
#include "../jni_bridge_common.h"

#include <android/log.h>
#include <jni.h>

#include <mutex>
#include <vector>

#define LOG_TAG "LlmJni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

thread_local std::string g_lastError;

void setLastError(const std::exception& error) {
    g_lastError = error.what();
    LOGE("%s", g_lastError.c_str());
}

std::vector<int> jintArrayToVector(JNIEnv* env, jintArray array) {
    const jsize length = env->GetArrayLength(array);
    std::vector<int> values(static_cast<std::size_t>(length));
    env->GetIntArrayRegion(array, 0, length, values.data());
    return values;
}

std::vector<float> jfloatArrayToVector(JNIEnv* env, jfloatArray array) {
    const jsize length = env->GetArrayLength(array);
    std::vector<float> values(static_cast<std::size_t>(length));
    env->GetFloatArrayRegion(array, 0, length, values.data());
    return values;
}

jintArray vectorToJintArray(JNIEnv* env, const std::vector<int>& values) {
    jintArray array = env->NewIntArray(static_cast<jsize>(values.size()));
    if (array == nullptr) {
        return nullptr;
    }
    env->SetIntArrayRegion(array, 0, static_cast<jsize>(values.size()), values.data());
    return array;
}

}  // namespace

extern "C" {

JNIEXPORT jstring JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_lastError(
    JNIEnv* env,
    jclass) {
    return stringToJstring(env, g_lastError);
}

JNIEXPORT jboolean JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeLoadClassifier(
    JNIEnv* env,
    jclass,
    jstring classifierPath,
    jstring labelMapPath,
    jint expectedHiddenSize) {
    try {
        g_lastError.clear();
        const std::string classifier = jstringToString(env, classifierPath);
        const std::string labelMap = jstringToString(env, labelMapPath);
        return LfmRuntimeHolder::instance().loadClassifierOnly(
            classifier,
            labelMap,
            static_cast<int>(expectedHiddenSize))
            ? JNI_TRUE
            : JNI_FALSE;
    } catch (const std::exception& error) {
        setLastError(error);
        return JNI_FALSE;
    }
}

JNIEXPORT jstring JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeClassifyEmbedding(
    JNIEnv* env,
    jclass,
    jfloatArray embedding) {
    try {
        g_lastError.clear();
        const auto values = jfloatArrayToVector(env, embedding);
        return stringToJstring(env, LfmRuntimeHolder::instance().classifyEmbedding(values));
    } catch (const std::exception& error) {
        setLastError(error);
        return nullptr;
    }
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeReleaseClassifier(
    JNIEnv*,
    jclass) {
    LfmRuntimeHolder::instance().releaseClassifierOnly();
    g_lastError.clear();
}

JNIEXPORT jboolean JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeLoad(
    JNIEnv* env,
    jclass,
    jstring modelPath,
    jstring classifierPath,
    jstring labelMapPath,
    jint nCtx) {
    try {
        g_lastError.clear();
        return LfmRuntimeHolder::instance().load(
            jstringToString(env, modelPath),
            jstringToString(env, classifierPath),
            jstringToString(env, labelMapPath),
            static_cast<int>(nCtx))
            ? JNI_TRUE
            : JNI_FALSE;
    } catch (const std::exception& error) {
        setLastError(error);
        return JNI_FALSE;
    }
}

JNIEXPORT jstring JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeClassify(
    JNIEnv* env,
    jclass,
    jintArray promptTokenIds,
    jint poolStart,
    jint poolEnd) {
    try {
        g_lastError.clear();
        const auto tokens = jintArrayToVector(env, promptTokenIds);
        return stringToJstring(
            env,
            LfmRuntimeHolder::instance().classify(tokens, static_cast<int>(poolStart), static_cast<int>(poolEnd)));
    } catch (const std::exception& error) {
        setLastError(error);
        return nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeGenerate(
    JNIEnv* env,
    jclass,
    jstring prefill,
    jint nPredict) {
    try {
        g_lastError.clear();
        return stringToJstring(
            env,
            LfmRuntimeHolder::instance().generate(jstringToString(env, prefill), static_cast<int>(nPredict)));
    } catch (const std::exception& error) {
        setLastError(error);
        return nullptr;
    }
}

JNIEXPORT jintArray JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeTokenize(
    JNIEnv* env,
    jclass,
    jstring text,
    jboolean addBos) {
    try {
        g_lastError.clear();
        return vectorToJintArray(
            env,
            LfmRuntimeHolder::instance().tokenize(jstringToString(env, text), addBos == JNI_TRUE));
    } catch (const std::exception& error) {
        setLastError(error);
        return nullptr;
    }
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeReset(
    JNIEnv*,
    jclass) {
    LfmRuntimeHolder::instance().reset();
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_intent_lfm_LfmNative_nativeRelease(
    JNIEnv*,
    jclass) {
    LfmRuntimeHolder::instance().release();
    g_lastError.clear();
}

}  // extern "C"
