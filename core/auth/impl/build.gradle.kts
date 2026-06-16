plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:auth:api"))
    implementation(project(":core:model"))
    implementation(project(":core:network:api"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:network:assertions"))
    testImplementation(libs.junit)
}
