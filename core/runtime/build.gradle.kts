plugins {
    id("glassnavlab.android.library.compose")
    id("glassnavlab.android.hilt")
}

android {
    namespace = "app.thdev.glassnavlab.core.runtime"
}

dependencies {
    api(project(":core:navigation:api"))
    implementation(project(":core:navigation:impl"))

    testImplementation(project(":core:navigation:assertions"))
    testImplementation(libs.junit)
}
