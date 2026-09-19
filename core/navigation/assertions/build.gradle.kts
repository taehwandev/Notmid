plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:navigation:api"))

    testImplementation(libs.junit)
}
