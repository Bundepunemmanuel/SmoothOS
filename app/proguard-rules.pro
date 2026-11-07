# Optimization rules for SmoothOS
-repackageclasses
-allowaccessmodification

# Keep OkHttp & Kotlin coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep data models
-keep class com.smoothos.app.model.** { *; }
