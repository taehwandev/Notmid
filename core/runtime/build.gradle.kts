plugins {
    id("glassnavlab.android.library.compose")
    id("glassnavlab.android.hilt")
}

android {
    namespace = "app.thdev.glassnavlab.core.runtime"
}

dependencies {
    api(project(":core:notice:api"))
    api(project(":core:navigation:api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation:impl"))
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:navigation:assertions"))
    testImplementation(libs.junit)
}
