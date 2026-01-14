# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# ====================
# Room Database
# ====================
-keep class com.bowlingclub.fee.data.local.database.entity.** { *; }
-keep class com.bowlingclub.fee.data.local.database.dao.** { *; }

# ====================
# Hilt Dependency Injection
# ====================
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.* <methods>;
}
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# ====================
# Domain Models
# ====================
-keep class com.bowlingclub.fee.domain.model.** { *; }

# ====================
# ML Kit OCR
# ====================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ====================
# CameraX
# ====================
-keep class androidx.camera.** { *; }

# ====================
# Jetpack Compose
# ====================
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ====================
# Kotlin Coroutines
# ====================
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ====================
# DataStore
# ====================
-keep class androidx.datastore.** { *; }

# ====================
# Kotlin Serialization (if used)
# ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# ====================
# General Optimization
# ====================
-optimizationpasses 5
-dontusemixedcaseclassnames
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ====================
# Remove logging in release
# ====================
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
