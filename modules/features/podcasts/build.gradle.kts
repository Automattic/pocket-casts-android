plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.podcasts"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.compose.animation)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.androidx.cardview)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    implementation(libs.lifecycle.reactivestreams.ktx)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    api(libs.androidx.swiperefreshlayout)
    implementation(libs.timber)
    api(libs.androidx.viewpager)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)

    // features
    implementation(projects.modules.features.account)
    implementation(projects.modules.features.search)
    implementation(projects.modules.features.reimagine)
    implementation(projects.modules.features.settings)
    api(projects.modules.features.player)

    // services
    api(projects.modules.services.analytics)
    api(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    api(projects.modules.services.model)
    api(projects.modules.services.localization)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.utils)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)
}
