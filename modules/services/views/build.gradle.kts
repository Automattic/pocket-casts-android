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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appcompat)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.mediarouter)
    api(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    api(libs.automattic.crashlogging)
    api(libs.compose.material)
    api(libs.dagger.hilt.android)
    api(libs.lottie)
    api(libs.material)
    api(libs.navigation.runtime)
    api(libs.play.review)
    api(libs.rx2.java)
    api(libs.rx2.relay)

    api(projects.modules.services.analytics)
    api(projects.modules.services.compose)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    api(projects.modules.services.utils)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.cardview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.viewpager)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coroutines.core)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.okhttp)
    implementation(libs.play.cast)
    implementation(libs.rx2.android)
    implementation(libs.rx2.kotlin)
    implementation(libs.timber)

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
