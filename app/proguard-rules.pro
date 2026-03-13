-keep class org.libtorrent4j.** { *; }
-keep class com.frostwire.jlibtorrent.** { *; }

-keepattributes Signature
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.cineflux.**$$serializer { *; }
-keepclassmembers class com.cineflux.** {
    *** Companion;
}
