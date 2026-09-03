// WhisperJni.cpp
#include "WhisperJni.h"
#include "jni_bridge_common.h"
#include "whisper.h"
#include <android/log.h>
#include <chrono>
#include <cstdio>
#include <cstdlib>
#include <mutex>
#include <sstream>
#include <thread>
#include <vector>

#define LOG_TAG "WhisperJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void whisperGgmlLog(enum ggml_log_level level, const char * text, void * /*user_data*/) {
    int prio = ANDROID_LOG_WARN;
    if (level == GGML_LOG_LEVEL_ERROR) prio = ANDROID_LOG_ERROR;
    __android_log_write(prio, "whisper.cpp", text);
}

// Vulkan backend performance tuning for Mali-G715 (Tensor G3 / Pixel 8).
// Whisper currently runs on CPU (no Vulkan support in whisper.cpp), but the
// ggml Vulkan backend is still initialized at library load time through the
// shared ggml build with llama.cpp. These env vars keep it configured
// consistently between the two JNI modules.
static void initVulkanEnv() {
    static bool done = false;
    if (done) return;
    setenv("GGML_VK_ALLOW_GRAPHICS_QUEUE", "1", 1);
    setenv("GGML_VK_FORCE_MAX_ALLOCATION_SIZE", "536870912", 1);
    setenv("GGML_VK_SUBALLOCATION_BLOCK_SIZE", "67108864", 1);
    done = true;
}

static std::mutex g_mutex;
static whisper_context* g_ctx = nullptr;
static std::string g_model_path;

static bool ensureModel(const std::string& path) {
    if (g_ctx && g_model_path == path) return true;
    if (g_ctx) { whisper_free(g_ctx); g_ctx = nullptr; }
    auto t0 = std::chrono::steady_clock::now();
    auto params = whisper_context_default_params();
    g_ctx = whisper_init_from_file_with_params(path.c_str(), params);
    auto t1 = std::chrono::steady_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(t1 - t0).count();
    LOGI("model loaded in %lldms, use_gpu=%d", (long long)ms, params.use_gpu);
    g_model_path = path;
    return g_ctx != nullptr;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_asr_WhisperNative_init(
    JNIEnv* env, jclass, jstring j_model_path
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    ggml_log_set(whisperGgmlLog, nullptr);
    whisper_log_set(whisperGgmlLog, nullptr);
    initVulkanEnv();

    std::string modelPath = jstringToString(env, j_model_path);
    return ensureModel(modelPath) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_asr_WhisperNative_transcribe(
    JNIEnv* env, jclass, jstring j_model_path, jshortArray j_pcm_data, jint j_sample_rate
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    ggml_log_set(whisperGgmlLog, nullptr);
    whisper_log_set(whisperGgmlLog, nullptr);
    initVulkanEnv();

    std::string modelPath = jstringToString(env, j_model_path);
    if (!ensureModel(modelPath)) return stringToJstring(env, "");

    jsize len = env->GetArrayLength(j_pcm_data);
    jshort* elements = env->GetShortArrayElements(j_pcm_data, nullptr);

    std::vector<float> pcmF32(len);
    for (jsize i = 0; i < len; i++) pcmF32[i] = elements[i] / 32768.0f;
    env->ReleaseShortArrayElements(j_pcm_data, elements, JNI_ABORT);

    // Use hardware_concurrency() capped to [2, 8] — encoder matmul saturates
    // at ~6-8 threads on big.LITTLE Arm cores; fewer threads waste throughput.
    int nThreads = static_cast<int>(std::thread::hardware_concurrency());
    if (nThreads < 2) nThreads = 2;
    if (nThreads > 8) nThreads = 8;

    auto wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress = false;
    wparams.print_timestamps = false;
    wparams.print_special = false;
    // Detect the source language inside whisper_full so translation reuses the
    // encoder pass. A fixed language would decode multilingual audio as that
    // language instead of translating it correctly.
    wparams.language = "auto";
    wparams.translate = true;
    wparams.suppress_nst = true;
    wparams.n_threads = nThreads;
    // DIAGNOSTIC: single_segment/no_timestamps removed to isolate the decode runaway.
    // Scale the audio context to the utterance instead of always encoding a
    // full 30s window. Encoder cost scales with audio_ctx, so a ~1s command no
    // longer pays for 30s of padding. mel hop is 160 samples and the encoder
    // conv halves the frame count, so audio_ctx ~= n_samples/320; add margin
    // and clamp to the model's full 1500.
    int audioCtx = static_cast<int>(pcmF32.size() / 320) + 16;
    if (audioCtx < 32) audioCtx = 32;
    if (audioCtx > 1500) audioCtx = 1500;
    wparams.audio_ctx = audioCtx;
    wparams.no_context = true;
    // DIAGNOSTIC: disable temperature fallback so the decode is a single greedy
    // pass (no 6-step retry ladder). Measures the whisper-small CPU floor.
    wparams.temperature_inc = 0.0f;
    wparams.progress_callback = nullptr;

    whisper_reset_timings(g_ctx);
    auto t0 = std::chrono::steady_clock::now();
    int decodeResult = whisper_full(g_ctx, wparams, pcmF32.data(), (int)pcmF32.size());
    auto t1 = std::chrono::steady_clock::now();
    auto inferMs = std::chrono::duration_cast<std::chrono::milliseconds>(t1 - t0).count();
    LOGI("whisper: %lldms, %d samples (%dms audio)",
         (long long)inferMs, (int)pcmF32.size(), (int)(pcmF32.size() * 1000 / 16000));

    if (decodeResult != 0)
        return stringToJstring(env, "");

    std::string text;
    std::ostringstream tokensJson;
    tokensJson << "[";
    bool firstTok = true;
    int n = whisper_full_n_segments(g_ctx);
    const int eot = whisper_token_eot(g_ctx);
    for (int i = 0; i < n; i++) {
        const char* seg = whisper_full_get_segment_text(g_ctx, i);
        if (seg) {
            if (!text.empty()) text += " ";
            text += seg;
        }
        const int nTok = whisper_full_n_tokens(g_ctx, i);
        for (int t = 0; t < nTok; t++) {
            whisper_token_data data = whisper_full_get_token_data(g_ctx, i, t);
            if (data.id >= eot) continue;
            const char* tok = whisper_full_get_token_text(g_ctx, i, t);
            if (!tok || tok[0] == '\0' || tok[0] == '[') continue;
            if (!firstTok) tokensJson << ",";
            firstTok = false;
            tokensJson << "{\"text\":\"";
            for (const char* p = tok; *p; ++p) {
                unsigned char c = static_cast<unsigned char>(*p);
                if (c == '"' || c == '\\') tokensJson << '\\' << *p;
                else if (c < 0x20) {
                    char buf[8];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", c);
                    tokensJson << buf;
                } else {
                    tokensJson << *p;
                }
            }
            tokensJson << "\",\"startMs\":" << (data.t0 * 10)
                       << ",\"endMs\":" << (data.t1 * 10) << "}";
        }
    }
    tokensJson << "]";

    std::ostringstream payload;
    payload << "{\"text\":\"";
    for (char ch : text) {
        unsigned char c = static_cast<unsigned char>(ch);
        if (c == '"' || c == '\\') payload << '\\' << ch;
        else if (c < 0x20) {
            char buf[8];
            std::snprintf(buf, sizeof(buf), "\\u%04x", c);
            payload << buf;
        } else {
            payload << ch;
        }
    }
    payload << "\",\"tokens\":" << tokensJson.str() << "}";

    LOGI("whisper result (%d segments): \"%s\"", n, text.c_str());
    return stringToJstring(env, payload.str());
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_asr_WhisperNative_setPipelineCachePath(
    JNIEnv* env, jclass, jstring j_path
) {
    std::string path = jstringToString(env, j_path);
    setenv("GGML_VULKAN_PIPELINE_CACHE_PATH", path.c_str(), 1);
    // Verify the directory is writable
    FILE* testf = fopen(path.c_str(), "wb");
    if (testf) {
        fwrite("test", 1, 4, testf);
        fclose(testf);
        LOGI("pipeline cache path writable: %s", path.c_str());
    } else {
        LOGE("pipeline cache path NOT writable: %s (errno=%d)", path.c_str(), errno);
    }
}

JNIEXPORT void JNICALL
Java_au_com_shiftyjelly_pocketcasts_voicecontrol_asr_WhisperNative_freeModel(
    JNIEnv*, jclass
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_ctx) {
        whisper_free(g_ctx);
        g_ctx = nullptr;
        g_model_path.clear();
        LOGI("whisper model freed");
    }
}

} // extern "C"
