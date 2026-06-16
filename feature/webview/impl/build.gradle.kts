plugins {
    id("glassnavlab.android.library")
}

android {
    namespace = "app.thdev.glassnavlab.feature.webview"
}

dependencies {
    implementation(project(":core:runtime"))
    implementation(project(":feature:webview:api"))
}
