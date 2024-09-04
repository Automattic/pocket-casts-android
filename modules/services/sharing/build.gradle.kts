plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.sharing"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = false
    }
}

dependencies {
    implementation(libs.coroutines.core)
    api(libs.hilt.android)
    implementation(libs.coil)
    implementation(libs.timber)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit) { exclude(group = "org.hamcrest") }

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(project(":modules:services:model"))
    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:repositories"))
    implementation(project(":modules:services:utils"))
}
