plugins {
    id("stratastax.library")
}

description = "The stratastax Kotlin style: ktlint_official plus the stratastax rules"

dependencies {
    api(libs.ktlint.rule.engine)
    implementation(libs.ktlint.rule.engine.core)
    implementation(libs.ktlint.ruleset.standard)
    // Registers the rule set for the ktlint CLI and IDE plugins (META-INF/services).
    implementation(libs.ktlint.cli.ruleset.core)
    // ktlint logs through slf4j without bringing it; the host (Gradle, the CLI) binds it.
    implementation(libs.slf4j.api)

    testImplementation(libs.bundles.test)
    testImplementation(kotlin("test"))
    // Captures ktlint's own warnings, see StratastaxStyleTest.
    testImplementation(libs.logback.classic)
}

publishing {
    // For style-gradle-plugin's functional tests.
    repositories.maven {
        name = "test"
        url =
            uri(
                rootProject.layout.projectDirectory.dir(
                    "style-gradle-plugin/build/test-repository",
                ),
            )
    }
}