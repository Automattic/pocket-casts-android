plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.voicecontrol"
    buildFeatures {
        buildConfig = true
        prefab = true
        compose = true
    }
    ndkVersion = "27.0.12077973"
    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_LD_FLAGS=-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384",
                )
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.dagger.hilt.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    // onnxruntime .so provided by sherpa-onnx JNI libs in jniLibs/; headers are bundled in src/main/cpp/
    implementation(libs.timber)

    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.coroutines)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.preferences)
    implementation(projects.modules.services.repositories)
    implementation(projects.modules.services.utils)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.activity)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(projects.modules.services.sharedtest)
}

// Ensure Gradle tracks native source file changes for build-cache invalidation.
// android_gradle_build.json only tracks CMakeLists.txt, so .cpp/.h changes would
// otherwise let the build cache (org.gradle.caching=true) serve stale .so outputs.
tasks.configureEach {
    if (name.matches(Regex("buildCMake(RelWithDebInfo|Release|Debug)\\[.+]"))) {
        inputs.dir(layout.projectDirectory.dir("src/main/cpp"))
            .withPropertyName("nativeSources")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
