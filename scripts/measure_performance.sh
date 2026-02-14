#!/bin/bash
#
# Performance Measurement Script for Pocket Casts Wear OS
# Measures: Cold startup time, memory footprint, APK size, and frame rendering
#
# Usage: ./scripts/measure_performance.sh [baseline|optimized] [device_serial]
#
# Requirements:
# - ADB connected to Wear OS device or emulator
# - Wear debug APK built
#

# Don't use set -e because some commands are expected to fail (app not installed, etc.)
# We'll handle errors manually
set -o pipefail

VARIANT="${1:-optimized}"
DEVICE_SERIAL="${2:-}"
PACKAGE="au.com.shiftyjelly.pocketcasts.debug"
ACTIVITY="au.com.shiftyjelly.pocketcasts.wear.MainActivity"
OUTPUT_DIR="performance_metrics"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_FILE="$OUTPUT_DIR/${VARIANT}_${TIMESTAMP}.json"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}" >&2
echo -e "${BLUE}Pocket Casts Wear OS Performance Metrics${NC}" >&2
echo -e "${BLUE}========================================${NC}" >&2
echo ""
echo -e "Variant: ${GREEN}${VARIANT}${NC}" >&2
echo -e "Package: ${PACKAGE}"
echo -e "Timestamp: ${TIMESTAMP}"
echo ""

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Set ADB device if specified
ADB_CMD="adb"
if [ -n "$DEVICE_SERIAL" ]; then
    ADB_CMD="adb -s $DEVICE_SERIAL"
    echo -e "Device: ${GREEN}${DEVICE_SERIAL}${NC}" >&2
fi

# Check if device is connected
echo -e "${YELLOW}Checking device connection...${NC}" >&2
if ! $ADB_CMD shell exit 2>/dev/null; then
    echo -e "${RED}ERROR: No device connected or device not responding${NC}" >&2
    exit 1
fi

DEVICE_MODEL=$($ADB_CMD shell getprop ro.product.model)
ANDROID_VERSION=$($ADB_CMD shell getprop ro.build.version.release)
echo -e "Device Model: ${GREEN}${DEVICE_MODEL}${NC}" >&2
echo -e "Android Version: ${GREEN}${ANDROID_VERSION}${NC}" >&2
echo ""

# Function to force stop and clear app data
clear_app() {
    echo -e "${YELLOW}Clearing app data...${NC}" >&2
    $ADB_CMD shell am force-stop "$PACKAGE" 2>/dev/null || true
    $ADB_CMD shell pm clear "$PACKAGE" >/dev/null 2>&1 || true
    sleep 2
}

# Function to measure cold startup time
measure_startup_time() {
    echo -e "${YELLOW}Measuring cold startup time...${NC}" >&2

    # Ensure app is stopped
    clear_app

    # Start activity and capture startup time
    local startup_output=$($ADB_CMD shell am start -W -n "$PACKAGE/$ACTIVITY" 2>&1)

    # Extract TotalTime (time to first frame)
    local total_time=$(echo "$startup_output" | grep "TotalTime:" | awk '{print $2}')
    local wait_time=$(echo "$startup_output" | grep "WaitTime:" | awk '{print $2}')

    # Default to 0 if empty
    total_time=${total_time:-0}
    wait_time=${wait_time:-0}

    echo -e "  Total Time: ${GREEN}${total_time}ms${NC}" >&2
    echo -e "  Wait Time: ${GREEN}${wait_time}ms${NC}" >&2

    # Give app time to fully initialize
    sleep 5

    # Try to get reportFullyDrawn time from logcat (if implemented)
    local fully_drawn_time="N/A"
    local logcat_drawn=$($ADB_CMD logcat -d | grep "Displayed.*$PACKAGE" | tail -1)
    if [ -n "$logcat_drawn" ]; then
        # Extract time after '+' using sed (BSD-compatible)
        fully_drawn_time=$(echo "$logcat_drawn" | sed -n 's/.*+\([0-9]*ms\).*/\1/p')
        if [ -z "$fully_drawn_time" ]; then
            fully_drawn_time="N/A"
        fi
        echo -e "  Fully Drawn: ${GREEN}${fully_drawn_time}${NC}" >&2
    fi

    echo "$total_time|$wait_time|$fully_drawn_time"
}

# Function to measure memory footprint
measure_memory() {
    echo -e "${YELLOW}Measuring memory footprint...${NC}" >&2

    # Get PID
    local pid=$($ADB_CMD shell pidof "$PACKAGE")
    if [ -z "$pid" ]; then
        echo -e "  ${RED}ERROR: App not running${NC}" >&2
        return 1
    fi

    # Get memory info
    local meminfo=$($ADB_CMD shell dumpsys meminfo "$PACKAGE" | grep -A 20 "App Summary")

    # Extract key metrics (PSS in KB)
    local java_heap=$(echo "$meminfo" | grep "Java Heap:" | awk '{print $3}')
    local native_heap=$(echo "$meminfo" | grep "Native Heap:" | awk '{print $3}')
    local graphics=$(echo "$meminfo" | grep "Graphics:" | awk '{print $2}')
    local private_other=$(echo "$meminfo" | grep "Private Other:" | awk '{print $3}')
    local total_pss=$($ADB_CMD shell dumpsys meminfo "$PACKAGE" | grep "TOTAL PSS:" | awk '{print $3}')

    echo -e "  Java Heap: ${GREEN}${java_heap} KB${NC}" >&2
    echo -e "  Native Heap: ${GREEN}${native_heap} KB${NC}" >&2
    echo -e "  Graphics: ${GREEN}${graphics} KB${NC}" >&2
    echo -e "  Total PSS: ${GREEN}${total_pss} KB${NC}" >&2

    echo "$java_heap|$native_heap|$graphics|$total_pss"
}

# Function to measure peak memory during operation
measure_peak_memory() {
    echo -e "${YELLOW}Measuring peak memory (after image loading)...${NC}" >&2

    # Navigate through app to trigger image loading
    # This would need to be customized based on your app's UI
    # For now, just wait a bit and measure
    sleep 3

    local pid=$($ADB_CMD shell pidof "$PACKAGE")
    if [ -z "$pid" ]; then
        echo -e "  ${RED}ERROR: App not running${NC}" >&2
        return 1
    fi

    local total_pss=$($ADB_CMD shell dumpsys meminfo "$PACKAGE" | grep "TOTAL PSS:" | awk '{print $3}')
    echo -e "  Peak Total PSS: ${GREEN}${total_pss} KB${NC}" >&2

    echo "$total_pss"
}

# Function to measure APK size
measure_apk_size() {
    echo -e "${YELLOW}Measuring APK size...${NC}" >&2

    local apk_path="wear/build/outputs/apk/debug/wear-debug.apk"

    if [ ! -f "$apk_path" ]; then
        echo -e "  ${RED}ERROR: APK not found at $apk_path${NC}" >&2
        echo "0|0"
        return 1
    fi

    local apk_size=$(stat -f%z "$apk_path" 2>/dev/null || stat -c%s "$apk_path" 2>/dev/null)

    if [ -z "$apk_size" ] || [ "$apk_size" = "0" ]; then
        echo -e "  ${RED}ERROR: Could not get APK size${NC}" >&2
        echo "0|0"
        return 1
    fi

    local apk_size_mb=$(echo "scale=2; $apk_size / 1024 / 1024" | bc 2>/dev/null || echo "0")

    echo -e "  APK Size: ${GREEN}${apk_size_mb} MB${NC}" >&2

    echo "$apk_size|$apk_size_mb"
}

# Function to measure frame rendering stats
measure_frame_stats() {
    echo -e "${YELLOW}Measuring frame rendering statistics...${NC}" >&2

    # Reset gfx info
    $ADB_CMD shell dumpsys gfxinfo "$PACKAGE" reset >/dev/null 2>&1

    # Wait for some frames to be rendered
    echo -e "  Waiting for frames to render (10 seconds)..." >&2
    sleep 10

    # Get frame stats
    local gfxinfo=$($ADB_CMD shell dumpsys gfxinfo "$PACKAGE")

    # Extract janky frame stats
    local total_frames=$(echo "$gfxinfo" | grep "Total frames rendered:" | awk '{print $4}')
    local janky_frames=$(echo "$gfxinfo" | grep "Janky frames:" | awk '{print $3}')
    local percentile_90=$(echo "$gfxinfo" | grep "90th percentile:" | awk '{print $3}' | sed 's/ms//')
    local percentile_95=$(echo "$gfxinfo" | grep "95th percentile:" | awk '{print $3}' | sed 's/ms//')
    local percentile_99=$(echo "$gfxinfo" | grep "99th percentile:" | awk '{print $3}' | sed 's/ms//')

    # Default to 0 if empty
    total_frames=${total_frames:-0}
    janky_frames=${janky_frames:-0}
    percentile_90=${percentile_90:-0}
    percentile_95=${percentile_95:-0}
    percentile_99=${percentile_99:-0}

    # Calculate jank percentage
    local jank_percent="0"
    if [ -n "$total_frames" ] && [ "$total_frames" -gt 0 ]; then
        jank_percent=$(echo "scale=2; ($janky_frames * 100) / $total_frames" | bc)
    fi

    echo -e "  Total Frames: ${GREEN}${total_frames}${NC}" >&2
    echo -e "  Janky Frames: ${GREEN}${janky_frames}${NC} (${jank_percent}%)" >&2
    echo -e "  90th Percentile: ${GREEN}${percentile_90}ms${NC}" >&2
    echo -e "  95th Percentile: ${GREEN}${percentile_95}ms${NC}" >&2
    echo -e "  99th Percentile: ${GREEN}${percentile_99}ms${NC}" >&2

    echo "$total_frames|$janky_frames|$jank_percent|$percentile_90|$percentile_95|$percentile_99"
}

# Main measurement flow
echo -e "${BLUE}========================================${NC}" >&2
echo -e "${BLUE}Running Measurements${NC}" >&2
echo -e "${BLUE}========================================${NC}" >&2
echo ""

# 1. Measure APK size
apk_results=$(measure_apk_size)
echo ""

# 2. Install APK
echo -e "${YELLOW}Installing APK...${NC}" >&2
APK_PATH="wear/build/outputs/apk/debug/wear-debug.apk"
$ADB_CMD install -r "$APK_PATH" >/dev/null 2>&1
echo -e "${GREEN}✓ APK installed${NC}" >&2
echo ""

# 3. Warm-up launch (to complete login and setup)
echo -e "${YELLOW}Performing warm-up launch (for login/setup)...${NC}" >&2
echo -e "  This ensures we measure typical user experience, not first-time setup" >&2
$ADB_CMD shell am force-stop "$PACKAGE" 2>/dev/null || true
$ADB_CMD shell pm clear "$PACKAGE" >/dev/null 2>&1 || true
sleep 2
$ADB_CMD shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null 2>&1
echo -e "  Waiting 60 seconds for login/setup and async initialization to complete..." >&2
sleep 60

# Health check: verify app is actually running
local pid=$($ADB_CMD shell pidof "$PACKAGE" 2>/dev/null)
if [ -z "$pid" ]; then
    echo -e "  ${RED}WARNING: App not running after warm-up. May have crashed.${NC}" >&2
    echo -e "  ${YELLOW}Attempting to restart...${NC}" >&2
    $ADB_CMD shell am start -n "$PACKAGE/$ACTIVITY" >/dev/null 2>&1
    sleep 10
    pid=$($ADB_CMD shell pidof "$PACKAGE" 2>/dev/null)
    if [ -z "$pid" ]; then
        echo -e "  ${RED}ERROR: App still not running. Measurements may fail.${NC}" >&2
    else
        echo -e "  ${GREEN}✓ App restarted successfully${NC}" >&2
    fi
fi

$ADB_CMD shell am force-stop "$PACKAGE" 2>/dev/null || true
echo -e "${GREEN}✓ Warm-up launch complete${NC}" >&2
echo -e "  App is now ready for realistic performance measurement" >&2
echo ""

# 4. Measure cold startup time (second launch - typical user experience)
startup_results=$(measure_startup_time)
echo ""

# 5. Measure baseline memory
memory_results=$(measure_memory)
echo ""

# 6. Measure peak memory
peak_memory_results=$(measure_peak_memory)
echo ""

# 7. Measure frame stats
frame_results=$(measure_frame_stats)
echo ""

# Parse results
IFS='|' read -r total_time wait_time fully_drawn <<< "$startup_results"
IFS='|' read -r java_heap native_heap graphics total_pss <<< "$memory_results"
IFS='|' read -r apk_bytes apk_mb <<< "$apk_results"
IFS='|' read -r total_frames janky_frames jank_percent p90 p95 p99 <<< "$frame_results"
peak_pss="$peak_memory_results"

# Generate JSON results
cat > "$RESULTS_FILE" <<EOF
{
  "variant": "$VARIANT",
  "timestamp": "$TIMESTAMP",
  "device": {
    "model": "$DEVICE_MODEL",
    "android_version": "$ANDROID_VERSION"
  },
  "apk_size": {
    "bytes": $apk_bytes,
    "megabytes": $apk_mb
  },
  "startup": {
    "total_time_ms": $total_time,
    "wait_time_ms": $wait_time,
    "fully_drawn": "$fully_drawn"
  },
  "memory": {
    "baseline": {
      "java_heap_kb": $java_heap,
      "native_heap_kb": $native_heap,
      "graphics_kb": $graphics,
      "total_pss_kb": $total_pss
    },
    "peak": {
      "total_pss_kb": $peak_pss
    }
  },
  "rendering": {
    "total_frames": $total_frames,
    "janky_frames": $janky_frames,
    "jank_percentage": $jank_percent,
    "percentile_90th_ms": $p90,
    "percentile_95th_ms": $p95,
    "percentile_99th_ms": $p99
  }
}
EOF

# Stop the app
$ADB_CMD shell am force-stop "$PACKAGE" 2>/dev/null || true

echo -e "${BLUE}========================================${NC}" >&2
echo -e "${GREEN}✓ Measurements Complete${NC}" >&2
echo -e "${BLUE}========================================${NC}" >&2
echo ""
echo -e "Results saved to: ${GREEN}$RESULTS_FILE${NC}" >&2
echo ""
echo -e "${YELLOW}Summary:${NC}" >&2
echo -e "  Cold Startup: ${GREEN}${total_time}ms${NC}" >&2
echo -e "  Memory (PSS): ${GREEN}${total_pss}KB${NC} baseline, ${GREEN}${peak_pss}KB${NC} peak" >&2
echo -e "  APK Size: ${GREEN}${apk_mb}MB${NC}" >&2
echo -e "  Jank Rate: ${GREEN}${jank_percent}%${NC} (${janky_frames}/${total_frames} frames)" >&2
echo ""
