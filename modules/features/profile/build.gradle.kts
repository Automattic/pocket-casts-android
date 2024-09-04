plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.profile"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.appcompat)
    implementation(libs.browser.helper)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.fragment.ktx)
    api(libs.hilt.android)
    implementation(libs.media3.datasource)
    api(libs.media3.exoplayer)
    implementation(libs.media3.extractor)
    implementation(libs.lifecycle.reactivestreams.java)
    implementation(libs.coroutines.reactive)
    implementation(libs.rxjava)
    implementation(libs.rxrelay)
    implementation(libs.rxkotlin)
    api(libs.cardview)
    implementation(libs.cast)
    implementation(libs.coil)
    api(libs.constraintlayout)
    implementation(libs.core.ktx)
    api(libs.material)
    implementation(libs.preference)
    api(libs.recyclerview)
    api(libs.swiperefreshlayout)
    implementation(libs.timber)
    api(libs.crashlogging)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
    testImplementation("org.mockito:mockito-core:5.7.0")

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    // features
    api(project(":modules:features:account"))
    implementation(project(":modules:features:cartheme"))
    implementation(project(":modules:features:endofyear"))
    implementation(project(":modules:features:player"))
    api(project(":modules:features:podcasts"))
    api(project(":modules:features:settings"))
    implementation(project(":modules:features:kids"))

    // services
    api(project(":modules:services:analytics"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:deeplink"))
    implementation(project(":modules:services:images"))
    implementation(project(":modules:services:localization"))
    api(project(":modules:services:model"))
    api(project(":modules:services:preferences"))
    api(project(":modules:services:repositories"))
    api(project(":modules:services:servers"))
    api(project(":modules:services:ui"))
    implementation(project(":modules:services:utils"))
    api(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))
}
