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

import dev.buijs.stratastax.style.StratastaxStyle
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters

/** The [StyleTask] work, run with only the style rules on its classpath. */
abstract class StyleWork : WorkAction<StyleWork.Parameters> {

    interface Parameters : WorkParameters {

        val files: ConfigurableFileCollection
        val maxLineLength: Property<Int>
        val preserveLineBreaks: Property<Boolean>
        val licenseHeader: Property<String>
        val check: Property<Boolean>
    }

    override fun execute() {
        val style =
            StratastaxStyle(
                parameters.maxLineLength.get(),
                parameters.preserveLineBreaks.get(),
                parameters.licenseHeader.orNull,
            )
        val files = parameters.files.files.sortedBy { it.path }

        if (!parameters.check.get()) {
            val changed = files.filter { style.format(it) }

            changed.forEach { logger.lifecycle("formatted ${it.path}") }
        }

        // After formatting, whatever is left only a hand can fix.
        val violations =
            files.flatMap { file ->
                style.check(file.readText(), file.path).map {
                    "${file.path}:${it.line}:${it.column}: ${it.message} (${it.ruleId})"
                }
            }

        if (violations.isNotEmpty()) {
            val hint =
                if (parameters.check.get()) {
                    "run ${StratastaxStylePlugin.APPLY_TASK} to fix what it can"
                } else {
                    "these need fixing by hand"
                }

            throw GradleException(
                "${violations.size} style violation(s); $hint:\n" + violations.joinToString("\n"),
            )
        }
    }

    private companion object {

        val logger =
            org.gradle.api.logging.Logging
                .getLogger(StyleWork::class.java)
    }
}
