pluginManagement {
    repositories {
        mavenLocal()
        maven("https://repo.repsy.io/mvn/buijs-dev/maven") {
            mavenContent {
                snapshotsOnly()
                includeGroupByRegex("dev\\.buijs.*")
            }
        }

        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("gradle-build")
include(":style-rules")
include(":style-cli")
include(":style-gradle-plugin")
