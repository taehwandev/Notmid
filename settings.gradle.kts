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
    ":core:auth:impl",
    ":core:data",
    ":core:designsystem",
    ":core:domain",
    ":core:model",
    ":core:feedback:api",
    ":core:network:api",
    ":core:network:assertions",
    ":core:network:impl",
    ":core:app",
    ":core:router:api",
    ":core:router:assertions",
    ":core:router:impl",
    ":feature:capture:api",
    ":feature:capture:impl",
    ":feature:feed:api",
    ":feature:feed:impl",
    ":feature:inbox:api",
    ":feature:inbox:impl",
    ":feature:map:api",
    ":feature:map:impl",
    ":feature:notmid:api",
    ":feature:notmid:common",
    ":feature:notmid:impl",
    ":feature:profile:api",
    ":feature:profile:impl",
    ":feature:webview:api",
    ":feature:webview:impl",
)
