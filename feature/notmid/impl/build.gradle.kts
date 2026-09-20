plugins {
    id("glassnavlab.android.library.compose")
    id("glassnavlab.android.hilt")
}

android {
    namespace = "app.thdev.glassnavlab.feature.notmid"
}

dependencies {
    implementation(project(":core:runtime"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(project(":feature:capture:api"))
    implementation(project(":feature:capture:ui"))
    implementation(project(":feature:feed:api"))
    implementation(project(":feature:feed:ui"))
    implementation(project(":feature:inbox:api"))
    implementation(project(":feature:inbox:ui"))
    implementation(project(":feature:map:api"))
    implementation(project(":feature:map:ui"))
    implementation(project(":core:navigation:api"))
    implementation(project(":feature:notmid:common"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:profile:ui"))
    implementation(project(":feature:webview:api"))

    testImplementation(project(":core:navigation:assertions"))
    testImplementation(libs.junit)
}
