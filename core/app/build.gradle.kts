plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.coreapp"
}

dependencies {
    api(project(":core:feedback:api"))
    api(project(":core:router:api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:router:impl"))
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:router:assertions"))
    testImplementation(libs.junit)
}
