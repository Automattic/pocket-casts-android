import com.automattic.android.measure.MeasureBuildsExtension
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.extensions.SentryPluginExtension
import java.util.EnumSet

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    // Gradle Plugins
    dependencies {
        // Open source licenses plugin
        classpath(libs.ossLicenses.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.sentry) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.measure.builds)
    alias(libs.plugins.protobuf) apply false
}

apply(from = rootProject.file("dependencies.gradle.kts"))
apply(from = "scripts/git-hooks/install.gradle")

measureBuilds {
    enable = project.extra.get("measureBuildsEnabled")?.toString().toBoolean()
    automatticProject = MeasureBuildsExtension.AutomatticProject.PocketCasts
    authToken = project.extra.get("appsMetricsToken")?.toString()
    attachGradleScanId = false
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("$buildDir/**/*.kt")
        targetExclude("bin/**/*.kt")
        ktlint("0.50.0")
    }
}

fun Project.configureSentry() {
    extensions.getByType(SentryPluginExtension::class.java).apply {
        includeProguardMapping = System.getenv()["CI"].toBoolean()
                && !project.properties["skipSentryProguardMappingUpload"]?.toString().toBoolean()

        tracingInstrumentation {
            features.set(EnumSet.allOf(InstrumentationFeature::class.java) - InstrumentationFeature.FILE_IO)
        }
    }
}

rootProject.subprojects {
    plugins.withId(rootProject.libs.plugins.sentry.get().pluginId) {
        configureSentry()
    }
}
