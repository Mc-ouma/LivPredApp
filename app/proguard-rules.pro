# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# If you need to keep the original class names, uncomment this
# to prevent ProGuard from renaming the classes.
#keep models classes
-keep class com.soccertips.predictx.data.**{*;}

# Performance optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep essential classes for faster startup
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment

# Hilt optimizations
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Firebase optimizations
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# AdMob optimizations
-keep class com.google.android.gms.ads.** { *; }
-keep public class com.google.android.gms.ads.AdActivity
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.internal.** { *; }

# User Messaging Platform (UMP)
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**

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

