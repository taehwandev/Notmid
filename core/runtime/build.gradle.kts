plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.core.runtime"
}

dependencies {
    api(project(":core:notice:api"))
    api(project(":core:router:api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:router:impl"))
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:router:assertions"))
    testImplementation(libs.junit)
}
