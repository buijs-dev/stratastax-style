repositories {
    google()
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
