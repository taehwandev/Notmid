plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:network:api"))

    testImplementation(libs.junit)
}
