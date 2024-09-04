plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.filters"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = false
    }
}

dependencies {
    api(libs.androidx.appcompat)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    api(libs.dagger.hilt.android)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    implementation(libs.rx2.kotlin)
    api(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.flexbox)
    api(libs.material)
    implementation(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    implementation(libs.timber)
    api(libs.androidx.viewpager)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    // features
    api(projects.modules.features.podcasts)

    // services
    api(projects.modules.services.analytics)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    implementation(projects.modules.services.utils)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)
}
