plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.crashlogging"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.dagger.hilt.android)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)

    api(libs.crashlogging)
    api(libs.encryptedlogging)

    api(project(":modules:services:utils"))
}
