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

# OkHttp 3 / OkHttp 4 / Retrofit Rules
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.bouncycastle.jsse.provider.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Keep Moshi, Models, and Reflection data classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep Room database, entities, and DAOs
-keep class androidx.room.** { *; }
-keep class com.example.data.database.** { *; }
-keep class com.example.data.dao.** { *; }

# Keep UpdateChecker models and utilities
-keep class com.example.ui.util.** { *; }
-keepclassmembers class com.example.ui.util.** { *; }

