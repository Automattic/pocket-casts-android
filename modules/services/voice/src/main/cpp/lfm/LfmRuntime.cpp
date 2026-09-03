#include "LfmRuntime.h"

#include <android/log.h>

#include <cmath>
#include <cstring>
#include <stdexcept>

#define LOG_TAG "LfmRuntime"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

constexpr const char* kToolCallEnd = "<|tool_call_end|>";

class LfmError : public std::runtime_error {
public:
    explicit LfmError(const std::string& message) : std::runtime_error(message) {}
};

void ensureBatch(LfmRuntime& runtime, int32_t capacity) {
    // We always (re)allocate on a capacity change. A batched decode that reused a
    // larger buffer across calls is fine, but never write past the currently allocated
    // token count: llama_batch_init places a NULL sentinel at seq_id[capacity], and
    // decoding exactly `capacity` tokens can otherwise write through it to address 0.
    if (!runtime.batchInitialized || capacity != runtime.batchCapacity) {
        if (runtime.batchInitialized) {
            llama_batch_free(runtime.batch);
            runtime.batchInitialized = false;
        }
        runtime.batch = llama_batch_init(capacity, 0, 1);
        runtime.batchCapacity = capacity;
        runtime.batchInitialized = true;
    }
}

void clearBatch(LfmRuntime& runtime) {
    runtime.batch.n_tokens = 0;
}

bool decodeTokens(LfmRuntime& runtime, const std::vector<llama_token>& tokens, bool requestEmbeddings) {
    if (tokens.empty()) {
        return true;
    }
    ensureBatch(runtime, static_cast<int32_t>(tokens.size()));
    clearBatch(runtime);

    // Defensive: if the batch came back without the pointers we are about to write
    // (allocation failure), fail cleanly instead of dereferencing null during decode.
    if (runtime.batch.token == nullptr || runtime.batch.pos == nullptr ||
        runtime.batch.n_seq_id == nullptr || runtime.batch.seq_id == nullptr ||
        runtime.batch.logits == nullptr) {
        LOGE("batch buffers are null");
        return false;
    }

    for (std::size_t i = 0; i < tokens.size(); ++i) {
        const int index = runtime.batch.n_tokens;
        if (index >= runtime.batchCapacity) {
            LOGE("batch overflow at index %d (cap %d)", index, runtime.batchCapacity);
            return false;
        }
        runtime.batch.token[index] = tokens[i];
        runtime.batch.pos[index] = runtime.position + static_cast<llama_pos>(i);
        runtime.batch.n_seq_id[index] = 1;
        runtime.batch.seq_id[index][0] = 0;
        runtime.batch.logits[index] = requestEmbeddings ? 1 : (i + 1 == tokens.size() ? 1 : 0);
        runtime.batch.n_tokens++;
    }

    if (llama_decode(runtime.context, runtime.batch) != 0) {
        LOGE("llama_decode failed");
        return false;
    }
    runtime.position += static_cast<llama_pos>(tokens.size());
    return true;
}

std::vector<llama_token> toLlamaTokens(const std::vector<int>& tokenIds) {
    std::vector<llama_token> tokens;
    tokens.reserve(tokenIds.size());
    for (int token : tokenIds) {
        tokens.push_back(static_cast<llama_token>(token));
    }
    return tokens;
}

}  // namespace

LfmRuntimeHolder& LfmRuntimeHolder::instance() {
    static LfmRuntimeHolder holder;
    return holder;
}

bool LfmRuntime::load(
    const std::string& modelPath,
    const std::string& classifierPath,
    const std::string& labelMapPath,
    int nCtx) {
    release();

    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0;
    model = llama_model_load_from_file(modelPath.c_str(), modelParams);
    if (model == nullptr) {
        throw LfmError("failed to load GGUF model");
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = static_cast<uint32_t>(nCtx);
    ctxParams.n_batch = static_cast<uint32_t>(nCtx);
    ctxParams.n_ubatch = static_cast<uint32_t>(nCtx);
    ctxParams.pooling_type = LLAMA_POOLING_TYPE_NONE;
    ctxParams.embeddings = true;
    context = llama_init_from_model(model, ctxParams);
    if (context == nullptr) {
        llama_model_free(model);
        model = nullptr;
        throw LfmError("failed to create llama context");
    }

    hiddenSize = llama_model_n_embd(model);
    classifier = ClassifierHead::loadFromFile(classifierPath, hiddenSize);
    labels = loadLabelMap(labelMapPath);
    if (static_cast<int>(labels.size()) != classifier->numLabels()) {
        throw LfmError("label_map size mismatch");
    }

    ensureBatch(*this, static_cast<int32_t>(nCtx));
    position = 0;
    return true;
}

void LfmRuntime::release() {
    if (batchInitialized) {
        llama_batch_free(batch);
        batchInitialized = false;
        batchCapacity = 0;
    }
    if (context != nullptr) {
        llama_free(context);
        context = nullptr;
    }
    if (model != nullptr) {
        llama_model_free(model);
        model = nullptr;
    }
    classifier.reset();
    labels.clear();
    hiddenSize = 0;
    position = 0;
}

void LfmRuntime::reset() {
    if (context != nullptr) {
        llama_memory_clear(llama_get_memory(context), true);
    }
    position = 0;
    clearBatch(*this);
}

std::vector<int> LfmRuntime::tokenize(const std::string& text, bool addBos) const {
    if (model == nullptr) {
        throw LfmError("model not loaded");
    }
    const llama_vocab* vocab = llama_model_get_vocab(model);
    std::vector<llama_token> tokens(text.size() + 8);
    const int32_t count = llama_tokenize(
        vocab,
        text.c_str(),
        static_cast<int32_t>(text.size()),
        tokens.data(),
        static_cast<int32_t>(tokens.size()),
        addBos,
        true);
    if (count < 0) {
        tokens.resize(static_cast<std::size_t>(-count));
        const int32_t retry = llama_tokenize(
            vocab,
            text.c_str(),
            static_cast<int32_t>(text.size()),
            tokens.data(),
            static_cast<int32_t>(tokens.size()),
            addBos,
            true);
        if (retry < 0) {
            throw LfmError("tokenization failed");
        }
        tokens.resize(static_cast<std::size_t>(retry));
    } else {
        tokens.resize(static_cast<std::size_t>(count));
    }

    std::vector<int> out;
    out.reserve(tokens.size());
    for (llama_token token : tokens) {
        out.push_back(static_cast<int>(token));
    }
    return out;
}

std::string LfmRuntime::classify(
    const std::vector<int>& promptTokenIds,
    int poolStart,
    int poolEnd) {
    if (classifier == nullptr || context == nullptr) {
        throw LfmError("runtime not loaded");
    }
    reset();

    const auto tokens = toLlamaTokens(promptTokenIds);
    if (!decodeTokens(*this, tokens, true)) {
        throw LfmError("prompt decode failed");
    }

    std::vector<const float*> tokenEmbeddings(tokens.size());
    for (std::size_t i = 0; i < tokens.size(); ++i) {
        tokenEmbeddings[i] = llama_get_embeddings_ith(context, static_cast<int32_t>(i));
        if (tokenEmbeddings[i] == nullptr) {
            throw LfmError("missing token embedding");
        }
    }

    const int labelIndex = classifier->argmaxPooled(tokenEmbeddings, poolStart, poolEnd);
    if (labelIndex < 0 || labelIndex >= static_cast<int>(labels.size())) {
        throw LfmError("label index out of range");
    }
    return labels[static_cast<std::size_t>(labelIndex)];
}

std::string LfmRuntime::generate(const std::string& prefill, int nPredict) {
    if (context == nullptr || model == nullptr) {
        throw LfmError("runtime not loaded");
    }

    const auto prefillTokens = toLlamaTokens(tokenize(prefill, false));
    // Empty prefill leaves no decoded logits; sampling at -1 would be undefined.
    if (prefillTokens.empty()) {
        throw LfmError("empty prefill");
    }
    if (!decodeTokens(*this, prefillTokens, false)) {
        throw LfmError("prefill decode failed");
    }

    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    std::string generated = prefill;
    const llama_vocab* vocab = llama_model_get_vocab(model);

    for (int step = 0; step < nPredict; ++step) {
        const llama_token next = llama_sampler_sample(sampler, context, -1);
        llama_sampler_accept(sampler, next);

        if (llama_vocab_is_eog(vocab, next)) {
            break;
        }

        char piece[256];
        const int32_t pieceLen = llama_token_to_piece(vocab, next, piece, sizeof(piece), 0, true);
        if (pieceLen > 0) {
            generated.append(piece, static_cast<std::size_t>(pieceLen));
        }
        if (generated.find(kToolCallEnd) != std::string::npos) {
            break;
        }

        // Keep the existing decode-size batch (no per-token churn); only allocate a
        // fresh one when the (empty) prefill left no buffer behind.
        if (batchCapacity < 1) {
            ensureBatch(*this, 1);
        }
        clearBatch(*this);
        batch.token[0] = next;
        batch.pos[0] = position;
        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = 1;
        batch.n_tokens = 1;
        if (llama_decode(context, batch) != 0) {
            llama_sampler_free(sampler);
            throw LfmError("generation decode failed");
        }
        position += 1;
    }

    llama_sampler_free(sampler);
    return generated;
}

std::string LfmRuntime::classifyEmbedding(const std::vector<float>& embedding) const {
    if (classifier == nullptr) {
        throw LfmError("classifier not loaded");
    }
    const int labelIndex = classifier->argmax(embedding);
    if (labelIndex < 0 || labelIndex >= static_cast<int>(labels.size())) {
        throw LfmError("label index out of range");
    }
    return labels[static_cast<std::size_t>(labelIndex)];
}

bool LfmRuntimeHolder::loadClassifierOnly(
    const std::string& classifierPath,
    const std::string& labelMapPath,
    int expectedHiddenSize) {
    std::lock_guard<std::mutex> lock(mutex_);
    classifierOnly_ = ClassifierHead::loadFromFile(classifierPath, expectedHiddenSize);
    classifierOnlyLabels_ = loadLabelMap(labelMapPath);
    if (static_cast<int>(classifierOnlyLabels_.size()) != classifierOnly_->numLabels()) {
        classifierOnly_.reset();
        classifierOnlyLabels_.clear();
        return false;
    }
    return true;
}

std::string LfmRuntimeHolder::classifyEmbedding(const std::vector<float>& embedding) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (runtime_ != nullptr) {
        return runtime_->classifyEmbedding(embedding);
    }
    if (classifierOnly_ == nullptr) {
        throw LfmError("classifier not loaded");
    }
    const int labelIndex = classifierOnly_->argmax(embedding);
    if (labelIndex < 0 || labelIndex >= static_cast<int>(classifierOnlyLabels_.size())) {
        throw LfmError("label index out of range");
    }
    return classifierOnlyLabels_[static_cast<std::size_t>(labelIndex)];
}

void LfmRuntimeHolder::releaseClassifierOnly() {
    std::lock_guard<std::mutex> lock(mutex_);
    classifierOnly_.reset();
    classifierOnlyLabels_.clear();
}

bool LfmRuntimeHolder::load(
    const std::string& modelPath,
    const std::string& classifierPath,
    const std::string& labelMapPath,
    int nCtx) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto next = std::make_unique<LfmRuntime>();
    next->load(modelPath, classifierPath, labelMapPath, nCtx);
    if (runtime_ != nullptr) {
        runtime_->release();
    }
    runtime_ = std::move(next);
    classifierOnly_.reset();
    classifierOnlyLabels_.clear();
    return true;
}

std::string LfmRuntimeHolder::classify(
    const std::vector<int>& promptTokenIds,
    int poolStart,
    int poolEnd) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (runtime_ == nullptr) {
        throw LfmError("runtime not loaded");
    }
    return runtime_->classify(promptTokenIds, poolStart, poolEnd);
}

std::string LfmRuntimeHolder::generate(const std::string& prefill, int nPredict) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (runtime_ == nullptr) {
        throw LfmError("runtime not loaded");
    }
    return runtime_->generate(prefill, nPredict);
}

std::vector<int> LfmRuntimeHolder::tokenize(const std::string& text, bool addBos) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (runtime_ == nullptr) {
        throw LfmError("runtime not loaded");
    }
    return runtime_->tokenize(text, addBos);
}

void LfmRuntimeHolder::reset() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (runtime_ != nullptr) {
        runtime_->reset();
    }
}

void LfmRuntimeHolder::release() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (runtime_ != nullptr) {
        runtime_->release();
        runtime_.reset();
    }
    classifierOnly_.reset();
    classifierOnlyLabels_.clear();
}
