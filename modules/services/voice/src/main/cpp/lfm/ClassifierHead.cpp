#include "ClassifierHead.h"

#include <cmath>
#include <cstring>
#include <fstream>
#include <limits>
#include <sstream>

namespace {

constexpr char kMagic[4] = {'L', 'F', 'M', 'C'};
constexpr float kL2Epsilon = 1e-6f;

class ClassifierLoadError : public std::runtime_error {
public:
    explicit ClassifierLoadError(const std::string& message) : std::runtime_error(message) {}
};

void readExact(std::ifstream& input, void* buffer, std::size_t size) {
    input.read(reinterpret_cast<char*>(buffer), static_cast<std::streamsize>(size));
    if (!input) {
        throw ClassifierLoadError("classifier.bin read failed");
    }
}

uint32_t readU32LE(const uint8_t* bytes) {
    // classifier.bin is little-endian on disk; load explicitly so host-endian hosts
    // cannot silently misread the header on a future big-endian exporter/host.
    return static_cast<uint32_t>(bytes[0])
        | (static_cast<uint32_t>(bytes[1]) << 8)
        | (static_cast<uint32_t>(bytes[2]) << 16)
        | (static_cast<uint32_t>(bytes[3]) << 24);
}

std::string unescapeJson(const std::string& value) {
    std::string out;
    out.reserve(value.size());
    for (std::size_t i = 0; i < value.size(); ++i) {
        if (value[i] == '\\' && i + 1 < value.size()) {
            out.push_back(value[++i]);
        } else {
            out.push_back(value[i]);
        }
    }
    return out;
}

}  // namespace

ClassifierHead::ClassifierHead(
    int numLabels,
    int hiddenSize,
    std::vector<float> weight,
    std::vector<float> bias)
    : num_labels_(numLabels),
      hidden_size_(hiddenSize),
      weight_(std::move(weight)),
      bias_(std::move(bias)) {}

std::unique_ptr<ClassifierHead> ClassifierHead::loadFromFile(
    const std::string& path,
    int expectedHiddenSize) {
    std::ifstream input(path, std::ios::binary);
    if (!input) {
        throw ClassifierLoadError("classifier.bin open failed");
    }

    uint8_t header[12] = {};
    readExact(input, header, sizeof(header));
    if (std::memcmp(header, kMagic, sizeof(kMagic)) != 0) {
        throw ClassifierLoadError("invalid classifier.bin magic");
    }

    const uint32_t numLabels = readU32LE(header + 4);
    const uint32_t hiddenSize = readU32LE(header + 8);
    if (numLabels == 0 || hiddenSize == 0) {
        throw ClassifierLoadError("classifier.bin has zero labels or hidden size");
    }
    if (expectedHiddenSize > 0 && static_cast<int>(hiddenSize) != expectedHiddenSize) {
        throw ClassifierLoadError("classifier hidden_size mismatch");
    }

    const std::size_t weightCount = static_cast<std::size_t>(numLabels) * hiddenSize;
    const std::size_t weightBytes = weightCount * sizeof(float);
    const std::size_t biasBytes = static_cast<std::size_t>(numLabels) * sizeof(float);
    const std::size_t expectedBytes = 12 + weightBytes + biasBytes;

    input.seekg(0, std::ios::end);
    const std::size_t fileBytes = static_cast<std::size_t>(input.tellg());
    if (fileBytes != expectedBytes) {
        throw ClassifierLoadError("classifier.bin size mismatch");
    }
    input.seekg(12, std::ios::beg);

    std::vector<float> weight(weightCount);
    std::vector<float> bias(numLabels);
    readExact(input, weight.data(), weightBytes);
    readExact(input, bias.data(), biasBytes);

    return std::unique_ptr<ClassifierHead>(
        new ClassifierHead(static_cast<int>(numLabels), static_cast<int>(hiddenSize), std::move(weight), std::move(bias)));
}

void ClassifierHead::l2Normalize(std::vector<float>& vector, float epsilon) {
    double sumSquares = 0.0;
    for (float value : vector) {
        sumSquares += static_cast<double>(value) * static_cast<double>(value);
    }
    const double norm = std::sqrt(sumSquares);
    const double scale = 1.0 / std::max(norm, static_cast<double>(epsilon));
    for (float& value : vector) {
        value = static_cast<float>(static_cast<double>(value) * scale);
    }
}

int ClassifierHead::argmax(const std::vector<float>& embedding) const {
    if (static_cast<int>(embedding.size()) != hidden_size_) {
        throw ClassifierLoadError("embedding hidden_size mismatch");
    }

    std::vector<float> normalized = embedding;
    l2Normalize(normalized, kL2Epsilon);

    int bestIndex = 0;
    float bestScore = -std::numeric_limits<float>::infinity();
    for (int label = 0; label < num_labels_; ++label) {
        float score = bias_[static_cast<std::size_t>(label)];
        const float* row = weight_.data() + static_cast<std::size_t>(label) * hidden_size_;
        for (int dim = 0; dim < hidden_size_; ++dim) {
            score += row[dim] * normalized[static_cast<std::size_t>(dim)];
        }
        if (score > bestScore) {
            bestScore = score;
            bestIndex = label;
        }
    }
    return bestIndex;
}

int ClassifierHead::argmaxPooled(
    const std::vector<const float*>& tokenEmbeddings,
    int poolStart,
    int poolEnd) const {
    if (poolStart < 0 || poolEnd < poolStart || poolEnd >= static_cast<int>(tokenEmbeddings.size())) {
        throw ClassifierLoadError("invalid pool span");
    }

    std::vector<float> pooled(static_cast<std::size_t>(hidden_size_), 0.0f);
    const int span = poolEnd - poolStart + 1;
    for (int index = poolStart; index <= poolEnd; ++index) {
        const float* embedding = tokenEmbeddings[static_cast<std::size_t>(index)];
        if (embedding == nullptr) {
            throw ClassifierLoadError("null token embedding");
        }
        for (int dim = 0; dim < hidden_size_; ++dim) {
            pooled[static_cast<std::size_t>(dim)] += embedding[dim];
        }
    }
    for (float& value : pooled) {
        value /= static_cast<float>(span);
    }
    return argmax(pooled);
}

std::vector<std::string> loadLabelMap(const std::string& path) {
    std::ifstream input(path);
    if (!input) {
        throw ClassifierLoadError("label_map.json open failed");
    }
    std::ostringstream buffer;
    buffer << input.rdbuf();
    const std::string json = buffer.str();

    const std::string key = "\"labels\"";
    const auto keyPos = json.find(key);
    if (keyPos == std::string::npos) {
        throw ClassifierLoadError("label_map.json missing labels");
    }
    const auto arrayStart = json.find('[', keyPos);
    const auto arrayEnd = json.find(']', arrayStart);
    if (arrayStart == std::string::npos || arrayEnd == std::string::npos || arrayEnd <= arrayStart) {
        throw ClassifierLoadError("label_map.json labels array invalid");
    }

    std::vector<std::string> labels;
    std::size_t cursor = arrayStart + 1;
    while (cursor < arrayEnd) {
        const auto quoteStart = json.find('"', cursor);
        if (quoteStart == std::string::npos || quoteStart >= arrayEnd) {
            break;
        }
        std::size_t quoteEnd = quoteStart + 1;
        while (quoteEnd < arrayEnd) {
            if (json[quoteEnd] == '"' && json[quoteEnd - 1] != '\\') {
                break;
            }
            quoteEnd++;
        }
        if (quoteEnd >= arrayEnd) {
            throw ClassifierLoadError("label_map.json unterminated label");
        }
        labels.push_back(unescapeJson(json.substr(quoteStart + 1, quoteEnd - quoteStart - 1)));
        cursor = quoteEnd + 1;
    }
    if (labels.empty()) {
        throw ClassifierLoadError("label_map.json has no labels");
    }
    return labels;
}
