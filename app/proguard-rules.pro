# JavaMail/Activation use reflection and service provider loading.
-keep class com.sun.mail.** { *; }
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-dontwarn javax.mail.**
-dontwarn javax.activation.**

# Preserve the deliberately narrow WebView font bridge.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
