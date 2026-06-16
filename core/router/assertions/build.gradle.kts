plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:router:api"))

    testImplementation(libs.junit)
}
