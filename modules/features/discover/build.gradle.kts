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
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.androidx.appcompat)
    api(libs.androidx.cardview)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.recyclerview)
    api(libs.automattic.crashlogging)
    api(libs.dagger.hilt.android)
    api(libs.hilt.work)
    api(libs.material)
    api(libs.rx2.java)
    api(libs.work.runtime)

    api(projects.modules.services.analytics)
    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.servers)
    api(projects.modules.services.ui)
    api(projects.modules.services.views)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.coil)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.rx2)
    implementation(libs.fragment.ktx)
    implementation(libs.material.dialogs)
    implementation(libs.material.progressbar)
    implementation(libs.rx2.android)
    implementation(libs.rx2.kotlin)
    implementation(libs.rx2.relay)
    implementation(libs.timber)

    implementation(projects.modules.features.podcasts)
    implementation(projects.modules.features.search)
    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.moshi)
    testImplementation(libs.okHttp.mockwebserver)
    testImplementation(libs.okhttp)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.moshi)
}
