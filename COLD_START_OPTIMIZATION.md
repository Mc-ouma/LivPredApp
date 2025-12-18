# Cold Start Optimization Guide

## Overview

This document describes the cold start optimizations implemented to address the slow cold start rate on low-RAM devices (1.5-2GB RAM), which had a significantly higher slow cold start rate of 20.33% compared to the overall rate of 7.23%.

## Problem Statement

- **Affected devices**: Devices with 1.5-2GB of RAM (15% of total installs)
- **Slow cold start rate**: 20.33% (vs 7.23% overall)
- **Root cause**: Heavy initialization during app startup blocking the main thread

## Solution Architecture

### 1. Device Performance Manager (`DevicePerformanceManager.kt`)

A singleton utility that detects device capabilities and provides adaptive startup configuration.

**Performance Tiers:**
- `VERY_LOW`: < 1.5GB RAM - Aggressive optimizations
- `LOW`: 1.5-2GB RAM - Moderate optimizations (critical range)
- `STANDARD`: 2-4GB RAM - Minimal optimizations
- `HIGH`: > 4GB RAM - No optimizations needed

**Startup Configuration per Tier:**

| Config | VERY_LOW | LOW | STANDARD | HIGH |
|--------|----------|-----|----------|------|
| Ad Init Delay | 3000ms | 2000ms | 1000ms | 500ms |
| Preload Delay | 5000ms | 3000ms | 2000ms | 1000ms |
| Firebase Delay | 2000ms | 1500ms | 1000ms | 500ms |
| Skip Aggressive Preload | Yes | Yes | No | No |

### 2. Application Startup Optimizations (`App.kt`)

**Changes Made:**
1. Initialize `DevicePerformanceManager` as the first operation in `onCreate()`
2. Device-aware background initialization with adaptive delays
3. Deferred preloading on low-memory devices
4. Skip aggressive category data preloading on constrained devices
5. Longer stabilization delays for MobileAds initialization on low-RAM devices

### 3. SplashViewModel Optimizations

**Changes Made:**
1. Device-aware initialization delays
2. Skip review prefetch on low-memory devices
3. Shorter delays for essential-only operations

### 4. MainActivity Ad Initialization

**Changes Made:**
1. Device-aware ad manager setup in `AdInitializedContent`
2. Skip immediate ad preloading on low-memory devices
3. Deferred preloading with device-appropriate delays

### 5. Baseline Profile (`baseline-prof.txt`)

Added AOT compilation hints for critical startup paths:
- Application and MainActivity classes
- SplashViewModel initialization
- DevicePerformanceManager methods
- Core Compose and Navigation paths
- Coroutines and SharedPreferences

## Implementation Details

### How It Works

1. **Early Detection**: `DevicePerformanceManager.initialize()` is called in `App.onCreate()` before any heavy initialization
2. **Adaptive Configuration**: Startup config is retrieved based on detected device tier
3. **Deferred Operations**: Heavy operations are delayed based on device capabilities
4. **Background Processing**: Non-essential work moved to IO dispatcher with device-aware delays

### Key Optimizations by Device Tier

**LOW/VERY_LOW Memory Devices:**
- 500ms delay before any background initialization
- Skip aggressive category data preloading
- Defer ad manager initialization by 2-3 seconds
- Skip review info prefetch during startup
- Reduce retry counts for activity readiness checks

**STANDARD/HIGH Memory Devices:**
- Standard startup flow with minimal delays
- Aggressive preloading enabled
- Full feature initialization

## Expected Results

- **Target**: Reduce slow cold start rate on 1.5-2GB devices from 20.33% to ~10%
- **Trade-off**: Slightly delayed feature availability on low-RAM devices
- **User Experience**: Faster time-to-interactive, smoother initial UI

## Monitoring

The app logs device performance information on first launch:
```
App starting on low-memory device
Device is in critical memory range (1.5-2GB) - applying aggressive optimizations
DevicePerformanceManager initialized:
  - Total RAM: 1800MB
  - isLowRamDevice: false
  - Performance tier: LOW
```

## Testing Recommendations

1. Test on devices with 1.5-2GB RAM (e.g., Samsung Galaxy A10, Nokia 2.3)
2. Monitor cold start times via Firebase Performance
3. Compare before/after metrics in Play Console Android Vitals
4. Use `adb shell am start -W` to measure app startup time

## Dependencies Added

- `androidx.profileinstaller:profileinstaller:1.4.1` - For baseline profile installation

## Files Modified

- `DevicePerformanceManager.kt` (NEW) - Device detection and adaptive config
- `App.kt` - Application startup optimizations
- `MainActivity.kt` - Ad initialization optimizations
- `SplashViewModel.kt` - Splash screen optimizations
- `AppModule.kt` - Hilt module for DevicePerformanceManager
- `baseline-prof.txt` (NEW) - AOT compilation hints
- `libs.versions.toml` - Added profileinstaller dependency
- `build.gradle.kts` - Added profileinstaller implementation
