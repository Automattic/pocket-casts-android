#pragma once

#include "ClassifierHead.h"

#include "llama.h"

#include <memory>
#include <mutex>
#include <string>
#include <vector>

struct LfmRuntime {
    llama_model* model = nullptr;
    llama_context* context = nullptr;
    llama_batch batch{};
    bool batchInitialized = false;
    int batchCapacity = 0;

    std::unique_ptr<ClassifierHead> classifier;
    std::vector<std::string> labels;
    int hiddenSize = 0;
    llama_pos position = 0;

    bool load(
        const std::string& modelPath,
        const std::string& classifierPath,
        const std::string& labelMapPath,
        int nCtx);

    void release();

    void reset();

    std::vector<int> tokenize(const std::string& text, bool addBos) const;

    std::string classify(const std::vector<int>& promptTokenIds, int poolStart, int poolEnd);

    std::string generate(const std::string& prefill, int nPredict);

    std::string classifyEmbedding(const std::vector<float>& embedding) const;
};

class LfmRuntimeHolder {
public:
    static LfmRuntimeHolder& instance();

    bool loadClassifierOnly(const std::string& classifierPath, const std::string& labelMapPath, int expectedHiddenSize);
    std::string classifyEmbedding(const std::vector<float>& embedding);
    void releaseClassifierOnly();

    bool load(
        const std::string& modelPath,
        const std::string& classifierPath,
        const std::string& labelMapPath,
        int nCtx);
    std::string classify(const std::vector<int>& promptTokenIds, int poolStart, int poolEnd);
    std::string generate(const std::string& prefill, int nPredict);
    std::vector<int> tokenize(const std::string& text, bool addBos);
    void reset();
    void release();

private:
    LfmRuntimeHolder() = default;

    std::mutex mutex_;
    std::unique_ptr<LfmRuntime> runtime_;
    std::unique_ptr<ClassifierHead> classifierOnly_;
    std::vector<std::string> classifierOnlyLabels_;
};
