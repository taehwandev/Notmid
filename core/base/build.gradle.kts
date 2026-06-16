plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.core.base"
}

dependencies {
    api(libs.androidx.activity.compose)
    api(libs.kotlinx.coroutines.core)
    api(project(":core:runtime"))
    api(project(":core:notice:api"))
}
