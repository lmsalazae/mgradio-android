# Keep rules for Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep rules for Room DB
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Keep rules for Firebase / Firestore DTOs
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.mgradio.app.data.remote.model.** { *; }
-keep class com.mgradio.app.data.local.entity.** { *; }

# Keep rules for Hilt
-keep class * extends java.lang.annotation.Annotation { *; }
