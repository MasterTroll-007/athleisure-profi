# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes Annotation
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.fitness.app.data.dto.**$$serializer { *; }
-keepclassmembers class com.fitness.app.data.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.fitness.app.data.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep DTOs for serialization
-keep class com.fitness.app.data.dto.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep R8 rules for runtime reflection
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === SECURITY HARDENING ===

# Aggressive obfuscation
-repackageclasses 'a'
-allowaccessmodification
-optimizationpasses 5

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Keep only error logs in release
-assumenosideeffects class android.util.Log {
    public static int e(...);
}

# Security-critical classes protection
-keep class com.fitness.app.data.local.TokenManager { *; }
-keep class com.fitness.app.data.api.TokenAuthenticator { *; }
-keep class com.fitness.app.data.api.AuthInterceptor { *; }

# Validation utilities
-keep class com.fitness.app.util.ValidationUtils { *; }

# OkHttp platform
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier

# Encrypted SharedPreferences
-keep class androidx.security.crypto.** { *; }
