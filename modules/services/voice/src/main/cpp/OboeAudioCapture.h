#ifndef POCKETCASTS_OBOE_AUDIO_CAPTURE_H
#define POCKETCASTS_OBOE_AUDIO_CAPTURE_H

#include <atomic>
#include <condition_variable>
#include <cstdint>
#include <mutex>
#include <oboe/Oboe.h>

constexpr int32_t kSampleRate = 16000;
constexpr int32_t kChannelCount = 1;          // MONO
constexpr int32_t kRingBufferCapacity = 32768; // ~2 seconds at 16kHz mono

/**
 * Lock-free single-producer single-consumer ring buffer for int16_t audio samples.
 *
 * The producer (Oboe onAudioReady callback) increments writeCursor.
 * The consumer (JNI poll thread) increments readCursor.
 * Both cursors are monotonic counters (never wrap modulo). The actual buffer
 * index is cursor % kRingBufferCapacity.
 *
 * Memory ordering:
 * - Producer: relaxed write cursor load, acquire read cursor load, release write cursor store
 * - Consumer: relaxed read cursor load, acquire write cursor load, release read cursor store
 */
class RingBuffer {
public:
    RingBuffer();
    ~RingBuffer();

    // Called from the audio thread (Oboe callback). Returns samples written.
    int32_t write(const int16_t* data, int32_t numSamples);

    // Called from the JNI polling thread. Returns samples read.
    int32_t read(int16_t* outData, int32_t maxSamples);

    // Returns number of samples available to read.
    int32_t available() const;

    void reset();

private:
    int16_t mBuffer[kRingBufferCapacity];
    std::atomic<int64_t> mWriteCursor{0};
    std::atomic<int64_t> mReadCursor{0};
};

/**
 * Wraps an Oboe input stream using callback mode.
 *
 * Callback mode avoids the AAudio releaseBuffer assertion that fires when
 * using blocking read/write with co-existing capture and playback streams
 * (Oboe issue #535, google-ai-edge/LiteRT-LM issues #684, #1033).
 */
class OboeAudioCapture : public oboe::AudioStreamDataCallback,
                          public oboe::AudioStreamErrorCallback {
public:
    OboeAudioCapture();
    ~OboeAudioCapture();

    // Open an input stream. Returns true on success.
    bool open();

    // Start the stream. Must call open() first.
    bool start();

    // Copy available samples from the ring buffer into outBuffer.
    // maxFrames is the desired number of audio frames to read.
    // Returns the number of frames actually written to outBuffer.
    int32_t readData(int16_t* outBuffer, int32_t maxFrames);

    // Block until data is available in the ring buffer or the stream stops.
    // Returns true if data is available to read, false on timeout or inactive.
    bool waitForData(int32_t timeoutMs);

    // Block until data is available, then read from the ring buffer.
    // This is the sole consumer path for the VAD processing thread.
    // Returns number of samples read, 0 on timeout, -1 if stream is not active.
    int32_t readRingBuffer(int16_t* outData, int32_t maxSamples, int32_t timeoutMs);

    // Stop the stream.
    void stop();

    // Close the stream and release resources.
    void close();

    // Returns true if the stream is active.
    bool isActive() const;

    // Accessors for actual stream parameters.
    int32_t getSampleRate() const { return mSampleRate; }
    int32_t getChannelCount() const { return mChannelCount; }

    // --- oboe::AudioStreamDataCallback ---
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) override;

    // --- oboe::AudioStreamErrorCallback ---
    void onErrorAfterClose(
        oboe::AudioStream* audioStream,
        oboe::Result error) override;

private:
    oboe::AudioStream* mStream = nullptr;
    RingBuffer mRingBuffer;
    int32_t mSampleRate = kSampleRate;
    int32_t mChannelCount = kChannelCount;
    std::atomic<bool> mActive{false};
    std::atomic<bool> mClosed{false};
    std::mutex mStreamMutex;
    std::mutex mWaitMutex;
    std::condition_variable mDataCv;

    oboe::AudioStreamBuilder buildStreamBuilder();
};

#endif // POCKETCASTS_OBOE_AUDIO_CAPTURE_H
