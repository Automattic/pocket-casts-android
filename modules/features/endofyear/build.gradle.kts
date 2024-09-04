plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.endofyear"
    buildFeatures {
        buildConfig = true
        viewBinding = false
        compose = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.compose.activity)
    implementation(libs.compose.material)
    implementation(libs.compose.constraint)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    api(libs.hilt.android)
    implementation(libs.core.ktx)
    api(libs.showkase)
    implementation(libs.timber)
    api(libs.crashlogging)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit) { exclude(group = "org.hamcrest") }
    testImplementation(libs.mockito.kotlin)
    testImplementation("org.mockito:mockito-core:5.7.0")

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.showkase.processor)

    // features
    implementation(project(":modules:features:settings"))
    // services
    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:ui"))
    api(project(":modules:services:utils"))
    api(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))
}
