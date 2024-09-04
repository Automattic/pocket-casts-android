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
    androidTestImplementation(projects.modules.services.compose)

    // features
    implementation(projects.modules.features.account)
    implementation(projects.modules.features.discover)
    implementation(projects.modules.features.endofyear)
    implementation(projects.modules.features.filters)
    implementation(projects.modules.features.navigation)
    implementation(projects.modules.features.player)
    implementation(projects.modules.features.podcasts)
    implementation(projects.modules.features.profile)
    implementation(projects.modules.features.search)
    implementation(projects.modules.features.settings)
    implementation(projects.modules.features.shared)
    implementation(projects.modules.features.reimagine)
    implementation(projects.modules.features.taskerplugin)
    implementation(projects.modules.features.widgets)
    implementation(projects.modules.features.nova)

    // services
    implementation(projects.modules.services.analytics)
    implementation(projects.modules.services.crashlogging)
    implementation(projects.modules.services.deeplink)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.mediaFfmpeg)
    implementation(projects.modules.services.model)
    implementation(projects.modules.services.preferences)
    implementation(projects.modules.services.repositories)
    implementation(projects.modules.services.servers)
    implementation(projects.modules.services.sharing)
    implementation(projects.modules.services.utils)
    implementation(projects.modules.services.ui)
    implementation(projects.modules.services.views)
    testImplementation(projects.modules.services.sharedtest)
    androidTestImplementation(projects.modules.services.sharedtest)
}
