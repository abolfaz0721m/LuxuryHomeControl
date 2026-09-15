# Keep native methods bound to JNI intact
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.example.wirelessmic.NativeGain { *; }
