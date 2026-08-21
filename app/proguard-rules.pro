# --- General Kotlin / Reflection ---
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn kotlin.**
-dontwarn org.jetbrains.annotations.**

# --- Coroutines ---
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# --- Media3 / ExoPlayer & Guava ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.common.util.concurrent.** { *; }
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.**

# --- Room DB ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep interface com.mgradio.app.data.local.dao.** { *; }
-keep class com.mgradio.app.data.local.dao.**_Impl { *; }
-keep class * extends androidx.room.EntityDeletionOrUpdateAdapter { *; }
-keep class * extends androidx.room.EntityInsertionAdapter { *; }
-keep class * extends androidx.room.SharedSQLiteStatement { *; }
-dontwarn androidx.room.**

# --- Firebase / Firestore DTOs & Models ---
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.mgradio.app.data.remote.model.** { *; }
-keep class com.mgradio.app.data.local.entity.** { *; }
-keep class com.mgradio.app.domain.model.** { *; }
-keep class com.mgradio.app.media.** { *; }

# --- Hilt Dependency Injection ---
-keep class * extends java.lang.annotation.Annotation { *; }
-keep,allowobfuscation,allowshrinking interface *

# --- Coil Image Loader ---
-keep class coil.** { *; }
-dontwarn coil.**

