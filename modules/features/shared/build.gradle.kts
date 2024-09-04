plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.shared"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    implementation(libs.coroutines.play.services)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    api(libs.hilt.android)
    implementation(libs.lifecycle.process)
    implementation(libs.play.services.wearable)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.lifecycle.runtime.testing)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(project(":modules:services:analytics"))
    api(project(":modules:services:crashlogging"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:utils"))
    api(project(":modules:services:model"))
}
