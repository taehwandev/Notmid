plugins {
    id("glassnavlab.android.library")
}

android {
    namespace = "app.thdev.glassnavlab.feature.webview"
}

dependencies {
    implementation(project(":core:app"))
    implementation(project(":feature:webview:api"))
}
