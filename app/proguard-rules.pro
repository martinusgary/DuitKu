# --- ATURAN DASAR ANDROID ---
-dontwarn android.**
-dontwarn androidx.**
-keepattributes Signature, Exceptions, *Annotation*

# --- ROOM DATABASE ---
-dontwarn androidx.room.**
-keep class androidx.room.** { *; }

# --- RETROFIT & OKHTTP (INTERNET) ---
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# --- MOSHI (KONVERTER DATA) ---
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn com.squareup.moshi.**

# --- COROUTINES (PROSES LATAR BELAKANG) ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**