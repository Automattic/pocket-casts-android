plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.localization"
    buildFeatures {
        buildConfig = true
    }

    lint {
        // We do not run Lint on localization project as it doesn't fit our release pipeline.
        // Running it there risks potential mistakes when making releases and requiring
        // developers to manually update the rules before submitting a release.
        lintConfig = rootProject.file("lint-no-op.xml")
    }
}

dependencies {
    compileOnly(libs.androidx.annotation)
}
