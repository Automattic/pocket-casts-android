plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.discover"
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
    api(libs.dagger.hilt.android)
    api(libs.hilt.work)
    testImplementation(libs.moshi)
    testImplementation(libs.okhttp)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.moshi)
    implementation(libs.rx2.android)
    api(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.rx2.kotlin)
    api(libs.work.runtime)
    api(libs.androidx.cardview)
    implementation(libs.coil)
    api(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    api(libs.material)
    implementation(libs.material.dialogs)
    implementation(libs.material.progressbar)
    implementation(libs.androidx.preference.ktx)
    api(libs.androidx.recyclerview)
    implementation(libs.timber)
    api(libs.crashlogging)
    implementation(platform(libs.compose.bom))

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.okHttp.mockwebserver)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    // features
    implementation(projects.modules.features.podcasts)
    implementation(projects.modules.features.search)

    // services
    api(projects.modules.services.analytics)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.images)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    implementation(projects.modules.services.utils)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)
}
