import java.util.Properties

val versionCodeDifferenceBetweenAppAndAutomotive = 50000
val versionCodeDifferenceBetweenAppAndWear = 50000 + versionCodeDifferenceBetweenAppAndAutomotive

val secretProperties = loadPropertiesFromFile(file("$rootDir/secret.properties"))
val versionProperties = loadPropertiesFromFile(file("${rootDir}/version.properties"))

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
    if (isAutomotiveBuild) {
        appVersionCode + versionCodeDifferenceBetweenAppAndAutomotive
    } else if (isWearBuild) {
        appVersionCode + versionCodeDifferenceBetweenAppAndWear
    } else {
        appVersionCode
    }
}
val getVersionName = {
    val versionName = versionProperties.getProperty("versionName", null)
    if (isAutomotiveBuild) {
        "${versionName}a"
    } else if (isWearBuild) {
        "${versionName}w"
    } else {
        versionName
    }
}

project.apply {
    extra.apply {
        // Application
        set("applicationId", "au.com.shiftyjelly.pocketcasts")

        set("versionName", getVersionName())
        set("versionCode", getVersionCode())

        // Android
        set("minSdkVersion", 23)
        set("minSdkVersionWear", 26)
        set("targetSdkVersion", 33)
        set("compileSdkVersion", 33)
        set("testInstrumentationRunner", "androidx.test.runner.AndroidJUnitRunner")

        // App Signing
        val storeFile = file("$rootDir/${secretProperties.getProperty("signingKeyStoreFile", null)}")
        if (storeFile.exists()) {
            // Use secret properties
            set("storePassword", secretProperties.getProperty("signingKeyStorePassword", null))
            set("keyAlias", secretProperties.getProperty("signingKeyAlias", null))
            set("keyPassword", secretProperties.getProperty("signingKeyPassword", null))
        } else {
            // Check local gradle properties
            val pocketcastsKeyStoreFile: String? by project
            if (!pocketcastsKeyStoreFile.isNullOrBlank()) {
                set("storeFile", file(pocketcastsKeyStoreFile))
                val pocketcastsKeyStorePassword: String by project
                set("storePassword", pocketcastsKeyStorePassword)
                val pocketcastsKeyStoreAlias: String by project
                set("keyAlias", pocketcastsKeyStoreAlias)
                val pocketcastsKeyStoreAliasPassword: String by project
                set("keyPassword", pocketcastsKeyStoreAliasPassword)
            }
        }
        set("canSignRelease", storeFile.exists())

        // Secrets
        set("settingsEncryptSecret", secretProperties.getProperty("pocketcastsSettingsEncryptSecret", ""))
        set("sharingServerSecret", secretProperties.getProperty("pocketcastsSharingServerSecret", ""))
        set("pocketcastsSentryDsn", secretProperties.getProperty("pocketcastsSentryDsn", ""))
        set("googleSignInServerClientId", secretProperties.getProperty("googleSignInServerClientId", ""))
    }
}
