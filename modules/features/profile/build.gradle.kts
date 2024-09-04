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
    api(projects.modules.features.account)
    implementation(projects.modules.features.cartheme)
    implementation(projects.modules.features.endofyear)
    implementation(projects.modules.features.player)
    api(projects.modules.features.podcasts)
    api(projects.modules.features.settings)
    implementation(projects.modules.features.kids)

    // services
    api(projects.modules.services.analytics)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    implementation(projects.modules.services.utils)
    api(projects.modules.services.views)
    testImplementation(projects.modules.services.sharedtest)
}
