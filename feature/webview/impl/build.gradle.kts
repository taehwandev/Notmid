plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.webview"
}

dependencies {
    implementation(project(":core:runtime"))
    implementation(project(":feature:webview:api"))

    implementation(libs.androidx.activity.compose)
}
