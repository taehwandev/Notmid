plugins {
    id("glassnavlab.android.application")
}

android {
    namespace = "app.thdev.glassnavlab"

    defaultConfig {
        applicationId = "app.thdev.glassnavlab"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:domain"))
    implementation(project(":core:router:api"))
    implementation(project(":core:router:impl"))
    implementation(project(":feature:capture:api"))
    implementation(project(":feature:feed:api"))
    implementation(project(":feature:inbox:api"))
    implementation(project(":feature:map:api"))
    implementation(project(":feature:notmid:api"))
    implementation(project(":feature:notmid:impl"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:webview:api"))
    implementation(project(":feature:webview:impl"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
