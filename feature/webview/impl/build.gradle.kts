plugins {
    id("glassnavlab.android.library")
}

android {
    namespace = "app.thdev.glassnavlab.feature.webview"
}

dependencies {
    implementation(project(":feature:webview:api"))
}
