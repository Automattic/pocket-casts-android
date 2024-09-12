import com.android.build.api.dsl.Lint
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.automattic.android.measure.reporters.InternalA8cCiReporter
import com.automattic.android.measure.reporters.SlowSlowTasksMetricsReporter
import com.google.devtools.ksp.gradle.KspExtension
import com.google.devtools.ksp.gradle.KspGradleSubplugin
import io.sentry.android.gradle.extensions.InstrumentationFeature
import io.sentry.android.gradle.extensions.SentryPluginExtension
import java.util.EnumSet
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    // Gradle Plugins
    dependencies {
        // Open source licenses plugin
        classpath(libs.osslicenses.plugin)
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
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dependency.analysis)
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

dependencyAnalysis {
    structure {
        ignoreKtx(true)
    }

    issues {
        all {
            onUsedTransitiveDependencies {
                severity("ignore")
            }

            onRuntimeOnly {
                severity("warn")
                exclude("org.jetbrains.kotlinx:kotlinx-coroutines-android")
            }

            onUnusedDependencies {
                // Mockito Android is incorrectly reported
                // We need it to run instrumentation tests that mock Android classes
                exclude("org.mockito:mockito-android")
            }
        }

        project(":app") {
            onIncorrectConfiguration {
                severity("warn")
                exclude("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }

        project(":wear") {
            onIncorrectConfiguration {
                severity("warn")
                exclude("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }

        project(":automotive") {
            onIncorrectConfiguration {
                severity("warn")
                exclude("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }
    }
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

val javaTarget = JvmTarget.fromTarget(libs.versions.java.get())

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.name.startsWith("kotlin-stdlib")) {
                useVersion(libs.versions.kotlin.asProvider().get())
            }
        }
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.dependency.analysis.get().pluginId)

    plugins.withType<KotlinBasePlugin>().configureEach {
        tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>>().configureEach {
            compilerOptions {
                jvmTarget.set(javaTarget)
                allWarningsAsErrors.set(true)
                optIn.addAll("kotlin.RequiresOptIn")
            }
        }
    }

    plugins.withType<KspGradleSubplugin>().configureEach {
        configure<KspExtension> {
            arg("skipPrivatePreviews", "true")
        }
    }

    plugins.withId(rootProject.libs.plugins.sentry.get().pluginId) {
        configureSentry()
    }

    configurations.configureEach {
        // Exclude the NDK from the Sentry Android SDK as we don't use it.
        exclude("io.sentry", "sentry-android-ndk")

        // https://github.com/android/android-test/issues/999
        if (name == "androidTestImplementation") {
            exclude("com.google.protobuf", "protobuf-lite")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = javaTarget.target
        targetCompatibility = javaTarget.target
    }

    val configureLint: Lint.() -> Unit = {
        baseline = project.file("lint-baseline.xml")
        lintConfig = rootProject.file("lint.xml")
        xmlReport = true
        sarifReport = System.getenv()["CI"].toBoolean()
        abortOnError = true

        checkAllWarnings = false
        warningsAsErrors = false

        checkDependencies = true
        checkTestSources = false
        checkGeneratedSources = false

        // Uncomment this when regenerating baseline files
        // ignoreWarnings = true

        // There's no point in slowing down assembling of release builds
        // since we execute lint explicitly on CI in a separate action
        checkReleaseBuilds = false
    }

    val SERVER_MAIN_URL_PROD = "\"https://refresh.pocketcasts.com\""
    val SERVER_API_URL_PROD = "\"https://api.pocketcasts.com\""
    val SERVER_CACHE_URL_PROD = "\"https://cache.pocketcasts.com\""
    val SERVER_CACHE_HOST_PROD = "\"cache.pocketcasts.com\""
    val SERVER_STATIC_URL_PROD = "\"https://static.pocketcasts.com\""
    val SERVER_SHARING_URL_PROD = "\"https://sharing.pocketcasts.com\""
    val SERVER_LIST_URL_PROD = "\"https://lists.pocketcasts.com\""
    val SERVER_LIST_HOST_PROD = "\"lists.pocketcasts.com\""
    val SERVER_SHORT_URL_PROD = "\"https://pca.st\""
    val SERVER_SHORT_HOST_PROD = "\"pca.st\""
    val WEB_BASE_HOST_PROD = "\"pocketcasts.com\""

    plugins.withType<BasePlugin>().configureEach {
        configure<BaseExtension> {
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
                sourceCompatibility = JavaVersion.toVersion(javaTarget.target)
                targetCompatibility = JavaVersion.toVersion(javaTarget.target)
            }

            setCompileSdkVersion(project.property("compileSdkVersion") as Int)

            defaultConfig {
                minSdk = project.property("minSdkVersion") as Int
                targetSdk = project.property("targetSdkVersion") as Int
                versionCode = project.property("versionCode") as Int
                versionName = project.property("versionName") as String

                buildConfigField("int", "VERSION_CODE", "${project.property("versionCode")}")
                buildConfigField("String", "VERSION_NAME", "\"${project.property("versionName")}\"")
                buildConfigField("String", "SETTINGS_ENCRYPT_SECRET", "\"${project.property("settingsEncryptSecret")}\"")
                buildConfigField("String", "SHARING_SERVER_SECRET", "\"${project.property("sharingServerSecret")}\"")
                buildConfigField("String", "GOOGLE_SIGN_IN_SERVER_CLIENT_ID", "\"${project.property("googleSignInServerClientId")}\"")
                buildConfigField("String", "SENTRY_DSN", "\"${project.property("pocketcastsSentryDsn")}\"")
                buildConfigField("String", "BUILD_PLATFORM", "\"${project.property("buildPlatform")}\"")
                buildConfigField("String", "ENCRYPTION_KEY", "\"${project.property("encryptionKey")}\"")
                buildConfigField("String", "APP_SECRET", "\"${project.property("appSecret")}\"")
                buildConfigField("String", "META_APP_ID", "\"${project.property("metaAppId")}\"")

                testInstrumentationRunner = project.property("testInstrumentationRunner") as String
                testApplicationId = "au.com.shiftyjelly.pocketcasts.test${project.name.replace("-", "_")}"
                vectorDrawables.useSupportLibrary = true
            }

            composeOptions {
                kotlinCompilerExtensionVersion = libs.versions.compose.kotlin.compiler.get()
            }

            testOptions {
                animationsDisabled = true
            }

            packagingOptions.resources.excludes += listOf(
                "META-INF/rxjava.properties",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/licenses/ASM",
                // Fixes issue running './gradlew connectedDebugAndroidTest' with clashing testing libraries.
                "**/attach_hotspot_windows.dll",
            )

            val canSignRelease = project.property("canSignRelease") == true

            signingConfigs {
                maybeCreate("debug").apply {
                    storeFile = rootProject.file("debug.keystore")
                    storePassword = "debugkey"
                    keyAlias = "debugkey"
                    keyPassword = "debugkey"
                }

                if (canSignRelease) {
                    maybeCreate("release").apply {
                        storeFile = project.property("storeFile") as File
                        storePassword = project.property("storePassword") as String
                        keyAlias = project.property("keyAlias") as String
                        keyPassword = project.property("keyPassword") as String
                    }
                }
            }

            buildTypes {
                named("debug") {
                    isPseudoLocalesEnabled = true
                    enableUnitTestCoverage = false
                    enableAndroidTestCoverage = false
                    ext.set("alwaysUpdateBuildId", false)

                    buildConfigField("String", "SERVER_MAIN_URL", "\"https://refresh.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_API_URL", "\"https://api.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_CACHE_URL", "\"https://podcast-api.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_CACHE_HOST", "\"podcast-api.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_STATIC_URL", "\"https://static.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_SHARING_URL", "\"https://sharing.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_SHORT_URL", "\"https://pcast.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_SHORT_HOST", "\"pcast.pocketcasts.net\"")
                    buildConfigField("String", "WEB_BASE_HOST", "\"pocket-casts-main-development.mystagingwebsite.com\"")
                    buildConfigField("String", "SERVER_LIST_URL", "\"https://lists.pocketcasts.net\"")
                    buildConfigField("String", "SERVER_LIST_HOST", "\"lists.pocketcasts.net\"")

                    signingConfig = signingConfigs.getByName("debug")
                }

                maybeCreate("debugProd").apply {
                    isDebuggable = true
                    isPseudoLocalesEnabled = true
                    enableUnitTestCoverage = false
                    enableAndroidTestCoverage = false
                    ext.set("alwaysUpdateBuildId", false)

                    buildConfigField("String", "SERVER_MAIN_URL", SERVER_MAIN_URL_PROD)
                    buildConfigField("String", "SERVER_API_URL", SERVER_API_URL_PROD)
                    buildConfigField("String", "SERVER_CACHE_URL", SERVER_CACHE_URL_PROD)
                    buildConfigField("String", "SERVER_CACHE_HOST", SERVER_CACHE_HOST_PROD)
                    buildConfigField("String", "SERVER_STATIC_URL", SERVER_STATIC_URL_PROD)
                    buildConfigField("String", "SERVER_SHARING_URL", SERVER_SHARING_URL_PROD)
                    buildConfigField("String", "SERVER_SHORT_URL", SERVER_SHORT_URL_PROD)
                    buildConfigField("String", "SERVER_SHORT_HOST", SERVER_SHORT_HOST_PROD)
                    buildConfigField("String", "WEB_BASE_HOST", WEB_BASE_HOST_PROD)
                    buildConfigField("String", "SERVER_LIST_URL", SERVER_LIST_URL_PROD)
                    buildConfigField("String", "SERVER_LIST_HOST", SERVER_LIST_HOST_PROD)

                    signingConfig = signingConfigs.getByName("debug")
                }

                named("release") {

                    buildConfigField("String", "SERVER_MAIN_URL", SERVER_MAIN_URL_PROD)
                    buildConfigField("String", "SERVER_API_URL", SERVER_API_URL_PROD)
                    buildConfigField("String", "SERVER_CACHE_URL", SERVER_CACHE_URL_PROD)
                    buildConfigField("String", "SERVER_CACHE_HOST", SERVER_CACHE_HOST_PROD)
                    buildConfigField("String", "SERVER_STATIC_URL", SERVER_STATIC_URL_PROD)
                    buildConfigField("String", "SERVER_SHARING_URL", SERVER_SHARING_URL_PROD)
                    buildConfigField("String", "SERVER_SHORT_URL", SERVER_SHORT_URL_PROD)
                    buildConfigField("String", "SERVER_SHORT_HOST", SERVER_SHORT_HOST_PROD)
                    buildConfigField("String", "WEB_BASE_HOST", WEB_BASE_HOST_PROD)
                    buildConfigField("String", "SERVER_LIST_URL", SERVER_LIST_URL_PROD)
                    buildConfigField("String", "SERVER_LIST_HOST", SERVER_LIST_HOST_PROD)

                    if (canSignRelease) {
                        signingConfig = signingConfigs.getByName("release")
                    }
                }
            }
        }

        dependencies {
            val coreLibraryDesugaring by configurations
            coreLibraryDesugaring(libs.desugar.jdk)
        }
    }

    plugins.withType<LibraryPlugin>().configureEach {
        configure<LibraryExtension> {
            buildTypes {
                named("release") {
                    isMinifyEnabled = false
                }
            }
        }
    }

    plugins.withType<AppPlugin>().configureEach {
        configure<BaseAppModuleExtension> {
            lint(configureLint)

            buildTypes {
                named("debug") {
                    applicationIdSuffix = ".debug"
                }

                maybeCreate("debugProd").apply {
                    applicationIdSuffix = ".debug"
                }

                named("release") {
                    isMinifyEnabled = true
                    proguardFiles.addAll(
                        listOf(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            rootProject.file("proguard-rules.pro"),
                        ),
                    )
                    isShrinkResources = true
                }
            }
        }

        // Set Gradle property 'au.com.shiftyjelly.pocketcasts.leakcanary=true' to enable Leak Canary.
        // You can do this using the local.properties file or any other avaialable Gradle mechanism.
        // See: https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
        if (properties["au.com.shiftyjelly.pocketcasts.leakcanary"]?.toString().toBoolean()) {
            dependencies {
                val implementation by configurations
                implementation(libs.leakcanary)
            }
        }
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
