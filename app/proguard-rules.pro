# Convert-X ProGuard rules

# Kotlin metadata
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions

# Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class com.cedrickgd.convertx.**$$serializer { *; }
-keepclassmembers class com.cedrickgd.convertx.** {
    *** Companion;
}
-keepclasseswithmembers class com.cedrickgd.convertx.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# FFmpegKit uses JNI
-keep class com.arthenica.** { *; }
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.**

# Compose runtime internals sometimes referenced via reflection
-keep class androidx.compose.runtime.** { *; }

# Keep model classes for reflection/serialization
-keep class com.cedrickgd.convertx.domain.** { *; }
-keep class com.cedrickgd.convertx.engine.** { *; }
