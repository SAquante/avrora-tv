# Keep serializable models and their generated serializers.
-keep class tv.iptv.app.data.** { *; }
-keepclassmembers class tv.iptv.app.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep media3/coil defaults that are looked up by name.
-dontwarn org.jetbrains.annotations.**
