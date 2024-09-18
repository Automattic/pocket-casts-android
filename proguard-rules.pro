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

# https://github.com/google/dagger/issues/4323
-keepclasseswithmembers,includedescriptorclasses class * {
   @dagger.internal.KeepFieldType <fields>;
}

# https://github.com/square/retrofit/issues/4134
-if interface *
-keepclasseswithmembers,allowobfuscation interface <1> {
  @retrofit2.http.* <methods>;
}

#
# ██      ███████  ██████   █████   ██████ ██    ██      ██████  ██████  ███    ██ ███████ ██  ██████
# ██      ██      ██       ██   ██ ██       ██  ██      ██      ██    ██ ████   ██ ██      ██ ██
# ██      █████   ██   ███ ███████ ██        ████       ██      ██    ██ ██ ██  ██ █████   ██ ██   ███
# ██      ██      ██    ██ ██   ██ ██         ██        ██      ██    ██ ██  ██ ██ ██      ██ ██    ██
# ███████ ███████  ██████  ██   ██  ██████    ██         ██████  ██████  ██   ████ ██      ██  ██████
#
# Configuration under this block is a legacy config that needs to be properly tested before removing.
# If work on any of the entries please move it to above and add an appropriate comment on what it does and why do we keep it.

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

# Without this the playback notification doesn't show up on fresh launch
# https://github.com/shiftyjelly/pocketcasts-android/issues/1656
-keep class au.com.shiftyjelly.pocketcasts.core.player.** { *; }
