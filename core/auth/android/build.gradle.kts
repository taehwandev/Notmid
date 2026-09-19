plugins {
    id("glassnavlab.android.library")
}

android {
    namespace = "app.thdev.glassnavlab.core.auth.android"
}

dependencies {
    implementation(project(":core:auth:api"))
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
