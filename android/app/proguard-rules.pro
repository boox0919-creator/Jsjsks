# Keep JavascriptInterface methods exposed to the offline 3D WebView
-keepclassmembers class com.humanatlas.android.bridge.AnatomyJsBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
