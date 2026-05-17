plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.map"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":feature:map:api"))
    implementation(project(":feature:notmid:common"))
}
