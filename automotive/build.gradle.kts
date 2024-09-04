plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.aboutlibraries)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts"

    defaultConfig {
        minSdk = project.property("minSdkVersionAutomotive") as Int
        applicationId = project.property("applicationId").toString()
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    buildTypes {
        named("debug") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_radioactive"
        }

        named("debugProd") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_radioactive"
        }

        named("release") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"
        }
    }
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.rx2)
    implementation(libs.dagger.hilt.android)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    androidTestImplementation(libs.junit)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.mockito.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose)
    androidTestImplementation(libs.androidx.test.junit.ext)

    implementation(libs.androidx.appcompat)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.crashlogging)
    implementation(libs.encryptedlogging)
    implementation(libs.guava)
    implementation(libs.dagger.hilt.core)
    implementation(libs.hilt.work)
    implementation(libs.material)
    implementation(libs.material.progressbar)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.extractor)
    implementation(libs.media3.ui)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.retrofit)
    implementation(libs.rx2.java)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.timber)
    implementation(libs.androidx.viewpager)
    implementation(libs.work.runtime)
    implementation(project(":modules:features:player"))
    implementation(project(":modules:features:search"))
    implementation(project(":modules:services:sharing"))

    implementation(project(":modules:services:crashlogging"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:media-noop"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:views"))
    implementation(project(":modules:services:compose"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:features:profile"))
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:settings"))
    implementation(project(":modules:features:discover"))
    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:shared"))
}

apply(plugin = "com.google.gms.google-services")
