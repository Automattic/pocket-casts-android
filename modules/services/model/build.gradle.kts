import java.security.MessageDigest
import java.io.File

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "au.com.shiftyjelly.pocketcasts.model"
    room {
        schemaDirectory("$projectDir/schemas")
    }
    buildFeatures {
        buildConfig = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    doFirst {
        fun computeMD5(file: File): String {
            if (file.exists() && file.isFile) {
                val md5Digest = MessageDigest.getInstance("MD5")
                file.inputStream().use { stream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        md5Digest.update(buffer, 0, bytesRead)
                    }
                }
                return md5Digest.digest().joinToString("") { "%02x".format(it) }
            }
            return "N/A"
        }

        val inputFile = project.file("build/${project.name}-task-inputs-${name}.txt")
        inputFile.writeText("Task: $name\nInputs:\n")

        // Save input files with relative paths
        inputs.files.sorted().forEach { file ->
            inputFile.appendText("Input File: ${project.relativePath(file)} (MD5: ${computeMD5(file)})\n")
        }

        // Save input properties with proper casting
        inputFile.appendText("\n=== Input Properties ===\n")
        inputs.properties.toSortedMap().forEach { (key, value) ->
            val valueString = when (value) {
                is org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions ->
                    value.arguments.toString()
                is org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions ->
                    """
                    jvmTarget: ${value.jvmTarget}
                    apiVersion: ${value.apiVersion}
                    freeCompilerArgs: ${value.freeCompilerArgs.get().joinToString(", ")}
                    allWarningsAsErrors: ${value.allWarningsAsErrors}
                    """.trimIndent()
                is org.jetbrains.kotlin.gradle.tasks.KotlinJavaToolchain ->
                    "Java Version: ${value.javaVersion}"
                null -> "null"
                else -> value.toString()
            }
            inputFile.appendText("$key = $valueString\n")
        }
    }
}


tasks.withType<JavaCompile>().configureEach {
    doFirst {
        val inputFile = project.file("build/${project.name}-task-inputs-${name}.txt")
        inputFile.writeText("Task: $name\n\n=== Input Files ===\n")

        // Function to compute MD5 hash of a file
        fun computeMD5(file: File): String {
            if (file.exists() && file.isFile) {
                val md5Digest = MessageDigest.getInstance("MD5")
                file.inputStream().use { stream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        md5Digest.update(buffer, 0, bytesRead)
                    }
                }
                return md5Digest.digest().joinToString("") { "%02x".format(it) }
            }
            return "N/A"
        }

        // Save all input files (including directories) with relative paths and MD5 hashes
        inputs.files.sorted().forEach { file ->
            val relativePath = project.relativePath(file)
            val md5Hash = computeMD5(file)
            inputFile.appendText("Input File: $relativePath (MD5: $md5Hash)\n")
        }

        // Save input properties
        inputFile.appendText("\n=== Input Properties ===\n")
        inputs.properties.forEach { (key, value) ->
            inputFile.appendText("$key: $value\n")
        }

        // Save task-specific properties
        inputFile.appendText("\n=== Task Properties ===\n")
        inputFile.appendText("Source Compatibility: $sourceCompatibility\n")
        inputFile.appendText("Target Compatibility: $targetCompatibility\n")

        // Log the options if available
        options.let { options ->
            options.compilerArgumentProviders.forEach { value ->
                when (value) {
                    is com.android.build.gradle.tasks.CommandLineArgumentProviderAdapter -> {
                        inputFile.appendText("Class names: ${value.classNames.get()}\n")
                        inputFile.appendText("Arguments: ${value.arguments.get()}\n")
                    }

                    is com.android.build.gradle.tasks.JdkImageInput -> {
                        inputFile.appendText(
                            "Generated module file: ${
                                project.relativePath(value.generatedModuleFile.get())
                            }, md5: ${computeMD5(value.generatedModuleFile.get())} \n"
                        )
                        inputFile.appendText(
                            "Jrt fs jar: ${
                                project.relativePath(value.jrtFsJar.get())
                            }, md5: ${computeMD5(value.jrtFsJar.get())} \n"
                        )
                    }
                }
            }
            options.debugOptions.let {
                inputFile.appendText("Debug level: ${it.debugLevel}\n")
            }
            inputFile.appendText("\n=== Compiler Options ===\n")
            inputFile.appendText("Compiler Args: ${options.compilerArgs.joinToString(", ")}\n")
            inputFile.appendText("Annotation Processor Path: ${options.annotationProcessorPath}\n")

            inputFile.appendText("\n=== Fork Options ===\n")
            inputFile.appendText("Fork: ${options.forkOptions.executable}\n")
            inputFile.appendText("Fork: ${options.forkOptions.javaHome}\n")
            inputFile.appendText("Fork: ${options.forkOptions.tempDir}\n")
        }

        inputFile.appendText("\n=== Metadata ===\n")

        val metadata = javaCompiler.get().metadata
        inputFile.appendText("Compiler: ${metadata.installationPath}\n")
        inputFile.appendText("Compiler: ${metadata.javaRuntimeVersion}\n")
        inputFile.appendText("Compiler: ${metadata.jvmVersion}\n")
        inputFile.appendText("Compiler: ${metadata.languageVersion}\n")
        inputFile.appendText("Compiler: ${metadata.vendor}\n")

        inputFile.appendText("\n=== Java compiler ===\n")
        inputFile.appendText("Compiler: ${javaCompiler.get().executablePath}\n")
        inputFile.appendText("Compiler: ${javaCompiler.get().metadata}\n")
    }
}


dependencies {
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.room.compiler)

    api(libs.billing.ktx)
    api(libs.media3.extractor)
    api(libs.moshi)
    api(libs.okhttp)
    api(libs.room)
    api(libs.rx2.java)

    api(projects.modules.services.utils)

    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.dagger.hilt.core)
    implementation(libs.play.auth)
    implementation(libs.play.cast)
    implementation(libs.room.ktx)
    implementation(libs.room.rx2)
    implementation(libs.timber)

    implementation(projects.modules.services.localization)

    debugImplementation(libs.compose.ui.tooling)

    debugProdImplementation(libs.compose.ui.tooling)

    compileOnly(libs.media3.common)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(projects.modules.services.sharedtest)

    androidTestImplementation(libs.androidx.annotation)
}
