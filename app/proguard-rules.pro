# ==============================================================================
# R8 / ProGuard Configuration for AI ScoreCast (com.soccertips.predictx)
# Optimized for maximum shrinking, aggressive optimization, and high obfuscation.
# ==============================================================================

# Preserve line numbers and source file attributes for Crashlytics stack traces
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Allow R8 to aggressively modify access modifiers and repackage classes
-allowaccessmodification
-repackageclasses 'o'

# ------------------------------------------------------------------------------
# Data Models & Serialization (Gson / Room)
# ------------------------------------------------------------------------------
# Preserve all data models and their fields so Gson reflection matches JSON keys in Release
-keep class com.soccertips.predictx.data.model.** { *; }
-keepclassmembers class com.soccertips.predictx.data.model.** { *; }

# Keep fields serialized by Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# Keep custom JSON deserializers and adapters
-keep class com.soccertips.predictx.data.model.RootResponseDeserializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * extends com.google.gson.TypeAdapter { *; }

# Keep Room database entities & DAOs
-keep class com.soccertips.predictx.data.local.** { *; }
-keepclassmembers class com.soccertips.predictx.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep explicit @Keep annotations
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ------------------------------------------------------------------------------
# Retrofit & OkHttp
# ------------------------------------------------------------------------------
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ------------------------------------------------------------------------------
# WorkManager (HiltWorker)
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ------------------------------------------------------------------------------
# Third Party Warnings Suppression
# ------------------------------------------------------------------------------
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.ads.mediation.**
-dontwarn com.facebook.infer.annotation.**
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.lang.invoke.**
