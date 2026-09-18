# R8 / ProGuard rules for Katzu Production Release

# Strip standard Android Log debug/verbose calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# PostHog Analytics
-keep class com.posthog.** { *; }
-dontwarn com.posthog.**

# Retain Parcelable & Serializable model classes
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}
