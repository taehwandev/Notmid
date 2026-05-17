plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.core.designsystem"
}

dependencies {
    api(libs.kyant.backdrop)
}
