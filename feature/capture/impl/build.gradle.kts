plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.capture"
}

dependencies {
    implementation(project(":feature:capture:api"))
    implementation(project(":feature:notmid:common"))
}
