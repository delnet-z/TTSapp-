# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.voiceapp.data.** { *; }
-keepclassmembers class com.voiceapp.data.** { *; }
