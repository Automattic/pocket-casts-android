# Output more information during processing
-verbose

# Make sure that the class loading doesn't fail if the file system is case-insesitive
-dontusemixedcaseclassnames

# Preserve some attributes that may be required for reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Assume isInEditMode() always return false in release builds so they can be pruned
-assumevalues public class * extends android.view.View {
  boolean isInEditMode() return false;
}

# Ensure the custom, fast service loader implementation is removed. R8 will fold these for us
-assumenosideeffects class kotlinx.coroutines.internal.MainDispatcherLoader {
    boolean FAST_SERVICE_LOADER_ENABLED return false;
}
-assumenosideeffects class kotlinx.coroutines.internal.FastServiceLoader {
    boolean ANDROID_DETECTED return true;
}
-checkdiscard class kotlinx.coroutines.internal.FastServiceLoader

# Protocol Buffers - keep the field names
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# https://github.com/square/retrofit/issues/4134
-if interface *
-keepclasseswithmembers,allowobfuscation interface <1> {
  @retrofit2.http.* <methods>;
}

# Do not optimize PlaybackService class.
# It causes an issue where the media notification is not displayed in some cases.
# https://github.com/shiftyjelly/pocketcasts-android/issues/1656
# https://github.com/shiftyjelly/pocketcasts-android/pulls/2921
-keep,allowobfuscation,allowshrinking class au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService { *; }

#
# ██      ███████  ██████   █████   ██████ ██    ██      ██████  ██████  ███    ██ ███████ ██  ██████
# ██      ██      ██       ██   ██ ██       ██  ██      ██      ██    ██ ████   ██ ██      ██ ██
# ██      █████   ██   ███ ███████ ██        ████       ██      ██    ██ ██ ██  ██ █████   ██ ██   ███
# ██      ██      ██    ██ ██   ██ ██         ██        ██      ██    ██ ██  ██ ██ ██      ██ ██    ██
# ███████ ███████  ██████  ██   ██  ██████    ██         ██████  ██████  ██   ████ ██      ██  ██████
#
# Configuration under this block is a legacy config that needs to be properly tested before removing.
# If you work on any of the entries please move them above and add an appropriate comment on what they do and why do we keep them.

# No explanation was provided for this rule
-keep public class android.support.v4.media.session.** { *; }

# Requested by Adam for MediaCompat and Android Auto
-keep class android.support.v4.media.** implements android.os.Parcelable {
   public static final ** CREATOR;
}

# Chrome cast
-keep class androidx.media3.cast.DefaultCastOptionsProvider { *; }
-keep class androidx.mediarouter.app.MediaRouteActionProvider { *; }
-keep class au.com.shiftyjelly.pocketcasts.CastOptionsProvider { *; }

# Keep layout classes
-keep class * extends android.view.View
