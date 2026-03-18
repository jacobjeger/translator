# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.megalife.translator.data.model.** { *; }
-keep class com.megalife.translator.data.remote.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
