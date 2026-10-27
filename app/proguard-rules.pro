# ProGuard rules for PDF Studio
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
