# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface com.ytmusicdl.app.data.api.** { *; }

# Gson models
-keep class com.ytmusicdl.app.data.api.** { *; }

# Coil
-keep class coil.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
