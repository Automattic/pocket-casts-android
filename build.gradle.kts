import com.automattic.android.measure.reporters.InternalA8cCiReporter
import com.automattic.android.measure.reporters.SlowSlowTasksMetricsReporter
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
    enable = project.extra.properties.get("measureBuildsEnabled")?.toString().toBoolean()
    onBuildMetricsReadyListener {
        val report = this@onBuildMetricsReadyListener
        SlowSlowTasksMetricsReporter.report(report)
        InternalA8cCiReporter.reportBlocking(
            metricsReport = report,
            projectName = "pocketcasts",
            authToken = project.extra.get("appsMetricsToken").toString(),
        )
    }
    attachGradleScanId = false
}

val ktlintVersion = libs.versions.ktlint.get()

spotless {
    kotlin {
        target(
            "app/src/**/*.kt",
            "automotive/src/**/*.kt",
            "modules/**/src/**/*.kt",
            "wear/src/**/*.kt",
        )
        ktlint(ktlintVersion)
    }

    kotlinGradle {
        target("*.kts")
        ktlint(ktlintVersion)
    }
}

fun Project.configureSentry() {
    extensions.getByType(SentryPluginExtension::class.java).apply {
        val shouldUploadDebugFiles = System.getenv()["CI"].toBoolean() &&
            !project.properties["skipSentryProguardMappingUpload"]?.toString().toBoolean()
        includeProguardMapping = shouldUploadDebugFiles
        includeSourceContext = shouldUploadDebugFiles

        tracingInstrumentation {
            features.set(EnumSet.allOf(InstrumentationFeature::class.java) - InstrumentationFeature.OKHTTP)
        }
        autoInstallation.enabled = false
        includeDependenciesReport = false
        ignoredBuildTypes = setOf("debug", "debugProd")
    }
}

rootProject.subprojects {
    plugins.withId(rootProject.libs.plugins.sentry.get().pluginId) {
        configureSentry()
    }
}
