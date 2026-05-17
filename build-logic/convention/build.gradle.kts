import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "app.thdev.glassnavlab.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.plugin.android.gradle)
    compileOnly(libs.plugin.kotlin.gradle)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "glassnavlab.android.application"
            implementationClass = "app.thdev.glassnavlab.gradle.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "glassnavlab.android.library"
            implementationClass = "app.thdev.glassnavlab.gradle.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "glassnavlab.android.library.compose"
            implementationClass = "app.thdev.glassnavlab.gradle.AndroidLibraryComposeConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "glassnavlab.kotlin.library"
            implementationClass = "app.thdev.glassnavlab.gradle.KotlinLibraryConventionPlugin"
        }
    }
}
