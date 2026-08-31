#ifndef POCKETCASTS_NATIVE_VAD_PROCESSOR_H
#define POCKETCASTS_NATIVE_VAD_PROCESSOR_H

#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <memory>
#include <mutex>
#include <thread>
#include <vector>

class OboeAudioCapture;

/**
 * C++ VAD state machine that consumes audio from the Oboe ring buffer,
 * runs an energy gate and Silero VAD ONNX inference, and signals
 * speech-start / speech-end events to Kotlin via JNI.
 *
 * The processing thread is the sole consumer of the ring buffer.
 * Kotlin blocks on waitForEvent() rather than polling per-frame.
 */
class NativeVadProcessor {
public:
    explicit NativeVadProcessor(OboeAudioCapture* capture);
    ~NativeVadProcessor();

    bool start();   // launch processing thread
    void stop();    // join thread, reset state

    // Block until event. Returns: 1=speech started, 2=speech ended, 0=timeout, -1=stopped
    int waitForEvent(int32_t timeoutMs);

    int32_t getSpeechPcmSize();
    int32_t getSpeechPcm(int16_t* outBuffer, int32_t maxSamples);
    int32_t getSpeechOnsetSample();

private:
    void runLoop();
    static bool energyGate(const int16_t* samples, int32_t count);

    // Parameters (matching spec thresholds)
    static constexpr int32_t kVadFrameSize = 1024;        // 64ms @ 16kHz
    static constexpr int32_t kSilenceTimeoutFrames = 7;    // ~448ms
    static constexpr int32_t kMinPostSpeechFrames = 10;    // ~640ms drain floor
    static constexpr int32_t kTargetTotalFrames = 55;      // ~3.5s target
    static constexpr int32_t kMaxContextFrames = 20;       // ~1.28s pre-speech
    static constexpr int32_t kMaxSpeechFrames = 78;         // ~5s max
    static constexpr int32_t kCooldownMs = 1500;
    static constexpr double kRmsThreshold = 200.0;
    static constexpr float kSpeechThreshold = 0.2f;

    OboeAudioCapture* mCapture;

    std::unique_ptr<std::thread> mThread;
    std::atomic<bool> mActive{false};

    // Circular pre-speech context buffer: stores up to kMaxContextFrames of
    // silent audio frames before speech onset.
    std::vector<int16_t> mContextBuffer;
    int32_t mContextHead = 0;
    int32_t mContextCount = 0;

    // Assembled utterance PCM (written by VAD thread).
    std::vector<int16_t> mSpeechBuffer;
    int32_t mSpeechFrames = 0;
    int32_t mSpeechOnsetSample = 0;
    bool mSpeechActive = false;

    // Snapshot of mSpeechBuffer for safe cross-thread access from Kotlin.
    std::vector<int16_t> mSnapshotBuffer;
    int32_t mSnapshotSpeechOnsetSample = 0;
    std::mutex mSpeechMutex;

    int32_t mConsecutiveSilentFrames = 0;
    int32_t mDrainRemaining = 0;
    int64_t mCooldownUntilUs = 0;

    std::mutex mEventMutex;
    std::condition_variable mEventCv;
    int mPendingEvent = 0;
    bool mEventReady = false;
};

#endif // POCKETCASTS_NATIVE_VAD_PROCESSOR_H
