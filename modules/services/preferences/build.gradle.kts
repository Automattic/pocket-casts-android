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
        val dataCollectionValue = if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
            val properties = Properties().apply {
                load(localPropertiesFile.inputStream())
            }
            properties.getProperty("au.com.shiftyjelly.pocketcasts.data.collection")?.toBooleanStrictOrNull()
        } else {
            null
        }
        buildConfigField("Boolean", "DATA_COLLECTION_DEFAULT_VALUE", dataCollectionValue.toString())
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
