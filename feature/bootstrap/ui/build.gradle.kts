plugins {
    id("glassnavlab.android.library.compose")
    id("glassnavlab.android.hilt")
}

android {
    namespace = "app.thdev.glassnavlab.feature.bootstrap"
}

dependencies {
    implementation(project(":core:data:api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":feature:notmid:common"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    testImplementation(project(":core:data:impl"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
