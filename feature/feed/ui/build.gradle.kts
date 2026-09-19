plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.feed"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":feature:feed:api"))
    implementation(project(":feature:notmid:common"))
}
