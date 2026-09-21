# --- Optimizaciones generales y trazas de pila legibles ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# --- Silenciar advertencias de dependencias externas ---
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.media3.**
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.**
-dontwarn androidx.room.**
-dontwarn coil.**

# --- Firebase Firestore (Serialización de DTOs por reflexión) ---
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.mgradio.app.data.remote.model.** { *; }

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep class com.mgradio.app.data.local.entity.** { *; }
-keep interface com.mgradio.app.data.local.dao.** { *; }
-keep class com.mgradio.app.data.local.dao.**_Impl { *; }
