plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network:api"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
