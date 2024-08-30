# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Without this the playback notification doesn't show up on fresh launch
# https://github.com/shiftyjelly/pocketcasts-android/issues/1656
-keep class au.com.shiftyjelly.pocketcasts.core.player.** { *; }

-dontwarn android.test.**
-dontwarn org.junit.internal.runners.statements.**
-dontwarn org.junit.rules.**
# retrolambda
-dontwarn java.lang.invoke.*
# okhttp
-dontwarn com.squareup.okhttp.**
-dontwarn org.conscrypt.**
-dontwarn okio.**
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions
-keepattributes *Annotation*

# kotlin
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# appcompat
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }
-keep public class android.support.v4.media.session.** { *; }
-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

# dagger 2
-dontwarn com.google.errorprone.annotations.*

-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.android.gms.measurement.**

# chrome cast
-keep class androidx.media3.cast.DefaultCastOptionsProvider { *; }
-keep class androidx.mediarouter.app.MediaRouteActionProvider { *; }
-keep class au.com.shiftyjelly.pocketcasts.CastOptionsProvider { *; }

-keepclassmembers enum * {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.Platform$Java8
-dontwarn javax.annotation.**

# moshi
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keep class **JsonAdapter {
    <init>(...);
    <fields>;
}
-keepnames @com.squareup.moshi.JsonClass class *

# Enum field names are used by the integrated EnumJsonAdapter.
# Annotate enums with @JsonClass(generateAdapter = false) to use them with Moshi.
-keepclassmembers @com.squareup.moshi.JsonClass class * extends java.lang.Enum {
    <fields>;
}

# Retain generated JsonAdapters if annotated type is retained.
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}

# Keep layout classes
-keep class * extends android.view.View

# clean up notes
-dontnote io.reactivex.**
-dontnote com.facebook.stetho.**
-dontnote com.afollestad.materialdialogs.internal.**
-dontnote com.astuetz.**
-dontnote com.google.android.gms.**
-dontnote com.google.android.material.**
-dontnote io.fabric.sdk.**
-dontnote com.google.firebase.**
-dontnote okhttp3.**
-dontnote retrofit2.**
-dontwarn com.google.common.**

# requested by Adam for MediaCompat and Android Auto
-keep class android.support.v4.media.** implements android.os.Parcelable {
   public static final ** CREATOR;
}

# corountines
-keepnames class kotlinx.coroutines.experimental.internal.MainDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

## remove our logs
#-assumenosideeffects class timber.log.Timber {
#    public static *** e(...);
#    public static *** d(...);
#    public static *** w(...);
#    public static *** i(...);
#}
#
#-assumenosideeffects class au.com.shiftyjelly.pocketcasts.core.helper.log.TimberHelper {
#    public static *** sql(...);
#}
