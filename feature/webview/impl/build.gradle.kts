plugins {
    id("glassnavlab.android.library.compose")
    id("glassnavlab.android.hilt")
}

android {
    namespace = "app.thdev.glassnavlab.feature.webview"
}

dependencies {
    implementation(project(":core:activity"))
    implementation(project(":feature:webview:api"))
    implementation(project(":feature:webview:ui"))

    implementation(libs.androidx.activity.compose)
}
