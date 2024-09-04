plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.google.services)
}

apply(plugin = "com.google.android.gms.oss-licenses-plugin")

android {
    namespace = "au.com.shiftyjelly.pocketcasts"

    defaultConfig {
        applicationId = project.property("applicationId").toString()
        multiDexEnabled = true
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir(files("$rootDir/modules/services/model/schemas"))
        }
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
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_3"
        }

        named("release") {
            manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher"

            if (!file("${project.rootDir}/sentry.properties").exists()) {
                println("WARNING: Sentry configuration file 'sentry.properties' not found. The ProGuard mapping files won't be uploaded.")
            }
        }
    }
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation(libs.coroutines.rx2)
    implementation(libs.firebase.config.ktx)
    implementation(libs.dagger.hilt.android)
    implementation(libs.rx2.kotlin)
    implementation(libs.play.cast)
    implementation(libs.coil)
    implementation(libs.material)
    implementation(libs.lifecycle.reactivestreams.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.reactive)
    implementation(libs.crashlogging)
    implementation(libs.encryptedlogging)
    implementation(libs.fragment.ktx)
    implementation(libs.dagger.hilt.core)
    implementation(libs.hilt.work)
    implementation(libs.horologist.auth.data.phone)
    implementation(libs.horologist.datalayer)
    implementation(libs.media3.extractor)
    implementation(libs.androidx.mediarouter)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.play.wearable)
    implementation(libs.retrofit)
    implementation(libs.rx2.java)
    implementation(libs.rx2.relay)
    implementation(libs.timber)
    implementation(libs.work.runtime)
    implementation(libs.guava)
    implementation(platform(libs.compose.bom))
    implementation(platform(libs.firebase.bom))

    debugImplementation(libs.compose.ui.tooling)
    debugProdImplementation(libs.compose.ui.tooling)

    testImplementation(libs.mockito.core)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.turbine)

    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    androidTestImplementation(libs.compose.activity)
    androidTestImplementation(libs.navigation.compose)
    androidTestImplementation(libs.retrofit.moshi)
    androidTestImplementation(libs.androidx.preference.ktx)
    androidTestImplementation(libs.androidx.recyclerview)
    androidTestImplementation(libs.navigation.runtime)
    androidTestImplementation(libs.room)

    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.barista)
    androidTestImplementation(libs.compose.ui.test.junit)
    androidTestImplementation(libs.jsonassert)
    androidTestImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.navigation.testing)
    androidTestImplementation(libs.okHttp.mockwebserver)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.work.test)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit.ext)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(project(":modules:services:compose"))

    // features
    implementation(project(":modules:features:account"))
    implementation(project(":modules:features:discover"))
    implementation(project(":modules:features:endofyear"))
    implementation(project(":modules:features:filters"))
    implementation(project(":modules:features:navigation"))
    implementation(project(":modules:features:player"))
    implementation(project(":modules:features:podcasts"))
    implementation(project(":modules:features:profile"))
    implementation(project(":modules:features:search"))
    implementation(project(":modules:features:settings"))
    implementation(project(":modules:features:shared"))
    implementation(project(":modules:features:reimagine"))
    implementation(project(":modules:features:taskerplugin"))
    implementation(project(":modules:features:widgets"))
    implementation(project(":modules:features:nova"))

    // services
    implementation(project(":modules:services:analytics"))
    implementation(project(":modules:services:crashlogging"))
    implementation(project(":modules:services:deeplink"))
    implementation(project(":modules:services:localization"))
    implementation(project(":modules:services:media-ffmpeg"))
    implementation(project(":modules:services:model"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:repositories"))
    implementation(project(":modules:services:servers"))
    implementation(project(":modules:services:sharing"))
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:ui"))
    implementation(project(":modules:services:views"))
    testImplementation(project(":modules:services:sharedtest"))
    androidTestImplementation(project(":modules:services:sharedtest"))
}
