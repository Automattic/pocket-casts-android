import java.util.Properties

val versionCodeDifferenceBetweenAppAndAutomotive = 50000
val versionCodeDifferenceBetweenAppAndWear = 50000 + versionCodeDifferenceBetweenAppAndAutomotive

val secretProperties = loadPropertiesFromFile(file("secret.properties"))
val versionProperties = loadPropertiesFromFile(file("version.properties"))

fun loadPropertiesFromFile(file: File): Properties {
    val properties = Properties()
    if (file.exists()) {
        file.inputStream().use { stream ->
            properties.load(stream)
        }
    }
    return properties
}

// Unfortunately we currently need to rely on a property to separate the version code between app & automotive builds.
// This is mainly because the library modules use the version code information from BuildConfig directly instead
// of receiving from the clients.
//
// On the bright side, the difference between the app & automotive modules is a static number, so even if there is
// an issue and the wrong version code information is used, it'd be easy to identify it.
//
// The long term solution is to move the version information out of the `base.gradle` and into the specific
// app & automotive modules and pass this information to the modules that require it.
val isAutomotiveBuild = (project.findProperty("IS_AUTOMOTIVE_BUILD") as? String)?.toBoolean() ?: false
val isWearBuild = (project.findProperty("IS_WEAR_BUILD") as? String)?.toBoolean() ?: false
val getVersionCode = {
    val appVersionCode = versionProperties.getProperty("versionCode", null).toInt()
    when {
        isAutomotiveBuild -> appVersionCode + versionCodeDifferenceBetweenAppAndAutomotive
        isWearBuild -> appVersionCode + versionCodeDifferenceBetweenAppAndWear
        else -> appVersionCode
    }
}
val getVersionName = {
    val versionName = versionProperties.getProperty("versionName", null)
    when {
        isAutomotiveBuild -> "${versionName}a"
        isWearBuild -> "${versionName}w"
        else -> versionName
    }
}
val getBuildPlatform = {
    when {
        isAutomotiveBuild -> "automotive"
        isWearBuild -> "wear"
        else -> "mobile"
    }
}

project.apply {
    extra.apply {
        // Application
        set("applicationId", "au.com.shiftyjelly.pocketcasts")

        set("versionName", getVersionName())
        set("versionCode", getVersionCode())
        set("buildPlatform", getBuildPlatform())

        // Android
        set("minSdkVersion", 24)
        set("minSdkVersionWear", 26)
        set("minSdkVersionAutomotive", 28)
        set("targetSdkVersion", 34)
        set("compileSdkVersion", 34)
        set("testInstrumentationRunner", "androidx.test.runner.AndroidJUnitRunner")

        // App Signing
        var canSignRelease = false
        // Check secret.properties for signing information
        val storeFilePath = secretProperties.getProperty("signingKeyStoreFile", null)
        if (!storeFilePath.isNullOrBlank()) {
            val storeFile = file(storeFilePath)
            canSignRelease = storeFile.exists()
            set("storeFile", storeFile)
            set("storePassword", secretProperties.getProperty("signingKeyStorePassword", null))
            set("keyAlias", secretProperties.getProperty("signingKeyAlias", null))
            set("keyPassword", secretProperties.getProperty("signingKeyPassword", null))
        }
        // Next check Gradle properties such as local.properties or ~/.gradle/gradle.properties
        if (!canSignRelease && project.hasProperty("signingKeyStoreFile")) {
            val signingKeyStoreFile = project.property("signingKeyStoreFile").toString()
            if (!signingKeyStoreFile.isNullOrBlank()) {
                val storeFile = file(signingKeyStoreFile)
                canSignRelease = storeFile.exists()
                set("storeFile", storeFile)
                set("storePassword", project.property("signingKeyStorePassword").toString())
                set("keyAlias", project.property("signingKeyAlias").toString())
                set("keyPassword", project.property("signingKeyPassword").toString())
            }
        }
        set("canSignRelease", canSignRelease)

        // Secrets
        set("settingsEncryptSecret", secretProperties.getProperty("pocketcastsSettingsEncryptSecret", ""))
        set("sharingServerSecret", secretProperties.getProperty("pocketcastsSharingServerSecret", ""))
        set("pocketcastsSentryDsn", secretProperties.getProperty("pocketcastsSentryDsn", ""))
        set("googleSignInServerClientId", secretProperties.getProperty("googleSignInServerClientId", ""))
        set("measureBuildsEnabled", secretProperties.getProperty("measureBuildsEnabled", ""))
        set("appsMetricsToken", secretProperties.getProperty("appsMetricsToken", ""))
        set("encryptionKey", secretProperties.getProperty("encryption_key", ""))
        set("appSecret", secretProperties.getProperty("app_secret", ""))
        set("metaAppId", secretProperties.getProperty("metaAppId", ""))
    }
}
