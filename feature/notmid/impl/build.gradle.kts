plugins {
    id("glassnavlab.android.library.compose")
}

android {
    namespace = "app.thdev.glassnavlab.feature.notmid"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":feature:capture:api"))
    implementation(project(":feature:capture:impl"))
    implementation(project(":feature:feed:api"))
    implementation(project(":feature:feed:impl"))
    implementation(project(":feature:inbox:api"))
    implementation(project(":feature:inbox:impl"))
    implementation(project(":feature:map:api"))
    implementation(project(":feature:map:impl"))
    implementation(project(":feature:notmid:api"))
    implementation(project(":feature:notmid:common"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:profile:impl"))
}
