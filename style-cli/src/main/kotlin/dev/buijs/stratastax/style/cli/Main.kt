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
package dev.buijs.stratastax.style.cli

import dev.buijs.stratastax.style.StratastaxStyle
import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess

private const val USAGE =
    """Usage: stratafmt <check|format> [--max-line-length N] [--no-preserve-line-breaks] [--license-header FILE] <file or directory>...
Formats or checks every .kt and .kts file in the stratastax style; build/ and .gradle/ are skipped.
Line breaks and blank lines already there are kept unless --no-preserve-line-breaks.
With --license-header, every .kt file opens with the header in FILE: added where missing."""

fun main(args: Array<String>) {
    exitProcess(Stratafmt(System.out, System.err).run(args.toList()))
}

/** The `stratafmt` command; [run] returns the process exit code. */
class Stratafmt(
    private val out: PrintStream,
    private val err: PrintStream,
) {

    fun run(args: List<String>): Int {
        val command = args.firstOrNull()
        if (command !in setOf("check", "format")) return usage()
        var maxLineLength = StratastaxStyle.DEFAULT_MAX_LINE_LENGTH
        var preserveLineBreaks = true
        var licenseHeader: String? = null
        val paths = mutableListOf<String>()
        val rest = args.drop(1).iterator()
        while (rest.hasNext()) {
            when (val arg = rest.next()) {
                "--max-line-length" -> {
                    maxLineLength =
                        rest.takeIf { it.hasNext() }?.next()?.toIntOrNull() ?: return usage()
                }
                "--no-preserve-line-breaks" -> {
                    preserveLineBreaks = false
                }
                "--license-header" -> {
                    licenseHeader =
                        rest
                            .takeIf { it.hasNext() }
                            ?.next()
                            ?.let(
                                ::File,
                            )?.takeIf { it.isFile }
                            ?.readText()
                            ?: return usage()
                }
                else -> {
                    paths += arg
                }
            }
        }

        if (paths.isEmpty()) return usage()
        val files = paths.flatMap { kotlinFiles(File(it)) }

        val style = StratastaxStyle(maxLineLength, preserveLineBreaks, licenseHeader)
        return if (command == "check") check(style, files) else format(style, files)
    }

    private fun check(
        style: StratastaxStyle,
        files: List<File>,
    ): Int {
        var count = 0
        files.forEach { file ->
            style.check(file.readText(), file.path).forEach {
                count++
                out.println("${file.path}:${it.line}:${it.column}: ${it.message} (${it.ruleId})")
            }
        }

        out.println("$count violation(s) in ${files.size} file(s)")
        return if (count == 0) 0 else 1
    }

    private fun format(
        style: StratastaxStyle,
        files: List<File>,
    ): Int {
        val changed = files.filter { style.format(it) }

        changed.forEach { out.println("formatted ${it.path}") }

        out.println("${changed.size} of ${files.size} file(s) formatted")
        return 0
    }

    private fun usage(): Int {
        err.println(USAGE)
        return 2
    }

    companion object {

        private val SKIPPED_DIRECTORIES = setOf("build", ".gradle")

        /** [root] itself when a Kotlin file, else every Kotlin file below it. */
        fun kotlinFiles(root: File): List<File> =
            root
                .walkTopDown()
                .onEnter { it == root || it.name !in SKIPPED_DIRECTORIES }
                .filter { it.isFile && (it.extension == "kt" || it.extension == "kts") }
                .sortedBy { it.path }
                .toList()
    }
}
