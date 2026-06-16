package app.thdev.glassnavlab.gradle

import app.thdev.glassnavlab.gradle.extensions.findLibrary
import app.thdev.glassnavlab.gradle.extensions.findVersion
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

internal fun Project.configureAndroid() {
    extensions.configure<CommonExtension> {
        compileSdk = findVersion("compileSdk").toInt()
        compileSdkMinor = findVersion("compileSdkMinor").toInt()

        defaultConfig.minSdk = findVersion("minSdk").toInt()
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        compileOptions.sourceCompatibility = JavaVersion.VERSION_11
        compileOptions.targetCompatibility = JavaVersion.VERSION_11
    }

    configureKotlinAndroid()
}

internal fun Project.configureAndroidApplication() {
    configureAndroid()

    extensions.configure<ApplicationExtension> {
        defaultConfig.targetSdk = findVersion("targetSdk").toInt()
    }
}

internal fun Project.configureComposeAndroid() {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

    extensions.configure<CommonExtension> {
        buildFeatures.compose = true
    }

    dependencies {
        "implementation"(platform(findLibrary("androidx-compose-bom")))
        "implementation"(findLibrary("androidx-compose-foundation"))
        "implementation"(findLibrary("androidx-compose-material3"))
        "implementation"(findLibrary("androidx-compose-runtime"))
        "implementation"(findLibrary("androidx-compose-ui"))
        "implementation"(findLibrary("androidx-compose-ui-graphics"))
        "implementation"(findLibrary("androidx-compose-ui-tooling-preview"))
        "debugImplementation"(findLibrary("androidx-compose-ui-tooling"))
    }
}

internal fun Project.configureKotlinAndroid() {
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}
