plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.profile"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:notmid:common"))
}
