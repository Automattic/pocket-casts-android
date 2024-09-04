plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.engage"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    api(libs.hilt.android)
    api(libs.hilt.work)
    implementation(libs.coroutines.play.services)
    implementation(libs.lifecycle.process)
    implementation(libs.work.runtime)
    implementation(libs.timber)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit) { exclude(group = "org.hamcrest") }

    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:deeplink"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:model"))
    api(project(":modules:services:repositories"))
    implementation(project(":modules:services:utils"))

    implementation(libs.engage)
}
