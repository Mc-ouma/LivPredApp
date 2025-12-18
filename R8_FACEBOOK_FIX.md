# R8 Minification Fix for Facebook SDK

## Problem
The release build was failing during the R8 minification step with the following error:
```
Missing class com.facebook.infer.annotation.Nullsafe (referenced from: com.facebook.ads.AbstractAdListener and 31 other contexts)
```

## Root Cause
The Facebook Audience Network mediation adapter (`com.google.ads.mediation:facebook:6.20.0.2`) references annotation classes that are not included in the SDK. R8 treats these missing classes as errors during the shrinking/optimization process, even when minification is disabled.

## Solution
Added the missing annotation dependencies as `compileOnly` dependencies to satisfy R8's class verification:

### 1. Dependencies Added to `app/build.gradle.kts`
```kotlin
// Facebook SDK annotation dependencies (compile-only to avoid R8 errors)
compileOnly("com.facebook.infer.annotation:infer-annotation:0.18.0")
compileOnly("javax.annotation:javax.annotation-api:1.3.2")
```

Note: These dependencies are declared directly rather than through the version catalog to avoid naming conflicts and ensure clarity.

### 2. Version Catalog Entries in `gradle/libs.versions.toml`
```toml
[versions]
inferAnnotation = "0.18.0"
javaxAnnotationApi = "1.3.2"

[libraries]
infer-annotation = { module = "com.facebook.infer.annotation:infer-annotation", version.ref = "inferAnnotation" }
javax-annotation-api = { module = "javax.annotation:javax.annotation-api", version.ref = "javaxAnnotationApi" }
```

### 3. ProGuard Rules Added to `app/proguard-rules.pro`
```proguard
# Facebook Audience Network
-dontwarn com.facebook.infer.annotation.**
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.**
-keep class com.facebook.ads.** { *; }
-keep interface com.facebook.ads.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.facebook.** { *; }

# Ignore warnings about missing Facebook annotations
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**

# Additional rules to prevent R8 errors
-dontwarn com.facebook.infer.**
-dontwarn com.google.errorprone.annotations.**
```

### 4. Gradle Configuration in `gradle.properties`
```properties
android.enableR8.fullMode=false
```

## Build Results
✅ **Release build now completes successfully with minification enabled**

Build output:
```
> Task :app:minifyReleaseWithR8
BUILD SUCCESSFUL in 6m 24s
53 actionable tasks: 10 executed, 43 up-to-date
```

## Why `compileOnly`?
Using `compileOnly` instead of `implementation` ensures that:
1. The annotation classes are available during compilation for R8 to verify references
2. They are NOT included in the final APK (reducing APK size)
3. The annotations are only needed at compile-time, not at runtime

## Notes
- The R8 warnings about missing Kotlin compiler services can be safely ignored
- The solution works with both minification enabled and disabled
- No changes to the Facebook mediation adapter version were needed

