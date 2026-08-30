plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.ai.preview)
}

// Renders this module's `@Preview`s to PNG on CI. `sdkVersion` is pinned because auto-detect
// silently clamps compileSdk 37 to Robolectric's sdk=36 ceiling; `hostTheme` is required because
// `HtmlText` resolves `?attr/primary_text_01`, which only our own themes define and a library
// module's merged manifest does not supply — without it those previews render nothing.
composePreview {
    variant.set("debug")
    sdkVersion.set(35)
    hostTheme.set("@style/ThemeLight")
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.compose"
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    api(libs.compose.material3.adaptive)

    api(projects.modules.services.model)
    api(projects.modules.services.preferences)
    api(projects.modules.services.repositories)
    api(projects.modules.services.ui)

    implementation(platform(libs.compose.bom))

    implementation(libs.androidx.webkit)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.compose.animation.graphics)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugProdImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.util)
    implementation(libs.fragment.compose)
    implementation(libs.lottie)
    implementation(libs.lottie.compose)
    implementation(libs.navigation.compose)
    implementation(libs.reorderable)

    implementation(projects.modules.services.images)
    implementation(projects.modules.services.localization)
    implementation(projects.modules.services.utils)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
