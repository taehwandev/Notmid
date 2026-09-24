plugins {
    id("glassnavlab.android.library.compose")
    id("glassnavlab.android.hilt")
}

android {
    namespace = "app.thdev.glassnavlab.core.activity"
}

dependencies {
    api(libs.androidx.activity.compose)
    api(libs.kotlinx.coroutines.core)
    api(project(":core:navigation:api"))
    api(project(":core:notice:ui"))
}
