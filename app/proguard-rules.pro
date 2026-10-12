# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.ludicindex.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.ludicindex.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
