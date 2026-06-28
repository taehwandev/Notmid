import java.util.Properties

plugins {
    id("glassnavlab.android.application")
    id("glassnavlab.android.hilt")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun localConfigValue(
    name: String,
    defaultValue: String = "",
): String {
    return providers.gradleProperty(name).orNull
        ?: localProperties.getProperty(name)
        ?: providers.environmentVariable(name).orNull
        ?: defaultValue
}

fun String.asBuildConfigString(): String {
    return "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

val notmidVersionCode = localConfigValue(
    name = "NOTMID_VERSION_CODE",
    defaultValue = "1",
).toIntOrNull() ?: error("NOTMID_VERSION_CODE must be an integer.")
val notmidVersionName = localConfigValue(
    name = "NOTMID_VERSION_NAME",
    defaultValue = "1.0",
)
val notmidDebugApiBaseUrl = localConfigValue(
    name = "NOTMID_DEBUG_API_BASE_URL",
    defaultValue = localConfigValue(
        name = "NOTMID_API_BASE_URL",
        defaultValue = "http://10.0.2.2:8787",
    ),
)
val notmidReleaseApiBaseUrl = localConfigValue(
    name = "NOTMID_RELEASE_API_BASE_URL",
    defaultValue = "https://thdev.app",
)
val notmidDebugAuthMode = localConfigValue(
    name = "NOTMID_DEBUG_AUTH_MODE",
    defaultValue = localConfigValue(
        name = "NOTMID_AUTH_MODE",
        defaultValue = "fake",
    ),
)
val notmidReleaseAuthMode = localConfigValue(
    name = "NOTMID_RELEASE_AUTH_MODE",
    defaultValue = "disabled",
)
val notmidDebugFirebaseApiKey = localConfigValue(
    name = "NOTMID_DEBUG_FIREBASE_API_KEY",
    defaultValue = localConfigValue("NOTMID_FIREBASE_API_KEY"),
)
val notmidReleaseFirebaseApiKey = localConfigValue(
    name = "NOTMID_RELEASE_FIREBASE_API_KEY",
)
val notmidDebugFirebaseAuthRequestUri = localConfigValue(
    name = "NOTMID_DEBUG_FIREBASE_AUTH_REQUEST_URI",
    defaultValue = localConfigValue(
        name = "NOTMID_FIREBASE_AUTH_REQUEST_URI",
        defaultValue = "https://thdev.app/notmid/firebase-auth/android",
    ),
)
val notmidReleaseFirebaseAuthRequestUri = localConfigValue(
    name = "NOTMID_RELEASE_FIREBASE_AUTH_REQUEST_URI",
    defaultValue = "https://thdev.app/notmid/firebase-auth/android",
)
val notmidDebugGoogleServerClientId = localConfigValue(
    name = "NOTMID_DEBUG_GOOGLE_SERVER_CLIENT_ID",
    defaultValue = localConfigValue(
        name = "NOTMID_GOOGLE_SERVER_CLIENT_ID",
        defaultValue = localConfigValue("NOTMID_GOOGLE_CLIENT_ID"),
    ),
)
val notmidReleaseGoogleServerClientId = localConfigValue(
    name = "NOTMID_RELEASE_GOOGLE_SERVER_CLIENT_ID",
    defaultValue = localConfigValue("NOTMID_GOOGLE_SERVER_CLIENT_ID"),
)
val notmidDebugContentSource = localConfigValue(
    name = "NOTMID_DEBUG_CONTENT_SOURCE",
    defaultValue = localConfigValue(
        name = "NOTMID_CONTENT_SOURCE",
        defaultValue = "api",
    ),
)
val notmidReleaseContentSource = localConfigValue(
    name = "NOTMID_RELEASE_CONTENT_SOURCE",
    defaultValue = "api",
)
val notmidDebugMapProvider = localConfigValue(
    name = "NOTMID_DEBUG_MAP_PROVIDER",
    defaultValue = localConfigValue(
        name = "NOTMID_MAP_PROVIDER",
        defaultValue = "fake",
    ),
)
val notmidReleaseMapProvider = localConfigValue(
    name = "NOTMID_RELEASE_MAP_PROVIDER",
    defaultValue = "fake",
)
val notmidGoogleMapsApiKey = localConfigValue("NOTMID_GOOGLE_MAPS_API_KEY")
val notmidMapboxAccessToken = localConfigValue("NOTMID_MAPBOX_ACCESS_TOKEN")
val notmidReleaseStoreFile = localConfigValue("NOTMID_RELEASE_STORE_FILE")
val notmidReleaseStorePassword = localConfigValue("NOTMID_RELEASE_STORE_PASSWORD")
val notmidReleaseKeyAlias = localConfigValue("NOTMID_RELEASE_KEY_ALIAS")
val notmidReleaseKeyPassword = localConfigValue("NOTMID_RELEASE_KEY_PASSWORD")
val hasNotmidReleaseSigning = listOf(
    notmidReleaseStoreFile,
    notmidReleaseStorePassword,
    notmidReleaseKeyAlias,
    notmidReleaseKeyPassword,
).all { it.isNotBlank() }

android {
    namespace = "app.thdev.glassnavlab"

    defaultConfig {
        applicationId = "app.thdev.glassnavlab"
        versionCode = notmidVersionCode
        versionName = notmidVersionName

        buildConfigField(
            "String",
            "NOTMID_GOOGLE_MAPS_API_KEY",
            notmidGoogleMapsApiKey.asBuildConfigString(),
        )
        buildConfigField(
            "String",
            "NOTMID_MAPBOX_ACCESS_TOKEN",
            notmidMapboxAccessToken.asBuildConfigString(),
        )

        manifestPlaceholders["notmidGoogleMapsApiKey"] = notmidGoogleMapsApiKey
        manifestPlaceholders["notmidMapboxAccessToken"] = notmidMapboxAccessToken
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("notmidRelease") {
            if (hasNotmidReleaseSigning) {
                storeFile = rootProject.file(notmidReleaseStoreFile)
                storePassword = notmidReleaseStorePassword
                keyAlias = notmidReleaseKeyAlias
                keyPassword = notmidReleaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "NOTMID_API_BASE_URL",
                notmidDebugApiBaseUrl.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_AUTH_MODE",
                notmidDebugAuthMode.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_FIREBASE_API_KEY",
                notmidDebugFirebaseApiKey.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_FIREBASE_AUTH_REQUEST_URI",
                notmidDebugFirebaseAuthRequestUri.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_GOOGLE_SERVER_CLIENT_ID",
                notmidDebugGoogleServerClientId.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_CONTENT_SOURCE",
                notmidDebugContentSource.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_MAP_PROVIDER",
                notmidDebugMapProvider.asBuildConfigString(),
            )
            buildConfigField("String", "NOTMID_BUILD_CHANNEL", "debug".asBuildConfigString())
        }

        release {
            buildConfigField(
                "String",
                "NOTMID_API_BASE_URL",
                notmidReleaseApiBaseUrl.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_AUTH_MODE",
                notmidReleaseAuthMode.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_FIREBASE_API_KEY",
                notmidReleaseFirebaseApiKey.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_FIREBASE_AUTH_REQUEST_URI",
                notmidReleaseFirebaseAuthRequestUri.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_GOOGLE_SERVER_CLIENT_ID",
                notmidReleaseGoogleServerClientId.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_CONTENT_SOURCE",
                notmidReleaseContentSource.asBuildConfigString(),
            )
            buildConfigField(
                "String",
                "NOTMID_MAP_PROVIDER",
                notmidReleaseMapProvider.asBuildConfigString(),
            )
            buildConfigField("String", "NOTMID_BUILD_CHANNEL", "release".asBuildConfigString())
            if (hasNotmidReleaseSigning) {
                signingConfig = signingConfigs.getByName("notmidRelease")
            }
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    implementation(project(":core:auth:api"))
    implementation(project(":core:auth:impl"))
    implementation(project(":core:base"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:notice:api"))
    implementation(project(":core:model"))
    implementation(project(":core:network:api"))
    implementation(project(":core:network:impl"))
    implementation(project(":core:runtime"))
    implementation(project(":feature:notmid:api"))
    implementation(project(":feature:notmid:impl"))
    implementation(project(":feature:webview:impl"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
