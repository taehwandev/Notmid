plugins {
    id("glassnavlab.android.library.compose")
    id("glassnavlab.android.hilt")
}

android {
    namespace = "app.thdev.glassnavlab.feature.profile"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:auth:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:navigation:api"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:notmid:common"))
    implementation(project(":feature:notmid:notice"))
}
