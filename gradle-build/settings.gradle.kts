pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.github.com/buijs-dev/*") {
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
            mavenContent {
                releasesOnly()
                includeGroupByRegex("dev\\.buijs.*")
            }
        }
        maven("https://repo.repsy.io/mvn/buijs-dev/maven") {
            mavenContent {
                snapshotsOnly()
                includeGroupByRegex("dev\\.buijs.*")
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}