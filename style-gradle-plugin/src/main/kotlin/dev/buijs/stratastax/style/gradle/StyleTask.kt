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

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Formats [sources] and fails on what is left ([check] `false`), or only checks them ([check]
 * `true`), in an isolated worker process.
 */
@DisableCachingByDefault(because = "Formats the sources in place, or only reads them")
abstract class StyleTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Classpath abstract val classpath: ConfigurableFileCollection

    @get:Input abstract val maxLineLength: Property<Int>

    @get:Input abstract val preserveLineBreaks: Property<Boolean>

    @get:Input @get:Optional
    abstract val licenseHeader: Property<String>

    @get:Input abstract val check: Property<Boolean>

    @get:Inject abstract val workers: WorkerExecutor

    @TaskAction
    fun run() {
        // A worker process of its own: ktlint and its Kotlin compiler would otherwise fill the
        // daemon's metaspace, once per module. Gradle reuses the process across tasks.
        workers
            .processIsolation {
                it.classpath.from(classpath)
                it.forkOptions.maxHeapSize = "1g"
            }.submit(StyleWork::class.java) {
                it.files.from(sources)
                it.maxLineLength.set(maxLineLength)
                it.preserveLineBreaks.set(preserveLineBreaks)
                it.licenseHeader.set(licenseHeader)
                it.check.set(check)
            }
    }
}
