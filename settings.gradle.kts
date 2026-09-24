pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "notmid"
include(
    ":app",
    ":core:auth:api",
    ":core:auth:android",
    ":core:auth:impl",
    ":core:data",
    ":core:base",
    ":core:designsystem",
    ":core:domain",
    ":core:model",
    ":core:notice:api",
    ":core:notice:ui",
    ":core:network:api",
    ":core:network:assertions",
    ":core:network:impl",
    ":core:runtime",
    ":core:navigation:api",
    ":core:navigation:assertions",
    ":core:navigation:impl",
    ":feature:auth:ui",
    ":feature:capture:api",
    ":feature:capture:ui",
    ":feature:feed:api",
    ":feature:feed:ui",
    ":feature:inbox:api",
    ":feature:inbox:ui",
    ":feature:map:api",
    ":feature:map:ui",
    ":feature:notmid:common",
    ":feature:notmid:notice",
    ":feature:profile:api",
    ":feature:profile:ui",
    ":feature:webview:api",
    ":feature:webview:impl",
)
