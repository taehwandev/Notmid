plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.notmid.common"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
}
