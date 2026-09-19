plugins {
    id("glassnavlab.kotlin.library")
}

sourceSets.test {
    resources.srcDir(rootProject.file("docs/contracts"))
    resources.include("notmid-openapi.json")
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network:api"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(project(":core:network:assertions"))
    testImplementation(libs.junit)
}
