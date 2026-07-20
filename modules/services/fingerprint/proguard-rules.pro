# UniFFI generated bindings use JNA to dynamically map Kotlin interface methods
# to native function symbols via reflection. R8 cannot trace this usage and will
# remove these classes/methods as "unused" without explicit keep rules.

# Keep all UniFFI generated classes and interfaces
-keep class uniffi.fingerprint_uniffi.** { *; }
-keep interface uniffi.fingerprint_uniffi.** { *; }

# JNA core classes required for native library loading and FFI
-keep class com.sun.jna.** { *; }
-keep interface com.sun.jna.** { *; }
