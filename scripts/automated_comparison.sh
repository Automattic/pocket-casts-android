#!/bin/bash
#
# Automated Performance Comparison Script
# Measures current branch vs main branch and generates comparison report
#
# Usage: ./scripts/automated_comparison.sh [device_serial]
#

set -e

DEVICE_SERIAL="${1:-}"
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
BASELINE_BRANCH="main"
PACKAGE="au.com.shiftyjelly.pocketcasts.debug"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Automated Performance Comparison${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "Current Branch: ${GREEN}${CURRENT_BRANCH}${NC}"
echo -e "Baseline Branch: ${GREEN}${BASELINE_BRANCH}${NC}"
echo ""

# Function to check if there are uncommitted changes
check_git_clean() {
    if ! git diff-index --quiet HEAD --; then
        echo -e "${YELLOW}Warning: You have uncommitted changes.${NC}"
        echo -e "These changes will be stashed temporarily during measurement."
        echo ""
        read -p "Continue? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Aborted."
            exit 1
        fi
        return 1
    fi
    return 0
}

# Function to stash changes
stash_changes() {
    echo -e "${YELLOW}Stashing changes...${NC}"
    git stash push -m "Automated performance comparison - $(date)" > /dev/null
    echo -e "${GREEN}✓ Changes stashed${NC}"
    echo ""
}

# Function to restore changes
restore_changes() {
    if git stash list | grep -q "Automated performance comparison"; then
        echo -e "${YELLOW}Restoring stashed changes...${NC}"
        git stash pop > /dev/null
        echo -e "${GREEN}✓ Changes restored${NC}"
        echo ""
    fi
}

# Function to uninstall app
uninstall_app() {
    echo -e "${YELLOW}Uninstalling app for clean measurement...${NC}"
    adb ${DEVICE_SERIAL:+-s $DEVICE_SERIAL} uninstall "$PACKAGE" 2>/dev/null || true
    echo -e "${GREEN}✓ App uninstalled${NC}"
    echo ""
}

# Check for uncommitted changes
HAS_CHANGES=false
if ! check_git_clean; then
    HAS_CHANGES=true
fi

# Step 1: Measure optimized version (current branch)
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 1/4: Measuring Optimized Version${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "Branch: ${GREEN}${CURRENT_BRANCH}${NC}"
echo ""

# Build optimized version
echo -e "${YELLOW}Building optimized version...${NC}"
./gradlew clean :wear:assembleDebug --quiet
echo -e "${GREEN}✓ Build complete${NC}"
echo ""

# Uninstall for clean measurement
uninstall_app

# Measure optimized
echo -e "${YELLOW}Running performance measurement...${NC}"
if [ -n "$DEVICE_SERIAL" ]; then
    ./scripts/measure_performance.sh optimized "$DEVICE_SERIAL"
else
    ./scripts/measure_performance.sh optimized
fi

# Find the most recent optimized JSON file
OPTIMIZED_JSON=$(ls -t performance_metrics/optimized_*.json | head -1)
echo ""
echo -e "${GREEN}✓ Optimized measurements saved to: ${OPTIMIZED_JSON}${NC}"
echo ""

# Step 2: Stash changes if needed
if [ "$HAS_CHANGES" = true ]; then
    stash_changes
fi

# Step 3: Measure baseline version (main branch)
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 2/4: Measuring Baseline Version${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Checkout baseline branch
echo -e "${YELLOW}Switching to ${BASELINE_BRANCH} branch...${NC}"
git checkout "$BASELINE_BRANCH" --quiet
echo -e "${GREEN}✓ Switched to ${BASELINE_BRANCH}${NC}"
echo ""

# Build baseline version
echo -e "${YELLOW}Building baseline version...${NC}"
./gradlew clean :wear:assembleDebug --quiet
echo -e "${GREEN}✓ Build complete${NC}"
echo ""

# Uninstall for clean measurement
uninstall_app

# Measure baseline
echo -e "${YELLOW}Running performance measurement...${NC}"
if [ -n "$DEVICE_SERIAL" ]; then
    ./scripts/measure_performance.sh baseline "$DEVICE_SERIAL"
else
    ./scripts/measure_performance.sh baseline
fi

# Find the most recent baseline JSON file
BASELINE_JSON=$(ls -t performance_metrics/baseline_*.json | head -1)
echo ""
echo -e "${GREEN}✓ Baseline measurements saved to: ${BASELINE_JSON}${NC}"
echo ""

# Step 4: Return to original branch
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 3/4: Returning to Original Branch${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}Switching back to ${CURRENT_BRANCH}...${NC}"
git checkout "$CURRENT_BRANCH" --quiet
echo -e "${GREEN}✓ Switched back to ${CURRENT_BRANCH}${NC}"
echo ""

# Restore stashed changes if needed
if [ "$HAS_CHANGES" = true ]; then
    restore_changes
fi

# Step 5: Generate comparison
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Step 4/4: Generating Comparison Report${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

./scripts/compare_performance.sh "$BASELINE_JSON" "$OPTIMIZED_JSON"

# Extract key metrics and create summary table
echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Quick Summary Table (Markdown)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to extract JSON value
get_json_value() {
    local file=$1
    local path=$2
    grep -o "\"${path}\": [^,}]*" "$file" | sed 's/.*: //' | sed 's/"//g' | head -1
}

# Function to calculate percentage change
calc_change() {
    local baseline=$1
    local optimized=$2
    echo "scale=2; (($optimized - $baseline) * 100) / $baseline" | bc
}

# Extract values
baseline_startup=$(get_json_value "$BASELINE_JSON" "total_time_ms")
optimized_startup=$(get_json_value "$OPTIMIZED_JSON" "total_time_ms")
startup_change=$(calc_change "$baseline_startup" "$optimized_startup")

baseline_apk=$(get_json_value "$BASELINE_JSON" "megabytes")
optimized_apk=$(get_json_value "$OPTIMIZED_JSON" "megabytes")
apk_change=$(calc_change "$baseline_apk" "$optimized_apk")

baseline_mem=$(get_json_value "$BASELINE_JSON" "total_pss_kb" | head -1)
optimized_mem=$(get_json_value "$OPTIMIZED_JSON" "total_pss_kb" | head -1)
mem_change=$(calc_change "$baseline_mem" "$optimized_mem")

baseline_peak=$(get_json_value "$BASELINE_JSON" "total_pss_kb" | tail -1)
optimized_peak=$(get_json_value "$OPTIMIZED_JSON" "total_pss_kb" | tail -1)
peak_change=$(calc_change "$baseline_peak" "$optimized_peak")

baseline_jank=$(get_json_value "$BASELINE_JSON" "jank_percentage")
optimized_jank=$(get_json_value "$OPTIMIZED_JSON" "jank_percentage")
jank_change=$(calc_change "$baseline_jank" "$optimized_jank")

# Get device info
device_model=$(get_json_value "$BASELINE_JSON" "model")

# Function to format change with emoji
format_change_md() {
    local change=$1
    change=$(echo "$change" | xargs)  # trim whitespace

    # Check if negative (improvement for most metrics)
    if [[ "$change" =~ ^- ]]; then
        echo "${change}% ✅"
    else
        echo "+${change}% ⚠️"
    fi
}

# Generate markdown table
cat << EOF

## Performance Comparison Results

**Device:** $device_model
**Baseline Branch:** \`$BASELINE_BRANCH\`
**Optimized Branch:** \`$CURRENT_BRANCH\`
**Date:** $(date +"%Y-%m-%d %H:%M:%S")

| Metric | Baseline ($BASELINE_BRANCH) | Optimized ($CURRENT_BRANCH) | Change | Impact |
|--------|-------------|------------|--------|--------|
| **🚀 Cold Startup** | ${baseline_startup}ms | ${optimized_startup}ms | $(format_change_md "$startup_change") | $([ $(echo "$startup_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |
| **📦 APK Size** | ${baseline_apk}MB | ${optimized_apk}MB | $(format_change_md "$apk_change") | $([ $(echo "$apk_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |
| **💾 Memory (Idle)** | ${baseline_mem}KB | ${optimized_mem}KB | $(format_change_md "$mem_change") | $([ $(echo "$mem_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |
| **💾 Memory (Peak)** | ${baseline_peak}KB | ${optimized_peak}KB | $(format_change_md "$peak_change") | $([ $(echo "$peak_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |
| **🎨 UI Jank Rate** | ${baseline_jank}% | ${optimized_jank}% | $(format_change_md "$jank_change") | $([ $(echo "$jank_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |

### Key Takeaways

EOF

# Add interpretation
overall_impact=$(echo "scale=2; ($startup_change + $mem_change + $peak_change + $apk_change + $jank_change) / 5" | bc)

if (( $(echo "$startup_change < 0" | bc -l) )); then
    echo "- ✅ **Startup time improved by ${startup_change#-}%** - App launches faster"
else
    echo "- ⚠️ **Startup time regressed by ${startup_change#+}%** - App launches slower"
fi

if (( $(echo "$apk_change < 0" | bc -l) )); then
    apk_reduction=$(echo "scale=2; $baseline_apk - $optimized_apk" | bc)
    echo "- ✅ **APK size reduced by ${apk_change#-}%** (saved ${apk_reduction}MB) - Faster downloads and installation"
else
    echo "- ⚠️ **APK size increased by ${apk_change#+}%** - Larger download size"
fi

if (( $(echo "$mem_change < 0" | bc -l) )); then
    echo "- ✅ **Memory footprint reduced by ${mem_change#-}%** - Better for low-RAM devices"
else
    echo "- ⚠️ **Memory footprint increased by ${mem_change#+}%** - Uses more RAM"
fi

if (( $(echo "$peak_change < 0" | bc -l) )); then
    echo "- ✅ **Peak memory reduced by ${peak_change#-}%** - Fewer OOM crashes expected"
else
    echo "- ⚠️ **Peak memory increased by ${peak_change#+}%** - May cause OOM on low-end devices"
fi

if (( $(echo "$jank_change < 0" | bc -l) )); then
    echo "- ✅ **UI smoothness improved** - Less jank during scrolling/animations"
elif (( $(echo "$jank_change > 5" | bc -l) )); then
    echo "- ⚠️ **UI smoothness regressed** - More jank noticed"
else
    echo "- ℹ️ **UI smoothness similar** - No significant change"
fi

echo ""
if (( $(echo "$overall_impact < -5" | bc -l) )); then
    echo "**Overall: ✅ SIGNIFICANT IMPROVEMENT** (avg ${overall_impact}% across metrics)"
elif (( $(echo "$overall_impact < 0" | bc -l) )); then
    echo "**Overall: ✅ Minor Improvement** (avg ${overall_impact}% across metrics)"
elif (( $(echo "$overall_impact > 5" | bc -l) )); then
    echo "**Overall: ⚠️ REGRESSION DETECTED** (avg +${overall_impact}% across metrics)"
else
    echo "**Overall: ℹ️ Neutral** (avg ${overall_impact}% across metrics)"
fi

echo ""
echo "---"
echo ""
echo "📊 **Detailed Reports:**"
echo "- Baseline: \`$BASELINE_JSON\`"
echo "- Optimized: \`$OPTIMIZED_JSON\`"
echo "- Full report: \`$(ls -t performance_metrics/comparison_report_*.md | head -1)\`"
echo ""

# Save the markdown table to a file
SUMMARY_FILE="performance_metrics/LATEST_COMPARISON.md"
{
    echo "## Performance Comparison Results"
    echo ""
    echo "**Device:** $device_model"
    echo "**Baseline Branch:** \`$BASELINE_BRANCH\`"
    echo "**Optimized Branch:** \`$CURRENT_BRANCH\`"
    echo "**Date:** $(date +"%Y-%m-%d %H:%M:%S")"
    echo ""
    echo "| Metric | Baseline ($BASELINE_BRANCH) | Optimized ($CURRENT_BRANCH) | Change | Impact |"
    echo "|--------|-------------|------------|--------|--------|"
    echo "| **🚀 Cold Startup** | ${baseline_startup}ms | ${optimized_startup}ms | $(format_change_md "$startup_change") | $([ $(echo "$startup_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |"
    echo "| **📦 APK Size** | ${baseline_apk}MB | ${optimized_apk}MB | $(format_change_md "$apk_change") | $([ $(echo "$apk_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |"
    echo "| **💾 Memory (Idle)** | ${baseline_mem}KB | ${optimized_mem}KB | $(format_change_md "$mem_change") | $([ $(echo "$mem_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |"
    echo "| **💾 Memory (Peak)** | ${baseline_peak}KB | ${optimized_peak}KB | $(format_change_md "$peak_change") | $([ $(echo "$peak_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |"
    echo "| **🎨 UI Jank Rate** | ${baseline_jank}% | ${optimized_jank}% | $(format_change_md "$jank_change") | $([ $(echo "$jank_change < 0" | bc) -eq 1 ] && echo "Better" || echo "Worse") |"
    echo ""
    echo "### Summary"
    echo ""
    if (( $(echo "$overall_impact < -5" | bc -l) )); then
        echo "**✅ SIGNIFICANT IMPROVEMENT** - Average ${overall_impact}% improvement across all metrics"
    elif (( $(echo "$overall_impact < 0" | bc -l) )); then
        echo "**✅ Minor Improvement** - Average ${overall_impact}% improvement across all metrics"
    elif (( $(echo "$overall_impact > 5" | bc -l) )); then
        echo "**⚠️ REGRESSION DETECTED** - Average +${overall_impact}% regression across all metrics"
    else
        echo "**ℹ️ Neutral** - Average ${overall_impact}% change across all metrics"
    fi
    echo ""
    echo "### Detailed Files"
    echo ""
    echo "- Baseline: \`$BASELINE_JSON\`"
    echo "- Optimized: \`$OPTIMIZED_JSON\`"
    echo "- Full report: \`$(ls -t performance_metrics/comparison_report_*.md | head -1)\`"
} > "$SUMMARY_FILE"

echo -e "${GREEN}✓ Summary saved to: ${SUMMARY_FILE}${NC}"
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Automated Comparison Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
