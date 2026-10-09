plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
    maven("https://repo.repsy.io/mvn/buijs-dev/maven") {
        mavenContent {
            snapshotsOnly()
            includeGroupByRegex("dev\\.buijs.*")
        }
    }
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.kotlin.plugin.jvm)
    implementation(libs.kover.plugin)
    implementation(libs.dokka.plugin)
}
