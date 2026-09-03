cmake_minimum_required(VERSION 3.22)
project("pocketcasts-voice-capture" CXX)

# Apply 16KB page size alignment for Android 16
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")

# Enable ARM v8.2-A dot product instructions for quantized matmul on CPU
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.2-a+dotprod")
endif()

include(FetchContent)

# --- llama.cpp first so its ggml targets are authoritative for pocketcasts_voice_llm ---
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
    llama
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG 0eadefebd3f8f92a86d634a0e5b8fffc9dc792c0
    GIT_SHALLOW TRUE
)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(llama)

target_compile_options(ggml-base PRIVATE -O2)
target_compile_options(ggml-cpu PRIVATE -O2)
target_compile_options(ggml PRIVATE -O2)

add_library(pocketcasts_voice_llm SHARED
    lfm/ClassifierHead.cpp
    lfm/LfmRuntime.cpp
    lfm/LlmJni.cpp
)

target_include_directories(pocketcasts_voice_llm PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${llama_SOURCE_DIR}/include
    ${llama_SOURCE_DIR}/ggml/include
)

target_link_libraries(pocketcasts_voice_llm
    llama
    log
    android
)

target_compile_features(pocketcasts_voice_llm PUBLIC cxx_std_17)
target_compile_options(pocketcasts_voice_llm PRIVATE -Wall -Werror -O2 -fvisibility=hidden)

# --- whisper.cpp capture stack (separate shared library; dual ggml is temporary) ---
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG 1.9.0
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(oboe)

set(WHISPER_VULKAN OFF CACHE BOOL "" FORCE)
set(WHISPER_SDL2 OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
    whispercpp
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
    GIT_TAG v1.8.4
    GIT_SHALLOW TRUE
)
set(WHISPER_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(whispercpp)

add_library(pocketcasts_voice_capture SHARED
    OboeAudioCapture.cpp
    jni_bridge.cpp
    NativeVadProcessor.cpp
    VadJni.cpp
    EmbeddingJni.cpp
    WhisperJni.cpp
    WakeWordJni.cpp
)

target_include_directories(pocketcasts_voice_capture PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${oboe_SOURCE_DIR}/include
    ${whispercpp_SOURCE_DIR}/include
    ${whispercpp_SOURCE_DIR}/ggml/include
)

target_link_libraries(pocketcasts_voice_capture
    oboe
    log
    android
    whisper
)

target_compile_features(pocketcasts_voice_capture PUBLIC cxx_std_17)
target_compile_options(pocketcasts_voice_capture PRIVATE -Wall -Werror -Os -fvisibility=hidden)
