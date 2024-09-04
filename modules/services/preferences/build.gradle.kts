plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.preferences"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.coroutines.core)
    api(libs.hilt.android)
    api(libs.moshi)
    api(libs.rxjava)
    api(libs.rxrelay)
    implementation(libs.core.ktx)
    implementation(libs.preference.ktx)
    api(libs.work.runtime)
    implementation(libs.timber)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.cast)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:utils"))
}
