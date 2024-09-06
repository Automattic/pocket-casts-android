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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appcompat)
    api(libs.androidx.cardview)
    api(libs.androidx.recyclerview)
    api(libs.androidx.viewpager)
    api(libs.dagger.hilt.android)
    api(libs.flexbox)
    api(libs.material)
    api(libs.rx2.java)

    api(projects.modules.features.podcasts)
    api(projects.modules.services.analytics)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)

    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.rx2.android)
    implementation(libs.rx2.kotlin)
    implementation(libs.timber)

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)
}
