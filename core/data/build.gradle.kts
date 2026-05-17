plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
}
