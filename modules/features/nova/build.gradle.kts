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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.dagger.hilt.android)
    api(libs.hilt.work)

    api(projects.modules.services.repositories)

    implementation(libs.coroutines.core)
    implementation(libs.lifecycle.process)
    implementation(libs.work.runtime)

    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.model)
    implementation(projects.modules.services.utils)
    // AAR dependencies cannot be resolved with Version Catalogs https://github.com/gradle/gradle/issues/20074
    implementation("io.branch.engage:conduit-source:0.2.3-pocketcasts.9@aar") { isTransitive = true }

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.coroutines.test)
}
