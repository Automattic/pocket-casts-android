#pragma once

#include <cstdint>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

class ClassifierHead {
public:
    static std::unique_ptr<ClassifierHead> loadFromFile(
        const std::string& path,
        int expectedHiddenSize = -1);

    int hiddenSize() const { return hidden_size_; }
    int numLabels() const { return num_labels_; }

    int argmax(const std::vector<float>& embedding) const;

    int argmaxPooled(
        const std::vector<const float*>& tokenEmbeddings,
        int poolStart,
        int poolEnd) const;

private:
    ClassifierHead(int numLabels, int hiddenSize, std::vector<float> weight, std::vector<float> bias);

    int num_labels_;
    int hidden_size_;
    std::vector<float> weight_;
    std::vector<float> bias_;

    static void l2Normalize(std::vector<float>& vector, float epsilon);
};

std::vector<std::string> loadLabelMap(const std::string& path);
