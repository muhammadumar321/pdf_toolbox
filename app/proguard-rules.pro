# PdfBox-Android
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.apache.fontbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.apache.fontbox.**

# Kotlin
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.**
-dontwarn kotlinx.**

# AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# Navigation
-keep class * extends androidx.fragment.app.Fragment
