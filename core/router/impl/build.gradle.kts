plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    implementation(project(":core:router:api"))

    testImplementation(project(":core:router:assertions"))
    testImplementation(libs.junit)
}
