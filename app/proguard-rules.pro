# ProGuard rules for MikLink
# ===========================

# Keep DTOs used by Moshi (backup - @JsonClass should handle this)
-keep class com.app.miklink.data.remote.mikrotik.dto.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory { *; }

# iText7 PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-dontwarn org.bouncycastle.**

# SLF4J (used by iText7)
-dontwarn org.slf4j.**

# Keep BuildConfig for runtime checks
-keep class com.app.miklink.BuildConfig { *; }
