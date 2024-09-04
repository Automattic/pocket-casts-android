plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.nova"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    implementation(libs.coroutines.core)
    api(libs.hilt.android)
    api(libs.hilt.work)
    implementation(libs.lifecycle.process)
    implementation(libs.work.runtime)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.coroutines.test)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    // AAR dependencies cannot be resolved with Version Catalogs https://github.com/gradle/gradle/issues/20074
    implementation("io.branch.engage:conduit-source:0.2.3-pocketcasts.9@aar") {
        isTransitive = true
    }

    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:deeplink"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:model"))
    api(project(":modules:services:repositories"))
    implementation(project(":modules:services:utils"))
}
