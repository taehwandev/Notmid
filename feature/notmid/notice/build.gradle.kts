plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:notice:api"))
}
