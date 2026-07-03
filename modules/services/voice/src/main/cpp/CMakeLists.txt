cmake_minimum_required(VERSION 3.22)
project("pocketcasts-voice-capture" CXX)

# Apply 16KB page size alignment for Android 16
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384")

# Enable ARM v8.2-A dot product instructions for quantized matmul on CPU
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8.2-a+dotprod")
endif()

include(FetchContent)

# Build Oboe from source (statically linked) to get proper 16KB segment alignment.
FetchContent_Declare(
    oboe
    GIT_REPOSITORY https://github.com/google/oboe.git
    GIT_TAG 1.9.0
    GIT_SHALLOW TRUE
)
FetchContent_MakeAvailable(oboe)

# Build whisper.cpp from source for on-device ASR.
# Disable GPU backends (Android NDK lacks glslc, Metal is Apple-only).
#
# Optimization notes:
#   - ggml matmul at -O0 (debug default) runs 20-50x slower; -O2 is forced below.
#   - flash_attn is a GPU-only feature; ggml-cpu ignores it, so don't bother.
#   - GGML_CPU_ALL_VERSIONS is redundant with -march=armv8.2-a+dotprod above.
#   - -march=armv8.2-a+dotprod + -O2 is the optimal CPU config for Pixel 8.
set(WHISPER_VULKAN OFF CACHE BOOL "" FORCE)
set(WHISPER_SDL2 OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
FetchContent_Declare(
    whispercpp
    GIT_REPOSITORY https://github.com/ggerganov/whisper.cpp.git
    GIT_TAG v1.8.4
    GIT_SHALLOW TRUE
)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(WHISPER_ALL_WARNINGS OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(whispercpp)

# Force -O2 on ggml targets even in debug builds — without this, ggml matmul at -O0
# takes 12–24s per encode pass on a Pixel 8 instead of well under a second.
target_compile_options(ggml-base PRIVATE -O2)
target_compile_options(ggml-cpu PRIVATE -O2)
target_compile_options(ggml PRIVATE -O2)

# --- pocketcasts_voice_capture (Oboe capture + VAD + Embedding + Whisper JNI) ---
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
