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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appcompat)
    api(libs.androidx.cardview)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.recyclerview)
    api(libs.androidx.swiperefreshlayout)
    api(libs.automattic.crashlogging)
    api(libs.dagger.hilt.android)
    api(libs.material)
    api(libs.media3.exoplayer)

    api(projects.modules.features.account)
    api(projects.modules.features.podcasts)
    api(projects.modules.features.settings)
    api(projects.modules.services.analytics)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)

    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.browser.helper)
    implementation(libs.coil)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactive)
    implementation(libs.coroutines.rx2)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.config.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.media3.datasource)
    implementation(libs.media3.extractor)
    implementation(libs.play.cast)
    implementation(libs.rx2.java)
    implementation(libs.rx2.kotlin)
    implementation(libs.rx2.relay)
    implementation(libs.timber)

    implementation(projects.modules.features.cartheme)
    implementation(projects.modules.features.endofyear)
    implementation(projects.modules.features.kids)
    implementation(projects.modules.features.player)
    implementation(projects.modules.features.referrals)
    implementation(projects.modules.services.compose)
    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)
}
