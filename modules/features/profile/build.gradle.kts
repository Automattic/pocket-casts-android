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
    api(libs.androidx.appcompat)
    implementation(libs.browser.helper)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.media3.datasource)
    api(libs.media3.exoplayer)
    implementation(libs.media3.extractor)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.coroutines.reactive)
    implementation(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.androidx.cardview)
    implementation(libs.play.cast)
    implementation(libs.coil)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    implementation(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    api(libs.androidx.swiperefreshlayout)
    implementation(libs.timber)
    api(libs.crashlogging)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)
    testImplementation(libs.mockito.core)

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
