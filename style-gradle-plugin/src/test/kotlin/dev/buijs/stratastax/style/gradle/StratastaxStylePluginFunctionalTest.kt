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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Runs the plugin in a real build; the style rules resolve from the test repository. */
class StratastaxStylePluginFunctionalTest {

    @field:TempDir private lateinit var projectDir: File

    private val testRepository = System.getProperty("stratastax.style.testRepository")

    private fun runner(vararg tasks: String) =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*tasks, "--stacktrace")

    private fun project(script: String) {
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"sample\"\n")
        File(projectDir, "build.gradle.kts")
            .writeText(
                """
                plugins {
                    base
                    id("dev.buijs.stratastax.style")
                }

                repositories {
                    maven { url = uri("${testRepository.replace("\\", "/")}") }
                    mavenCentral()
                }
                """.trimIndent() + "\n",
            )
        File(projectDir, "extra.gradle.kts").writeText(script)
    }

    private val unformatted = "class A(\n    @Suppress(\"a\") @Deprecated(\"b\") val a: Int,\n)\n"

    private val formatted =
        "class A(\n    @Suppress(\"a\")\n    @Deprecated(\"b\")\n    val a: Int,\n)\n"

    @Test
    fun `build formats what it can and succeeds`() {
        project(unformatted)

        val result = runner("build").build()

        assertThat(result.task(":stratastaxStyleApply")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(File(projectDir, "extra.gradle.kts").readText()).isEqualTo(formatted)
    }

    @Test
    fun `build fails on what only a hand can fix`() {
        val name = "aVeryLongVariableNameThatKeepsGoing"
        project(
            "val $name = 1\n" +
                "if ($name == 1 ||\n    $name == 2 ||\n    $name == 3 ||\n    $name == 4\n) {\n" +
                "    println($name)\n}\n",
        )

        val result = runner("build").buildAndFail()

        assertThat(result.output)
            .contains("these need fixing by hand")
            .contains("stratastax:single-line-condition")
    }

    @Test
    fun `stratastaxStyleCheck fails without changing anything`() {
        project(unformatted)

        val result = runner("stratastaxStyleCheck").buildAndFail()

        assertThat(result.output)
            .contains("run stratastaxStyleApply to fix what it can")
            .contains("stratastax:parameter-annotation-per-line")
        assertThat(File(projectDir, "extra.gradle.kts").readText()).isEqualTo(unformatted)
    }

    @Test
    fun `applyOnBuild off leaves build alone`() {
        project(unformatted)
        File(projectDir, "build.gradle.kts")
            .appendText("stratastaxStyle { applyOnBuild.set(false) }\n")

        val result = runner("build").build()

        assertThat(result.task(":stratastaxStyleApply")).isNull()
        assertThat(File(projectDir, "extra.gradle.kts").readText()).isEqualTo(unformatted)
    }

    @Test
    fun `a license header leaves build scripts without one`() {
        project(formatted)
        File(projectDir, "build.gradle.kts")
            .appendText("stratastaxStyle { licenseHeader.set(\"/** Licensed */\") }\n")

        val result = runner("stratastaxStyleApply").build()

        assertThat(result.task(":stratastaxStyleApply")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(File(projectDir, "extra.gradle.kts").readText()).isEqualTo(formatted)
        assertThat(
            File(projectDir, "build.gradle.kts").readText(),
        ).doesNotContain("/** Licensed */\n\n")
    }
}
