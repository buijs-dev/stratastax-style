/**
 * Copyright (c) 2021 - 2026 Buijs Software
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.buijs.stratastax.style.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetContainer
import java.util.Properties

/** `stratastaxStyle { ... }`. */
abstract class StratastaxStyleExtension {

    /** The longest line the style allows; 100 by default. */
    abstract val maxLineLength: Property<Int>

    /** Whether `build` runs `stratastaxStyleApply` (before compiling); on by default. */
    abstract val applyOnBuild: Property<Boolean>

    /**
     * Whether formatting keeps every line break and blank line someone wrote, only ever adding
     * them; on by default.
     */
    abstract val preserveLineBreaks: Property<Boolean>

    /**
     * The license header every `.kt` source opens with: added where it is missing, reported by
     * `stratastaxStyleCheck`. Unset (the default), headers are left alone.
     */
    abstract val licenseHeader: Property<String>
}

/**
 * Applies the stratastax Kotlin style:
 * - `stratastaxStyleApply` formats, then fails on what is left - what only a hand can fix. `build`
 *   runs it before compiling, unless `applyOnBuild` is off.
 * - `stratastaxStyleCheck` changes nothing and fails on anything not in style, for CI.
 *
 * Covers every Kotlin source set and the build scripts; anything under the build directory
 * (generated code) is left alone.
 */
class StratastaxStylePlugin : Plugin<Project> {

    override fun apply(project: Project): Unit =
        with(project) {
            val extension =
                extensions.create("stratastaxStyle", StratastaxStyleExtension::class.java).apply {
                    maxLineLength.convention(100)
                    applyOnBuild.convention(true)
                    preserveLineBreaks.convention(true)
                }

            val styleClasspath =
                configurations.create(CONFIGURATION) {
                    it.isCanBeConsumed = false
                    it.description = "The stratastax style rules, run in an isolated worker."
                    it.defaultDependencies { dependencies ->
                        dependencies.add(
                            project.dependencies.create(
                                "dev.buijs.stratastax:style-rules:$styleRulesVersion",
                            ),
                        )
                        dependencies.add(project.dependencies.create(SLF4J_NOP))
                    }
                }

            fun StyleTask.configure(checkOnly: Boolean) {
                group = "formatting"
                classpath.from(styleClasspath)
                maxLineLength.set(extension.maxLineLength)
                preserveLineBreaks.set(extension.preserveLineBreaks)
                licenseHeader.set(extension.licenseHeader)
                check.set(checkOnly)
                sources.from(kotlinSources(project))
            }

            val apply =
                tasks.register(APPLY_TASK, StyleTask::class.java) {
                    it.description =
                        "Formats the Kotlin sources in the stratastax style; fails on what is " +
                        "left to fix by hand."
                    it.configure(checkOnly = false)
                }

            tasks.register(CHECK_TASK, StyleTask::class.java) {
                it.description = "Fails when a Kotlin source is not in the stratastax style."
                it.configure(checkOnly = true)
            }

            // Format first, then compile what was formatted.
            tasks
                .matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }
                .configureEach { it.mustRunAfter(apply) }

            pluginManager.withPlugin("base") {
                tasks.named("build") { build ->
                    build.dependsOn(
                        extension.applyOnBuild.map { if (it) listOf(apply) else emptyList() },
                    )
                }
            }
        }

    /** Every Kotlin source set's files plus the build scripts, never anything under build/. */
    private fun kotlinSources(project: Project) =
        project.provider {
            val buildDirectory =
                project.layout.buildDirectory
                    .get()
                    .asFile
            val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
            val kotlin =
                sourceSets
                    ?.flatMap {
                        (it.extensions.findByName("kotlin") as? SourceDirectorySet)?.files.orEmpty()
                    }.orEmpty()
            val scripts =
                project.projectDir.listFiles { file -> file.name.endsWith(".gradle.kts") }.orEmpty()
            (kotlin + scripts).filterNot { it.startsWith(buildDirectory) }
        }

    // The version this plugin was built in, from the resource its build writes.
    private val styleRulesVersion: String by lazy {
        Properties()
            .apply {
                StratastaxStylePlugin::class.java
                    .getResourceAsStream("/stratastax-style.properties")
                    ?.use { load(it) }
            }.getProperty("version")
    }

    companion object {

        const val APPLY_TASK = "stratastaxStyleApply"
        const val CHECK_TASK = "stratastaxStyleCheck"
        const val CONFIGURATION = "stratastaxStyle"
        private const val SLF4J_NOP = "org.slf4j:slf4j-nop:2.0.17"
    }
}
