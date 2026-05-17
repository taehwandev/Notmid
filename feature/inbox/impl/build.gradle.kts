plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.inbox"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":feature:inbox:api"))
    implementation(project(":feature:notmid:common"))
}
