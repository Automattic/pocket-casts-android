#include "OboeAudioCapture.h"
#include <android/log.h>

#define LOG_TAG "OboeCapture"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ---------------------------------------------------------------------------
// RingBuffer
// ---------------------------------------------------------------------------

RingBuffer::RingBuffer() {
    reset();
}

RingBuffer::~RingBuffer() = default;

int32_t RingBuffer::write(const int16_t* data, int32_t numSamples) {
    int64_t writeCursor = mWriteCursor.load(std::memory_order_relaxed);
    int64_t readCursor = mReadCursor.load(std::memory_order_acquire);

    int64_t used = writeCursor - readCursor;
    int64_t capacity = static_cast<int64_t>(kRingBufferCapacity);
    int64_t available = capacity - used;

    if (available <= 0) {
        // Buffer full — drop samples. Acceptable for ASR.
        return 0;
    }

    int32_t toWrite = static_cast<int32_t>(
        (numSamples < available) ? numSamples : available);

    for (int32_t i = 0; i < toWrite; ++i) {
        int32_t idx = static_cast<int32_t>((writeCursor + i) % kRingBufferCapacity);
        mBuffer[idx] = data[i];
    }

    mWriteCursor.store(writeCursor + toWrite, std::memory_order_release);
    return toWrite;
}

int32_t RingBuffer::read(int16_t* outData, int32_t maxSamples) {
    int64_t readCursor = mReadCursor.load(std::memory_order_relaxed);
    int64_t writeCursor = mWriteCursor.load(std::memory_order_acquire);

    int64_t available = writeCursor - readCursor;
    if (available <= 0) {
        return 0;
    }

    int32_t toRead = static_cast<int32_t>(
        (maxSamples < available) ? maxSamples : available);

    for (int32_t i = 0; i < toRead; ++i) {
        int32_t idx = static_cast<int32_t>((readCursor + i) % kRingBufferCapacity);
        outData[i] = mBuffer[idx];
    }

    mReadCursor.store(readCursor + toRead, std::memory_order_release);
    return toRead;
}

int32_t RingBuffer::available() const {
    int64_t writeCursor = mWriteCursor.load(std::memory_order_acquire);
    int64_t readCursor = mReadCursor.load(std::memory_order_acquire);
    return static_cast<int32_t>(writeCursor - readCursor);
}

void RingBuffer::reset() {
    mWriteCursor.store(0, std::memory_order_relaxed);
    mReadCursor.store(0, std::memory_order_relaxed);
}

// ---------------------------------------------------------------------------
// OboeAudioCapture
// ---------------------------------------------------------------------------

OboeAudioCapture::OboeAudioCapture() = default;

OboeAudioCapture::~OboeAudioCapture() {
    close();
}

oboe::AudioStreamBuilder OboeAudioCapture::buildStreamBuilder() {
    oboe::AudioStreamBuilder builder;

    builder.setDirection(oboe::Direction::Input)
           ->setSampleRate(mSampleRate)
           ->setChannelCount(mChannelCount)
           ->setFormat(oboe::AudioFormat::I16)
           ->setSharingMode(oboe::SharingMode::Exclusive)
           ->setPerformanceMode(oboe::PerformanceMode::None)
           ->setInputPreset(oboe::InputPreset::VoiceRecognition)
           ->setDataCallback(this)
           ->setErrorCallback(this);

    return builder;
}

bool OboeAudioCapture::open() {
    std::lock_guard<std::mutex> lock(mStreamMutex);
    if (mStream != nullptr) {
        return true; // already open
    }

    auto builder = buildStreamBuilder();

    oboe::Result result = builder.openStream(&mStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open input stream: %s", oboe::convertToText(result));
        return false;
    }

    // Record actual parameters (may differ from requested)
    mSampleRate = mStream->getSampleRate();
    mChannelCount = mStream->getChannelCount();

    LOGI("Opened input stream: %d Hz, %d channels",
         mSampleRate, mChannelCount);
    return true;
}

bool OboeAudioCapture::start() {
    std::lock_guard<std::mutex> lock(mStreamMutex);
    if (mStream == nullptr) {
        LOGE("Cannot start: stream is null");
        return false;
    }

    oboe::Result result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start stream: %s", oboe::convertToText(result));
        return false;
    }

    mActive.store(true, std::memory_order_release);
    LOGI("Capture started");
    return true;
}

int32_t OboeAudioCapture::readData(int16_t* outBuffer, int32_t maxFrames) {
    int32_t samplesToRead = maxFrames * mChannelCount;
    return mRingBuffer.read(outBuffer, samplesToRead);
}

void OboeAudioCapture::stop() {
    mActive.store(false, std::memory_order_release);
    mDataCv.notify_all();

    std::lock_guard<std::mutex> lock(mStreamMutex);
    if (mStream != nullptr) {
        oboe::Result result = mStream->requestStop();
        if (result != oboe::Result::OK) {
            LOGE("Failed to stop stream: %s", oboe::convertToText(result));
        }
    }

    LOGI("Capture stopped");
}

void OboeAudioCapture::close() {
    if (mClosed.exchange(true, std::memory_order_acq_rel)) {
        return; // already closed
    }

    mActive.store(false, std::memory_order_release);

    std::lock_guard<std::mutex> lock(mStreamMutex);
    if (mStream != nullptr) {
        mStream->requestStop();
        mStream->close();
        mStream = nullptr;
    }

    mRingBuffer.reset();
    mSampleRate = kSampleRate;
    mChannelCount = kChannelCount;

    LOGI("Capture closed");
}

bool OboeAudioCapture::isActive() const {
    return mActive.load(std::memory_order_acquire)
        && !mClosed.load(std::memory_order_acquire);
}

bool OboeAudioCapture::waitForData(int32_t timeoutMs) {
    std::unique_lock<std::mutex> lock(mWaitMutex);
    return mDataCv.wait_for(lock, std::chrono::milliseconds(timeoutMs),
        [this]{ return mRingBuffer.available() > 0 || !isActive(); });
}

int32_t OboeAudioCapture::readRingBuffer(int16_t* outData, int32_t maxSamples, int32_t timeoutMs) {
    std::unique_lock<std::mutex> lock(mWaitMutex);
    bool ready = mDataCv.wait_for(lock, std::chrono::milliseconds(timeoutMs),
        [this, maxSamples]{ return mRingBuffer.available() >= maxSamples || !isActive(); });

    if (!isActive()) {
        return -1;
    }

    if (!ready || mRingBuffer.available() < maxSamples) {
        return 0;
    }

    return mRingBuffer.read(outData, maxSamples);
}

oboe::DataCallbackResult OboeAudioCapture::onAudioReady(
    oboe::AudioStream* /*audioStream*/,
    void* audioData,
    int32_t numFrames)
{
    int32_t numSamples = numFrames * mChannelCount;
    auto* inputData = static_cast<const int16_t*>(audioData);

    mRingBuffer.write(inputData, numSamples);
    mDataCv.notify_one();

    return oboe::DataCallbackResult::Continue;
}

void OboeAudioCapture::onErrorAfterClose(
    oboe::AudioStream* /*audioStream*/,
    oboe::Result error)
{
    mActive.store(false, std::memory_order_release);

    if (mClosed.load(std::memory_order_acquire)) {
        return; // close() already cleaned up
    }

    std::lock_guard<std::mutex> lock(mStreamMutex);
    mStream = nullptr;
    LOGE("Stream error after close: %s", oboe::convertToText(error));
}
