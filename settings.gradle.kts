pluginManagement {
    repositories {
        mavenLocal()
        maven("https://repo.repsy.io/mvn/buijs-dev/maven")
        maven("https://maven.pkg.github.com/buijs-dev/*") {
            credentials {
                username =
                    providers
                        .gradleProperty(
                            "gpr.user",
                        ).orElse(providers.environmentVariable("GITHUB_ACTOR"))
                        .orNull
                password =
                    providers
                        .gradleProperty(
                            "gpr.key",
                        ).orElse(providers.environmentVariable("GITHUB_TOKEN"))
                        .orNull
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
