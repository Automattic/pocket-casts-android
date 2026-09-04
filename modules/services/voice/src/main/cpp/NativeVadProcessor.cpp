#include "NativeVadProcessor.h"
#include "OboeAudioCapture.h"

#include <android/log.h>
#include <chrono>
#include <cmath>
#include <cstring>

#define LOG_TAG "VoicePipeline"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "[VoicePipeline] " __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "[VoicePipeline] " __VA_ARGS__)

// Reusable Silero VAD inference, implemented in VadJni.cpp
extern float sileroVadPredict(const int16_t* samples, int32_t count);
extern void sileroVadResetState();

using Clock = std::chrono::steady_clock;

// ---------------------------------------------------------------------------
// NativeVadProcessor
// ---------------------------------------------------------------------------

NativeVadProcessor::NativeVadProcessor(OboeAudioCapture* capture)
    : mCapture(capture)
    , mContextBuffer(kMaxContextFrames * kVadFrameSize, 0)
{
}

NativeVadProcessor::~NativeVadProcessor() {
    stop();
}

bool NativeVadProcessor::start() {
    if (mActive.load(std::memory_order_acquire)) {
        return true; // already running
    }

    // Reset Silero VAD LSTM state so a new session starts fresh
    // rather than carrying over hidden/cell state from the prior run.
    sileroVadResetState();

    mActive.store(true, std::memory_order_release);
    mThread = std::make_unique<std::thread>(&NativeVadProcessor::runLoop, this);
    LOGI("VAD processor thread started");
    return true;
}

void NativeVadProcessor::stop() {
    mActive.store(false, std::memory_order_release);

    // Wake up any blocked callers
    {
        std::lock_guard<std::mutex> lock(mEventMutex);
        mPendingEvent = -1;
        mEventReady = true;
        mEventCv.notify_all();
    }

    if (mThread && mThread->joinable()) {
        mThread->join();
    }
    mThread.reset();

    // Reset state
    mContextHead = 0;
    mContextCount = 0;
    mSpeechBuffer.clear();
    mSpeechFrames = 0;
    mSpeechOnsetSample = 0;
    mSpeechActive = false;
    mConsecutiveSilentFrames = 0;
    mDrainRemaining = 0;
    mCooldownUntilUs = 0;

    {
        std::lock_guard<std::mutex> lock(mSpeechMutex);
        mSnapshotBuffer.clear();
        mSnapshotSpeechOnsetSample = 0;
    }

    LOGI("VAD processor thread stopped");
}

int NativeVadProcessor::waitForEvent(int32_t timeoutMs) {
    std::unique_lock<std::mutex> lock(mEventMutex);
    if (!mEventReady) {
        mEventCv.wait_for(lock, std::chrono::milliseconds(timeoutMs));
    }
    if (!mEventReady) {
        return 0; // timeout
    }
    int event = mPendingEvent;
    mPendingEvent = 0;
    mEventReady = false;
    return event;
}

int32_t NativeVadProcessor::getSpeechPcmSize() {
    std::lock_guard<std::mutex> lock(mSpeechMutex);
    return static_cast<int32_t>(mSnapshotBuffer.size());
}

int32_t NativeVadProcessor::getSpeechPcm(int16_t* outBuffer, int32_t maxSamples) {
    std::lock_guard<std::mutex> lock(mSpeechMutex);
    if (mSnapshotBuffer.empty()) {
        return 0;
    }
    int32_t count = (maxSamples < static_cast<int32_t>(mSnapshotBuffer.size()))
        ? maxSamples
        : static_cast<int32_t>(mSnapshotBuffer.size());
    std::memcpy(outBuffer, mSnapshotBuffer.data(), static_cast<size_t>(count) * sizeof(int16_t));
    return count;
}

int32_t NativeVadProcessor::getSpeechOnsetSample() {
    std::lock_guard<std::mutex> lock(mSpeechMutex);
    return mSnapshotSpeechOnsetSample;
}

// ---------------------------------------------------------------------------
// energyGate — fast RMS check to skip Silero VAD for silent frames
// ---------------------------------------------------------------------------
bool NativeVadProcessor::energyGate(const int16_t* samples, int32_t count) {
    double sum = 0.0;
    for (int32_t i = 0; i < count; ++i) {
        double v = static_cast<double>(samples[i]);
        sum += v * v;
    }
    double rms = std::sqrt(sum / static_cast<double>(count));
    return rms >= kRmsThreshold;
}

// ---------------------------------------------------------------------------
// Helper: flush context buffer into speech buffer
// ---------------------------------------------------------------------------
static void flushContextIntoSpeech(
    std::vector<int16_t>& speechBuffer,
    int32_t& speechFrames,
    const std::vector<int16_t>& contextBuffer,
    int32_t contextHead,
    int32_t contextCount,
    int32_t maxContextFrames,
    int32_t vadFrameSize)
{
    if (contextCount <= 0) return;

    // Context frames are stored circularly. The oldest frame is at
    // (contextHead - contextCount) wrapped around.
    int32_t startIdx = (contextHead - contextCount + maxContextFrames) % maxContextFrames;

    for (int32_t i = 0; i < contextCount; ++i) {
        int32_t idx = (startIdx + i) % maxContextFrames;
        int32_t offset = idx * vadFrameSize;
        speechBuffer.insert(speechBuffer.end(),
            contextBuffer.begin() + offset,
            contextBuffer.begin() + offset + vadFrameSize);
        speechFrames++;
    }
}

// ---------------------------------------------------------------------------
// Helper: add a frame to the circular context buffer
// ---------------------------------------------------------------------------
static void addToContext(
    std::vector<int16_t>& contextBuffer,
    int32_t& contextHead,
    int32_t& contextCount,
    const int16_t* samples,
    int32_t maxContextFrames,
    int32_t vadFrameSize)
{
    int32_t offset = contextHead * vadFrameSize;
    std::memcpy(&contextBuffer[static_cast<size_t>(offset)], samples,
        static_cast<size_t>(vadFrameSize) * sizeof(int16_t));
    contextHead = (contextHead + 1) % maxContextFrames;
    if (contextCount < maxContextFrames) {
        contextCount++;
    }
}

// ---------------------------------------------------------------------------
// runLoop — main VAD processing loop
// ---------------------------------------------------------------------------
void NativeVadProcessor::runLoop() {
    LOGI("VAD runLoop started");

    int16_t chunk[kVadFrameSize];

    while (mActive.load(std::memory_order_acquire)) {
        // 1. Block on ring buffer for one full VAD frame.
        // readRingBuffer waits for >=1024 samples to accumulate.
        int32_t read = mCapture->readRingBuffer(chunk, kVadFrameSize, 500);
        if (read < 0) {
            break; // stream inactive
        }
        if (read < kVadFrameSize) {
            continue; // timeout — retry
        }

        // 2. Cooldown gate — discard frames while cooldown is active.
        int64_t nowUs = std::chrono::duration_cast<std::chrono::microseconds>(
            Clock::now().time_since_epoch()).count();
        if (nowUs < mCooldownUntilUs) {
            // Still in cooldown: add to context buffer (so we have pre-speech
            // audio when cooldown expires) but don't process.
            addToContext(mContextBuffer, mContextHead, mContextCount,
                chunk, kMaxContextFrames, kVadFrameSize);
            continue;
        }

        // 3. Max-duration check.
        if (mSpeechActive && mSpeechFrames >= kMaxSpeechFrames) {
            LOGI("VAD: max duration exceeded (%d frames), finalizing",
                mSpeechFrames);

            {
                std::lock_guard<std::mutex> lock(mSpeechMutex);
                mSnapshotBuffer = std::move(mSpeechBuffer);
                mSnapshotSpeechOnsetSample = mSpeechOnsetSample;
                mSpeechBuffer.clear();
            }

            mSpeechFrames = 0;
            mSpeechOnsetSample = 0;
            mSpeechActive = false;
            mConsecutiveSilentFrames = 0;
            mDrainRemaining = 0;
            mContextCount = 0;
            mContextHead = 0;
            mCooldownUntilUs = nowUs + kCooldownMs * 1000;

            std::lock_guard<std::mutex> lock(mEventMutex);
            mPendingEvent = 2; // speech ended
            mEventReady = true;
            mEventCv.notify_one();

            continue;
        }

        // 4. Energy gate — skip Silero VAD for silent frames.
        bool hasEnergy = energyGate(chunk, kVadFrameSize);
        bool isSpeech = false;
        if (hasEnergy) {
            float prob = sileroVadPredict(chunk, kVadFrameSize);
            isSpeech = (prob >= kSpeechThreshold);
        }

        // 5. Speech detected.
        if (isSpeech) {
            mConsecutiveSilentFrames = 0;
            mDrainRemaining = 0;

            if (!mSpeechActive) {
                mSpeechOnsetSample = mContextCount * kVadFrameSize;

                // Flush pre-speech context into speech buffer.
                flushContextIntoSpeech(mSpeechBuffer, mSpeechFrames,
                    mContextBuffer, mContextHead, mContextCount,
                    kMaxContextFrames, kVadFrameSize);
                mContextCount = 0;
                mContextHead = 0;

                mSpeechActive = true;

                // Signal speech started.
                std::lock_guard<std::mutex> lock(mEventMutex);
                mPendingEvent = 1; // speech started
                mEventReady = true;
                mEventCv.notify_one();

                LOGI("VAD: speech started (pre-speech context=%d samples)",
                    mSpeechOnsetSample);
            }

            // Accumulate current frame.
            mSpeechBuffer.insert(mSpeechBuffer.end(), chunk, chunk + kVadFrameSize);
            mSpeechFrames++;
            continue;
        }

        // 6. Silence handling.
        if (!mSpeechActive) {
            // No speech yet: buffer silent audio as potential pre-speech context.
            addToContext(mContextBuffer, mContextHead, mContextCount,
                chunk, kMaxContextFrames, kVadFrameSize);
        } else {
            mConsecutiveSilentFrames++;

            if (mConsecutiveSilentFrames < kSilenceTimeoutFrames) {
                // Micro-pause bridging: accumulate silent frames as part of speech.
                mSpeechBuffer.insert(mSpeechBuffer.end(), chunk, chunk + kVadFrameSize);
                mSpeechFrames++;
            } else {
                // Silence timeout reached — enter drain phase.
                if (mDrainRemaining <= 0) {
                    // Compute how many more frames to drain to reach the target
                    // utterance length (~3.5s) for reliable Whisper language detection.
                    int32_t totalSoFar = mSpeechFrames;
                    int32_t needed = kTargetTotalFrames - totalSoFar;
                    if (needed < kMinPostSpeechFrames) {
                        needed = kMinPostSpeechFrames;
                    }
                    if (needed > 40) {
                        needed = 40;
                    }
                    mDrainRemaining = needed;
                }

                mDrainRemaining--;
                mSpeechBuffer.insert(mSpeechBuffer.end(), chunk, chunk + kVadFrameSize);
                mSpeechFrames++;

                if (mDrainRemaining <= 0) {
                    // Drain complete — finalize the utterance.
                    LOGI("VAD: speech ended (%d frames total)", mSpeechFrames);

                    {
                        std::lock_guard<std::mutex> lock(mSpeechMutex);
                        mSnapshotBuffer = std::move(mSpeechBuffer);
                        mSnapshotSpeechOnsetSample = mSpeechOnsetSample;
                        mSpeechBuffer.clear();
                    }

                    mSpeechFrames = 0;
                    mSpeechOnsetSample = 0;
                    mSpeechActive = false;
                    mConsecutiveSilentFrames = 0;
                    mDrainRemaining = 0;
                    mContextCount = 0;
                    mContextHead = 0;
                    mCooldownUntilUs = nowUs + kCooldownMs * 1000;

                    std::lock_guard<std::mutex> lock(mEventMutex);
                    mPendingEvent = 2; // speech ended
                    mEventReady = true;
                    mEventCv.notify_one();
                }
            }
        }
    }

    LOGI("VAD runLoop exited");
}
