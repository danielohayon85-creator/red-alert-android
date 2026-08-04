# Room, Hilt, and androidx.hilt:hilt-work all ship their own consumer ProGuard rules bundled in
# their AARs — nothing needed here for them. kotlinx.serialization's Gradle plugin also generates
# consumer rules automatically, but the block below (the project's official recommended rules) is
# kept as an explicit safety net since a broken release-only serialization crash from an R8 gap
# would be easy to miss without a release build actually running on a device.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.shomerapp.alerts.**$$serializer { *; }
-keepclassmembers class com.shomerapp.alerts.** {
    *** Companion;
}
-keepclasseswithmembers class com.shomerapp.alerts.** {
    kotlinx.serialization.KSerializer serializer(...);
}
