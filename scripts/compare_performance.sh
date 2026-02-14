#!/bin/bash
#
# Performance Comparison Script
# Compares baseline vs optimized performance metrics
#
# Usage: ./scripts/compare_performance.sh <baseline_json> <optimized_json>
#

set -e

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <baseline_json> <optimized_json>"
    exit 1
fi

BASELINE_FILE="$1"
OPTIMIZED_FILE="$2"

if [ ! -f "$BASELINE_FILE" ]; then
    echo "Error: Baseline file not found: $BASELINE_FILE"
    exit 1
fi

if [ ! -f "$OPTIMIZED_FILE" ]; then
    echo "Error: Optimized file not found: $OPTIMIZED_FILE"
    exit 1
fi

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to extract JSON value
get_json_value() {
    local file=$1
    local path=$2
    grep -o "\"${path}\": [^,}]*" "$file" | sed 's/.*: //' | sed 's/"//g'
}

# Function to calculate percentage change
calc_change() {
    local baseline=$1
    local optimized=$2

    # Check if baseline is empty or zero
    if [ -z "$baseline" ] || [ "$baseline" = "0" ] || [ "$baseline" = "0.00" ]; then
        echo "N/A"
        return
    fi

    # Check if optimized is empty
    if [ -z "$optimized" ]; then
        echo "N/A"
        return
    fi

    # Calculate change
    echo "scale=2; (($optimized - $baseline) * 100) / $baseline" | bc 2>/dev/null || echo "N/A"
}

# Function to format improvement (negative is better for most metrics)
format_improvement() {
    local change=$1
    local invert=${2:-false}  # For metrics where higher is worse

    # Remove leading/trailing spaces
    change=$(echo "$change" | xargs)

    # Handle N/A values
    if [ "$change" = "N/A" ]; then
        echo -e "${YELLOW}N/A${NC} (data unavailable)"
        return
    fi

    # Check if change is negative (improvement for most metrics)
    if [[ "$change" =~ ^- ]]; then
        if [ "$invert" = "true" ]; then
            echo -e "${RED}${change}%${NC} ⬆️ (worse)"
        else
            echo -e "${GREEN}${change}%${NC} ⬇️ (better)"
        fi
    else
        if [ "$invert" = "true" ]; then
            echo -e "${GREEN}+${change}%${NC} ⬆️ (better)"
        else
            echo -e "${RED}+${change}%${NC} ⬆️ (worse)"
        fi
    fi
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Performance Comparison Report${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Device info
baseline_device=$(get_json_value "$BASELINE_FILE" "model")
optimized_device=$(get_json_value "$OPTIMIZED_FILE" "model")

echo -e "${YELLOW}Device Information:${NC}"
echo -e "  Baseline: ${baseline_device}"
echo -e "  Optimized: ${optimized_device}"
echo ""

# APK Size
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}📦 APK Size${NC}"
echo -e "${BLUE}========================================${NC}"

baseline_apk=$(get_json_value "$BASELINE_FILE" "megabytes")
optimized_apk=$(get_json_value "$OPTIMIZED_FILE" "megabytes")
apk_change=$(calc_change "$baseline_apk" "$optimized_apk")

echo -e "  Baseline:  ${baseline_apk} MB"
echo -e "  Optimized: ${optimized_apk} MB"
echo -e "  Change:    $(format_improvement "$apk_change")"
echo ""

# Startup Time
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}🚀 Cold Startup Time${NC}"
echo -e "${BLUE}========================================${NC}"

baseline_startup=$(get_json_value "$BASELINE_FILE" "total_time_ms")
optimized_startup=$(get_json_value "$OPTIMIZED_FILE" "total_time_ms")
startup_change=$(calc_change "$baseline_startup" "$optimized_startup")

echo -e "  Baseline:  ${baseline_startup} ms"
echo -e "  Optimized: ${optimized_startup} ms"
echo -e "  Change:    $(format_improvement "$startup_change")"
echo ""

# Memory Baseline
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}💾 Memory Footprint (Baseline)${NC}"
echo -e "${BLUE}========================================${NC}"

baseline_mem=$(get_json_value "$BASELINE_FILE" "total_pss_kb" | head -1)
optimized_mem=$(get_json_value "$OPTIMIZED_FILE" "total_pss_kb" | head -1)
mem_change=$(calc_change "$baseline_mem" "$optimized_mem")

baseline_java=$(get_json_value "$BASELINE_FILE" "java_heap_kb")
optimized_java=$(get_json_value "$OPTIMIZED_FILE" "java_heap_kb")
java_change=$(calc_change "$baseline_java" "$optimized_java")

baseline_native=$(get_json_value "$BASELINE_FILE" "native_heap_kb")
optimized_native=$(get_json_value "$OPTIMIZED_FILE" "native_heap_kb")
native_change=$(calc_change "$baseline_native" "$optimized_native")

baseline_graphics=$(get_json_value "$BASELINE_FILE" "graphics_kb")
optimized_graphics=$(get_json_value "$OPTIMIZED_FILE" "graphics_kb")
graphics_change=$(calc_change "$baseline_graphics" "$optimized_graphics")

echo -e "  Total PSS:"
echo -e "    Baseline:  ${baseline_mem} KB"
echo -e "    Optimized: ${optimized_mem} KB"
echo -e "    Change:    $(format_improvement "$mem_change")"
echo ""
echo -e "  Java Heap:"
echo -e "    Baseline:  ${baseline_java} KB"
echo -e "    Optimized: ${optimized_java} KB"
echo -e "    Change:    $(format_improvement "$java_change")"
echo ""
echo -e "  Native Heap:"
echo -e "    Baseline:  ${baseline_native} KB"
echo -e "    Optimized: ${optimized_native} KB"
echo -e "    Change:    $(format_improvement "$native_change")"
echo ""
echo -e "  Graphics:"
echo -e "    Baseline:  ${baseline_graphics} KB"
echo -e "    Optimized: ${optimized_graphics} KB"
echo -e "    Change:    $(format_improvement "$graphics_change")"
echo ""

# Memory Peak
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}💾 Memory Footprint (Peak)${NC}"
echo -e "${BLUE}========================================${NC}"

baseline_peak=$(get_json_value "$BASELINE_FILE" "total_pss_kb" | tail -1)
optimized_peak=$(get_json_value "$OPTIMIZED_FILE" "total_pss_kb" | tail -1)
peak_change=$(calc_change "$baseline_peak" "$optimized_peak")

echo -e "  Baseline:  ${baseline_peak} KB"
echo -e "  Optimized: ${optimized_peak} KB"
echo -e "  Change:    $(format_improvement "$peak_change")"
echo ""

# Frame Rendering
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}🎨 Frame Rendering Performance${NC}"
echo -e "${BLUE}========================================${NC}"

baseline_jank=$(get_json_value "$BASELINE_FILE" "jank_percentage")
optimized_jank=$(get_json_value "$OPTIMIZED_FILE" "jank_percentage")
jank_change=$(calc_change "$baseline_jank" "$optimized_jank")

baseline_p90=$(get_json_value "$BASELINE_FILE" "percentile_90th_ms")
optimized_p90=$(get_json_value "$OPTIMIZED_FILE" "percentile_90th_ms")
p90_change=$(calc_change "$baseline_p90" "$optimized_p90")

baseline_p95=$(get_json_value "$BASELINE_FILE" "percentile_95th_ms")
optimized_p95=$(get_json_value "$OPTIMIZED_FILE" "percentile_95th_ms")
p95_change=$(calc_change "$baseline_p95" "$optimized_p95")

echo -e "  Jank Rate:"
echo -e "    Baseline:  ${baseline_jank}%"
echo -e "    Optimized: ${optimized_jank}%"
echo -e "    Change:    $(format_improvement "$jank_change")"
echo ""
echo -e "  90th Percentile Frame Time:"
echo -e "    Baseline:  ${baseline_p90} ms"
echo -e "    Optimized: ${optimized_p90} ms"
echo -e "    Change:    $(format_improvement "$p90_change")"
echo ""
echo -e "  95th Percentile Frame Time:"
echo -e "    Baseline:  ${baseline_p95} ms"
echo -e "    Optimized: ${optimized_p95} ms"
echo -e "    Change:    $(format_improvement "$p95_change")"
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}📊 Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Calculate overall score (simple weighted average of improvements)
# Negative is better
# Only include numeric values in the calculation
valid_count=0
sum=0

for value in "$startup_change" "$mem_change" "$peak_change" "$apk_change" "$jank_change"; do
    if [ "$value" != "N/A" ] && [ -n "$value" ]; then
        sum=$(echo "scale=2; $sum + $value" | bc 2>/dev/null || echo "$sum")
        valid_count=$((valid_count + 1))
    fi
done

if [ "$valid_count" -gt 0 ]; then
    overall_impact=$(echo "scale=2; $sum / $valid_count" | bc 2>/dev/null || echo "N/A")
else
    overall_impact="N/A"
fi

echo -e "Key Improvements:"
echo ""
echo -e "  🚀 Startup Time:     $(format_improvement "$startup_change")"
echo -e "  💾 Memory (Base):    $(format_improvement "$mem_change")"
echo -e "  💾 Memory (Peak):    $(format_improvement "$peak_change")"
echo -e "  📦 APK Size:         $(format_improvement "$apk_change")"
echo -e "  🎨 Frame Jank:       $(format_improvement "$jank_change")"
echo ""

if [ "$overall_impact" = "N/A" ]; then
    echo -e "${YELLOW}⚠ Overall Performance Impact: UNAVAILABLE${NC}"
    echo -e "  Insufficient data to calculate overall impact"
elif (( $(echo "$overall_impact < 0" | bc -l) )); then
    echo -e "${GREEN}✓ Overall Performance Impact: POSITIVE${NC}"
    echo -e "  Average improvement across metrics: ${GREEN}${overall_impact}%${NC}"
else
    echo -e "${RED}✗ Overall Performance Impact: NEGATIVE${NC}"
    echo -e "  Average regression across metrics: ${RED}+${overall_impact}%${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"

# Generate markdown report
REPORT_FILE="performance_metrics/comparison_report_$(date +%Y%m%d_%H%M%S).md"

cat > "$REPORT_FILE" <<EOF
# Performance Comparison Report

**Generated:** $(date)
**Baseline Device:** $baseline_device
**Optimized Device:** $optimized_device

## Summary

| Metric | Baseline | Optimized | Change | Impact |
|--------|----------|-----------|--------|--------|
| **Cold Startup** | ${baseline_startup}ms | ${optimized_startup}ms | ${startup_change}% | $([ $(echo "$startup_change < 0" | bc) -eq 1 ] && echo "✅ Better" || echo "❌ Worse") |
| **APK Size** | ${baseline_apk}MB | ${optimized_apk}MB | ${apk_change}% | $([ $(echo "$apk_change < 0" | bc) -eq 1 ] && echo "✅ Better" || echo "❌ Worse") |
| **Memory (Baseline)** | ${baseline_mem}KB | ${optimized_mem}KB | ${mem_change}% | $([ $(echo "$mem_change < 0" | bc) -eq 1 ] && echo "✅ Better" || echo "❌ Worse") |
| **Memory (Peak)** | ${baseline_peak}KB | ${optimized_peak}KB | ${peak_change}% | $([ $(echo "$peak_change < 0" | bc) -eq 1 ] && echo "✅ Better" || echo "❌ Worse") |
| **Jank Rate** | ${baseline_jank}% | ${optimized_jank}% | ${jank_change}% | $([ $(echo "$jank_change < 0" | bc) -eq 1 ] && echo "✅ Better" || echo "❌ Worse") |

## Detailed Breakdown

### 🚀 Startup Time
- **Baseline:** ${baseline_startup}ms
- **Optimized:** ${optimized_startup}ms
- **Change:** ${startup_change}%

### 📦 APK Size
- **Baseline:** ${baseline_apk}MB
- **Optimized:** ${optimized_apk}MB
- **Change:** ${apk_change}%

### 💾 Memory Footprint

#### Baseline State
- **Total PSS:** ${baseline_mem}KB → ${optimized_mem}KB (${mem_change}%)
- **Java Heap:** ${baseline_java}KB → ${optimized_java}KB (${java_change}%)
- **Native Heap:** ${baseline_native}KB → ${optimized_native}KB (${native_change}%)
- **Graphics:** ${baseline_graphics}KB → ${optimized_graphics}KB (${graphics_change}%)

#### Peak State
- **Total PSS:** ${baseline_peak}KB → ${optimized_peak}KB (${peak_change}%)

### 🎨 Frame Rendering
- **Jank Rate:** ${baseline_jank}% → ${optimized_jank}% (${jank_change}%)
- **90th Percentile:** ${baseline_p90}ms → ${optimized_p90}ms (${p90_change}%)
- **95th Percentile:** ${baseline_p95}ms → ${optimized_p95}ms (${p95_change}%)

## Conclusions

Overall performance impact: **${overall_impact}%** $([ $(echo "$overall_impact < 0" | bc) -eq 1 ] && echo "(Improvement ✅)" || echo "(Regression ❌)")

### Key Takeaways
- Cold startup time changed by ${startup_change}%
- Memory footprint changed by ${mem_change}%
- APK size changed by ${apk_change}%
- UI smoothness (jank) changed by ${jank_change}%

EOF

echo -e "Markdown report saved to: ${GREEN}$REPORT_FILE${NC}"
echo ""
