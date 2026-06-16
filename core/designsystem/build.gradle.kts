plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.core.designsystem"
}

dependencies {
    api(libs.kyant.backdrop)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
