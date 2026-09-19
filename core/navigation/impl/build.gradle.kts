plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:navigation:api"))

    testImplementation(project(":core:navigation:assertions"))
    testImplementation(libs.junit)
}
