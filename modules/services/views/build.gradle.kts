plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.views"
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
}

dependencies {
    api(libs.androidx.appcompat)
    api(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.coroutines.core)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.fragment.ktx)
    api(libs.dagger.hilt.android)
    api(libs.lottie)
    implementation(libs.okhttp)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    implementation(libs.androidx.cardview)
    implementation(libs.play.cast)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    api(libs.androidx.mediarouter)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    implementation(libs.timber)
    api(libs.navigation.runtime)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.androidx.viewpager)
    api(libs.play.review)
    implementation(platform(libs.compose.bom))
    api(libs.crashlogging)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)


    api(projects.modules.services.analytics)
    api(projects.modules.services.compose)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    api(projects.modules.services.utils)
}
