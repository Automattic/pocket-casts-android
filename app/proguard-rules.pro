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

# glide
-keep class com.bumptech.glide.integration.okhttp3.OkHttpGlideModule
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

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

# Resolve AGP 8.0 update missing class errors by keeping the current behavior 
# Example failure:
# 
# > Task :modules:services:model:minifyReleaseWithR8 FAILED
# ERROR: Missing classes detected while running R8. Please add the missing classes or apply additional keep rules that are generated in ~/pocket-casts-android/modules/services/model/build/outputs/mapping/release/missing_rules.txt.
# ERROR: R8: Missing class au.com.shiftyjelly.pocketcasts.localization.R$string (referenced from: void au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency.<clinit>())
-dontwarn au.com.shiftyjelly.pocketcasts.*.R$attr
-dontwarn au.com.shiftyjelly.pocketcasts.*.R$color
-dontwarn au.com.shiftyjelly.pocketcasts.*.R$drawable
-dontwarn au.com.shiftyjelly.pocketcasts.*.R$id
-dontwarn au.com.shiftyjelly.pocketcasts.*.R$layout
-dontwarn au.com.shiftyjelly.pocketcasts.*.R$string
-dontwarn au.com.shiftyjelly.pocketcasts.*.R$styleable
-dontwarn com.google.android.material.R$attr
-dontwarn com.google.android.material.R$dimen
-dontwarn com.google.android.material.R$style
-dontwarn com.google.android.material.R$styleable

# R8 full mode strips generic signatures from return types if not kept.
# This stop the app crashing on startup and will only be required until Retrofit 2.10.0 is released as it's included https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
