plugins {
    id("glassnavlab.kotlin.library")
}

dependencies {
    api(project(":core:data:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
