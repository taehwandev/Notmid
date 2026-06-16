plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:network:api"))
    implementation(libs.okhttp)
    testImplementation(libs.junit)
}
