plugins {
    id("stratastax.coverage")
    id("stratastax.dokka")
}

val stratafmt = configurations.create("stratafmt") { isCanBeConsumed = false }

dependencies {
    stratafmt(project(":style-cli"))

    kover(project(":style-rules"))
    kover(project(":style-cli"))
    kover(project(":style-gradle-plugin"))

    dokka(project(":style-rules"))
    dokka(project(":style-gradle-plugin"))
}

// stratastax-style can't apply its own plugin (it is built here), so it formats itself with
// style-cli: `stratastaxStyleApply` formats, `stratastaxStyleCheck` only checks (for CI).
val licenseHeader = layout.projectDirectory.file("gradle/license-header.txt")
val styledSources =
    listOf("style-rules", "style-cli", "style-gradle-plugin").flatMap { module ->
        listOf("$module/src", "$module/build.gradle.kts")
    } + listOf("build.gradle.kts", "settings.gradle.kts")

fun registerStyleTask(
    name: String,
    command: String,
) = tasks.register<JavaExec>(name) {
    group = "formatting"
    description = "Runs stratafmt $command over this build's own Kotlin sources."
    classpath = stratafmt
    mainClass.set("dev.buijs.stratastax.style.cli.MainKt")
    args(command, "--license-header", licenseHeader.asFile.path)
    args(
        styledSources.map {
            layout.projectDirectory
                .file(it)
                .asFile.path
        },
    )
}

val styleApply = registerStyleTask("stratastaxStyleApply", "format")
registerStyleTask("stratastaxStyleCheck", "check")

// Unlike the plugin, this can't format before compiling: stratafmt is compiled from these sources.
subprojects { pluginManager.withPlugin("base") { tasks.named("build") { dependsOn(styleApply) } } }
