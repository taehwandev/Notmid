plugins {
    id("glassnavlab.kotlin.library")
}

sourceSets.test {
    resources.srcDir(rootProject.file("docs/contracts"))
    resources.include("notmid-openapi.json")
}

dependencies {
    implementation(project(":core:data:api"))
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network:api"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":core:network:assertions"))
    testImplementation(project(":core:data:assertions"))
    testImplementation(libs.junit)
}
