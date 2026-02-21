# Performance Measurement Instructions

This guide provides step-by-step instructions for measuring the performance improvements from the Wear OS optimizations.

---

## Prerequisites

### Hardware
- ✅ Wear OS device (physical or emulator)
- ✅ USB debugging enabled
- ✅ Computer with ADB installed

### Software
- ✅ Android SDK with ADB
- ✅ `bc` command (for calculations)
  - macOS: `brew install bc`
  - Linux: `sudo apt-get install bc`

### Setup Check
```bash
# Verify ADB can see your device
adb devices

# Should show something like:
# List of devices attached
# <serial>    device
```

---

## Quick Start (Recommended)

If you just want to measure the current optimized build vs a previous baseline:

### Option A: Compare Against Main Branch

```bash
# 1. Measure current optimized branch
./gradlew :wear:assembleDebug
./scripts/measure_performance.sh optimized

# 2. Switch to main branch
git stash push -m "Performance optimizations"
git checkout main

# 3. Build and measure baseline
./gradlew :wear:assembleDebug
./scripts/measure_performance.sh baseline

# 4. Return to your branch
git checkout -
git stash pop

# 5. Compare results (use most recent files)
./scripts/compare_performance.sh \
  performance_metrics/baseline_*.json \
  performance_metrics/optimized_*.json
```

### Option B: Before/After This Optimization Work

Since the optimizations are uncommitted changes, we need to selectively revert them for baseline:

```bash
# 1. Save current state (all optimizations + crash fix)
git add -A
git stash push -m "All performance work + crash fix"

# 2. Build baseline (current HEAD without optimizations)
./gradlew clean :wear:assembleDebug
./scripts/measure_performance.sh baseline

# 3. Restore optimizations + crash fix
git stash pop

# 4. Build optimized version
./gradlew clean :wear:assembleDebug
./scripts/measure_performance.sh optimized

# 5. Compare
./scripts/compare_performance.sh \
  performance_metrics/baseline_*.json \
  performance_metrics/optimized_*.json
```

---

## Detailed Measurement Process

### Step 1: Prepare Your Device

```bash
# 1. Connect device via ADB
adb devices

# 2. Uninstall any existing version (fresh start)
adb uninstall au.com.shiftyjelly.pocketcasts.debug

# 3. Clear logcat
adb logcat -c

# 4. Ensure device is not in power saving mode
# Check device settings: Settings > Battery > Battery Saver = OFF
```

### Step 2: Build Baseline Version

```bash
# Clean build to ensure no cached artifacts
./gradlew clean :wear:assembleDebug

# Verify APK exists
ls -lh wear/build/outputs/apk/debug/wear-debug.apk
```

### Step 3: Run Baseline Measurement

```bash
# If you have a physical device with specific serial:
./scripts/measure_performance.sh baseline <device_serial>

# For emulator or single connected device:
./scripts/measure_performance.sh baseline

# This will:
# 1. Clear app data
# 2. Measure APK size
# 3. Launch app and measure startup time
# 4. Measure memory footprint
# 5. Measure frame rendering stats
# 6. Save results to JSON
```

**Note the output location:**
```
Results saved to: performance_metrics/baseline_20260210_123456.json
```

### Step 4: Build Optimized Version

```bash
# Apply optimizations (git stash pop or checkout branch)
git stash pop

# Clean build
./gradlew clean :wear:assembleDebug
```

### Step 5: Run Optimized Measurement

```bash
# Use same device as baseline!
./scripts/measure_performance.sh optimized <same_device_serial>

# Note the output location:
# Results saved to: performance_metrics/optimized_20260210_123500.json
```

### Step 6: Compare Results

```bash
./scripts/compare_performance.sh \
  performance_metrics/baseline_20260210_123456.json \
  performance_metrics/optimized_20260210_123500.json

# This generates:
# 1. Terminal output with color-coded improvements
# 2. Markdown report: performance_metrics/comparison_report_*.md
```

---

## Understanding the Results

### Metrics Explained

#### 🚀 Cold Startup Time
**What it measures:** Time from app launch to first frame rendered
**Good result:** Negative percentage = faster startup
**Target:** -15% to -30% improvement

#### 📦 APK Size
**What it measures:** Installation size of the debug APK
**Good result:** Negative percentage = smaller APK
**Target:** -3 to -5 MB reduction (from Lottie removal)

#### 💾 Memory Footprint (Baseline)
**What it measures:** RAM usage after app startup (idle state)
- **Java Heap:** Dalvik/ART heap memory
- **Native Heap:** Native code allocations
- **Graphics:** GPU/bitmap memory
- **Total PSS:** Proportional Set Size (actual RAM used)

**Good result:** Negative percentage = less memory used
**Target:** -10% to -20% improvement

#### 💾 Memory Footprint (Peak)
**What it measures:** RAM usage after loading images and content
**Good result:** Negative percentage = less memory used
**Target:** -15% to -25% improvement

#### 🎨 Frame Rendering
**What it measures:** UI smoothness
- **Jank Rate:** % of frames that took >16ms to render
- **90th Percentile:** 90% of frames render faster than X ms
- **95th/99th Percentile:** Long tail performance

**Good result:** Lower jank rate and percentiles
**Target:** Similar or slightly improved (optimizations focus on memory/startup)

### Sample Good Results

```
Key Improvements:

  🚀 Startup Time:     -22.5% ⬇️ (better)
  💾 Memory (Base):    -18.3% ⬇️ (better)
  💾 Memory (Peak):    -21.7% ⬇️ (better)
  📦 APK Size:         -12.1% ⬇️ (better)
  🎨 Frame Jank:       -5.2% ⬇️ (better)

✓ Overall Performance Impact: POSITIVE
  Average improvement across metrics: -15.96%
```

---

## Troubleshooting

### "No device connected"
```bash
# Check USB debugging is enabled
adb devices

# If device shows as "unauthorized", accept prompt on device

# For emulator, make sure it's running
emulator -avd <your_wear_avd>
```

### "Permission denied" on scripts
```bash
chmod +x scripts/measure_performance.sh
chmod +x scripts/compare_performance.sh
```

### "bc: command not found"
```bash
# macOS
brew install bc

# Linux/Debian
sudo apt-get install bc

# Linux/Fedora
sudo dnf install bc
```

### APK not found
```bash
# Make sure you built the wear module specifically
./gradlew :wear:assembleDebug

# Check the path
ls -l wear/build/outputs/apk/debug/
```

### App not responding during measurement
```bash
# Force stop and retry
adb shell am force-stop au.com.shiftyjelly.pocketcasts.debug
adb shell pm clear au.com.shiftyjelly.pocketcasts.debug

# Re-run measurement
./scripts/measure_performance.sh <variant>
```

### Inconsistent results
Performance can vary due to:
- Device thermal state (let device cool between runs)
- Background processes
- Power saving modes
- Network conditions

**Best Practice:**
1. Run each measurement 3 times
2. Use the median result
3. Ensure device is in same state (temperature, battery level)
4. Close all other apps

---

## Advanced: Multiple Device Testing

### Test on Both Emulator and Physical Device

```bash
# Emulator
./scripts/measure_performance.sh baseline emulator-5554
./scripts/measure_performance.sh optimized emulator-5554

# Physical device
./scripts/measure_performance.sh baseline 1234ABCD5678
./scripts/measure_performance.sh optimized 1234ABCD5678

# Compare each
./scripts/compare_performance.sh \
  performance_metrics/baseline_*_emulator.json \
  performance_metrics/optimized_*_emulator.json

./scripts/compare_performance.sh \
  performance_metrics/baseline_*_physical.json \
  performance_metrics/optimized_*_physical.json
```

### Test on Low-End vs High-End Devices

Performance improvements often show larger impact on constrained devices:

```bash
# Low-end device (512MB RAM)
./scripts/measure_performance.sh baseline <low_end_serial>
./scripts/measure_performance.sh optimized <low_end_serial>

# High-end device (1GB+ RAM)
./scripts/measure_performance.sh baseline <high_end_serial>
./scripts/measure_performance.sh optimized <high_end_serial>
```

---

## What to Do With Results

### 1. Share the Comparison Report

The markdown report (`performance_metrics/comparison_report_*.md`) is perfect for:
- Pull Request descriptions
- Team updates
- Documentation

### 2. Monitor in Production

After releasing, track these metrics:
- Firebase Performance Monitoring: Cold startup time
- Firebase Performance Monitoring: Memory usage
- Crashlytics: Crash-free rate
- Play Console: ANR rate

### 3. Iterate

If results show:
- **Great improvements:** Document what worked, apply similar patterns elsewhere
- **Marginal improvements:** Investigate further, profile with Android Studio
- **Regressions:** Review specific optimizations, may need refinement

---

## Tips for Best Results

### 1. Clean Builds
Always use `./gradlew clean` before building for measurements to avoid cached artifacts.

### 2. Consistent Environment
- Same device for baseline and optimized
- Same Android version
- Same power state (not charging vs charging matters!)
- Same network conditions

### 3. Multiple Runs
Run each measurement at least 3 times and use median:
```bash
# Run 3 times
./scripts/measure_performance.sh baseline
./scripts/measure_performance.sh baseline
./scripts/measure_performance.sh baseline

# Pick the median startup time and memory values
```

### 4. Real-World Scenarios
The script measures cold startup with clean data. Also test:
- Warm startup (app in background)
- Typical usage (after signing in, loading content)
- Edge cases (poor network, low storage)

---

## Next Steps After Measurement

1. **Document Results**
   - Add comparison report to PR
   - Note any surprising findings
   - Explain any regressions

2. **Share With Team**
   - Discuss if improvements meet goals
   - Get feedback on methodology
   - Plan production rollout

3. **Production Monitoring**
   - Enable performance monitoring
   - Set up alerts for regressions
   - Track metrics over time

4. **Iterate**
   - Identify further optimization opportunities
   - Apply learnings to other modules
   - Continue measuring and improving

---

## Questions?

If you encounter issues or need clarification:
1. Check the troubleshooting section above
2. Review the main summary: `PERFORMANCE_OPTIMIZATION_SUMMARY.md`
3. Ask for help with specific error messages

Good luck with your measurements! 🚀
