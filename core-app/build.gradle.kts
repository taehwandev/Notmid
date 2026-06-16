plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.coreapp"
}

dependencies {
    api(project(":core:router:api"))
    implementation(project(":core:router:impl"))

    testImplementation(project(":core:router:assertions"))
    testImplementation(libs.junit)
}
