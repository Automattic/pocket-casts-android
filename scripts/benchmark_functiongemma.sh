#!/usr/bin/env bash
set -euo pipefail

# FunctionGemma Performance Benchmark
# Runs the benchmark instrumentation test on a connected device and extracts
# structured results from logcat.
#
# Usage: ./scripts/benchmark_functiongemma.sh [options]
#   -d DEVICE   Serial of target device (default: first from adb devices)
#   -r RUNS     Number of benchmark iterations (default: 1)
#   -s          Skip build (use already-installed APKs)
#   -o FILE     Write JSON results to FILE
#   -h          Show help

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

DEVICE=""
RUNS=1
SKIP_BUILD=false
OUTPUT_FILE=""

usage() {
    grep '^# ' "$0" | sed 's/^# //' | sed 's/^#//'
    exit 0
}

while getopts "d:r:so:h" opt; do
    case "$opt" in
        d) DEVICE="$OPTARG" ;;
        r) RUNS="$OPTARG" ;;
        s) SKIP_BUILD=true ;;
        o) OUTPUT_FILE="$OPTARG" ;;
        h) usage ;;
        *) usage ;;
    esac
done

ADB_CMD=(adb)
if [[ -n "$DEVICE" ]]; then
    ADB_CMD+=(-s "$DEVICE")
fi

require_device() {
    if [[ -z "$DEVICE" ]]; then
        DEVICES=$("${ADB_CMD[@]}" devices | tail -n +2 | grep -v "^$" | awk '{print $1}')
        DEVICE_COUNT=$(echo "$DEVICES" | grep -c . || true)
        if [[ "$DEVICE_COUNT" -eq 0 ]]; then
            echo "ERROR: No devices connected." >&2
            exit 1
        fi
        DEVICE=$(echo "$DEVICES" | head -1)
        echo "Using device: $DEVICE" >&2
    fi
    "${ADB_CMD[@]}" shell echo ok > /dev/null || {
        echo "ERROR: Cannot communicate with device $DEVICE" >&2
        exit 1
    }
}

build_apks() {
    echo "==> Building APKs..."
    cd "$PROJECT_DIR"

    if [[ "$SKIP_BUILD" == true ]]; then
        echo "Skipping build (--skip-build)."
        return
    fi

    ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --quiet || {
        echo "ERROR: Build failed." >&2
        exit 1
    }
    echo "Build OK."
}

install_apks() {
    echo "==> Installing APKs..."

    APK_DIR="$PROJECT_DIR/app/build/outputs/apk/debug"
    TEST_APK="$APK_DIR/app-debug-androidTest.apk"
    APP_APK="$APK_DIR/app-debug.apk"

    if [[ ! -f "$TEST_APK" ]]; then
        TEST_APK=$(find "$PROJECT_DIR/app/build/outputs" -name '*androidTest*.apk' | head -1)
    fi
    if [[ ! -f "$APP_APK" ]]; then
        APP_APK=$(find "$PROJECT_DIR/app/build/outputs" -name 'app-debug*.apk' | grep -v androidTest | head -1)
    fi

    echo "App APK:  $APP_APK"
    echo "Test APK: $TEST_APK"

    "${ADB_CMD[@]}" install -r -g "$APP_APK" || {
        echo "ERROR: Failed to install app APK." >&2
        exit 1
    }
    "${ADB_CMD[@]}" install -r -g "$TEST_APK" || {
        echo "ERROR: Failed to install test APK." >&2
        exit 1
    }
    echo "Install OK."
}

ensure_model() {
    echo "==> Checking FunctionGemma model..."
    local PKG="au.com.shiftyjelly.pocketcasts.debug"
    local MODEL_DIR="/data/data/$PKG/files/functiongemma-model"
    local HAS_MODEL
    HAS_MODEL=$("${ADB_CMD[@]}" shell run-as "$PKG" "[ -f $MODEL_DIR/model.litertlm ] && echo yes || echo no" 2>/dev/null || echo "no")
    if [[ "$HAS_MODEL" == "yes" ]]; then
        echo "Model files found on device."
        return
    fi

    local LOCAL_MODEL_DIR="$PROJECT_DIR/.functiongemma-model"
    if [[ -d "$LOCAL_MODEL_DIR" ]] && [[ -f "$LOCAL_MODEL_DIR/model.litertlm" ]]; then
        echo "Pushing model from $LOCAL_MODEL_DIR..."
        local CACHE_FILE
        CACHE_FILE=$(ls "$LOCAL_MODEL_DIR"/model.litertlm.xnnpack_cache_* 2>/dev/null | head -1)
        CACHE_NAME=$(basename "$CACHE_FILE")

        "${ADB_CMD[@]}" push "$LOCAL_MODEL_DIR/manifest.json" /data/local/tmp/fg_manifest.json
        "${ADB_CMD[@]}" push "$LOCAL_MODEL_DIR/model.litertlm" /data/local/tmp/fg_model.litertlm
        [[ -n "$CACHE_FILE" ]] && "${ADB_CMD[@]}" push "$CACHE_FILE" "/data/local/tmp/$CACHE_NAME"

        "${ADB_CMD[@]}" shell run-as "$PKG" "mkdir -p $MODEL_DIR && cp /data/local/tmp/fg_manifest.json $MODEL_DIR/manifest.json && cp /data/local/tmp/fg_model.litertlm $MODEL_DIR/model.litertlm"
        [[ -n "$CACHE_FILE" ]] && "${ADB_CMD[@]}" shell run-as "$PKG" "cp /data/local/tmp/$CACHE_NAME $MODEL_DIR/$CACHE_NAME"

        "${ADB_CMD[@]}" shell rm /data/local/tmp/fg_manifest.json /data/local/tmp/fg_model.litertlm 2>/dev/null || true
        [[ -n "$CACHE_FILE" ]] && "${ADB_CMD[@]}" shell rm "/data/local/tmp/$CACHE_NAME" 2>/dev/null || true
        echo "Model push OK."
        return
    fi

    echo "NOTE: Model not found on device or locally."
    echo "The benchmark will attempt to download it. This requires network access on the device."
    echo "To pre-seed the model, place files in: $LOCAL_MODEL_DIR"
}

clear_logcat() {
    echo "==> Clearing logcat..."
    "${ADB_CMD[@]}" logcat -c
}

run_benchmark() {
    local run_num="$1"
    echo "==> Run $run_num/$RUNS: Starting benchmark..."

    TEST_CLASS="au.com.shiftyjelly.pocketcasts.voicecontrol.benchmark.FunctionGemmaBenchmark"
    TEST_RUNNER="androidx.test.runner.AndroidJUnitRunner"

    # Run the benchmark test. Timeout: 30 minutes for model download + 100 requests.
    "${ADB_CMD[@]}" shell am instrument -w -r \
        -e class "$TEST_CLASS#runBenchmark" \
        -e timeout_msec 1800000 \
        "au.com.shiftyjelly.pocketcasts.testapp/$TEST_RUNNER" &

    ADB_PID=$!

    # Stream logcat in parallel
    "${ADB_CMD[@]}" logcat -v brief -s FunctionGemmaBenchmark:I System.err:W AndroidRuntime:E &

    LOGCAT_PID=$!

    wait $ADB_PID 2>/dev/null || true
    sleep 2
    kill $LOGCAT_PID 2>/dev/null || true
    wait $LOGCAT_PID 2>/dev/null || true
}

extract_results() {
    echo "==> Extracting results..."

    # Try result file first (most reliable)
    local PKG="au.com.shiftyjelly.pocketcasts.debug"
    local RESULT_FILE="/data/data/$PKG/files/functiongemma_benchmark_result.json"
    local HAS_FILE
    HAS_FILE=$("${ADB_CMD[@]}" shell run-as "$PKG" "[ -f $RESULT_FILE ] && echo yes || echo no" 2>/dev/null || echo "no")
    if [[ "$HAS_FILE" == "yes" ]]; then
        echo "Found result file on device."
        "${ADB_CMD[@]}" shell run-as "$PKG" cat "$RESULT_FILE" 2>/dev/null
        if [[ -n "$OUTPUT_FILE" ]]; then
            "${ADB_CMD[@]}" shell run-as "$PKG" cat "$RESULT_FILE" 2>/dev/null > "$OUTPUT_FILE"
            echo "Results written to: $OUTPUT_FILE"
        fi
        return 0
    fi

    # Fallback: search logcat
    echo "Result file not found, searching logcat..."
    local JSON_LINE
    JSON_LINE=$("${ADB_CMD[@]}" logcat -d -s FunctionGemmaBenchmark:I 2>/dev/null | grep "result_json=" | tail -1 || true)
    if [[ -z "$JSON_LINE" ]]; then
        echo "ERROR: No benchmark results found." >&2
        "${ADB_CMD[@]}" logcat -d -s FunctionGemmaBenchmark:I 2>/dev/null | tail -30 >&2
        return 1
    fi
    echo "$JSON_LINE"
}

print_summary() {
    echo "==> Summary from logcat..."
    "${ADB_CMD[@]}" logcat -d -s FunctionGemmaBenchmark:I 2>/dev/null | grep -E "phase=|progress=|result |meta " || {
        echo "No benchmark entries found in logcat."
    }
}

main() {
    require_device
    build_apks
    install_apks
    ensure_model
    clear_logcat

    for ((run = 1; run <= RUNS; run++)); do
        run_benchmark "$run"
        sleep 1
    done

    print_summary
    extract_results || true
}

main
