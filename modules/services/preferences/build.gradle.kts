import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.preferences"
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val localPropertiesFile = rootProject.file("local.properties")
        var dataCollection: Boolean? = null
        var sendCrashReports: Boolean? = null
        var doneInitialOnboarding: Boolean? = null
        var autoDownloadOnFollow: Boolean? = null
        if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
            val properties = Properties().apply {
                load(localPropertiesFile.inputStream())
            }
            dataCollection = properties.getProperty("au.com.shiftyjelly.pocketcasts.data.collection")?.toBooleanStrictOrNull()
            sendCrashReports = properties.getProperty("au.com.shiftyjelly.pocketcasts.send.crash.reports")?.toBooleanStrictOrNull()
            doneInitialOnboarding = properties.getProperty("au.com.shiftyjelly.pocketcasts.done.initial.onboarding")?.toBooleanStrictOrNull()
            autoDownloadOnFollow = properties.getProperty("au.com.shiftyjelly.pocketcasts.auto.download.on.follow")?.toBooleanStrictOrNull()
        }
        buildConfigField("Boolean", "DATA_COLLECTION_DEFAULT_VALUE", dataCollection.toString())
        buildConfigField("Boolean", "SEND_CRASH_REPORTS_DEFAULT_VALUE", sendCrashReports.toString())
        buildConfigField("Boolean", "DONE_INITIAL_ONBOARDING_DEFAULT_VALUE", doneInitialOnboarding.toString())
        buildConfigField("Boolean", "AUTO_DOWNLOAD_ON_FOLLOW_DEFAULT_VALUE", autoDownloadOnFollow.toString())

    }
}

dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)

    api(libs.dagger.hilt.android)
    api(libs.moshi)
    api(libs.rx2.java)
    api(libs.rx2.relay)
    api(libs.work.runtime)

    api(projects.modules.services.model)
    api(projects.modules.services.utils)

    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.firebase.config)
    implementation(libs.play.cast)
    implementation(libs.timber)

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
}
