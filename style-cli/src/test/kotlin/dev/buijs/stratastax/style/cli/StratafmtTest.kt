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

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class StratafmtTest {

    @field:TempDir private lateinit var dir: File

    private val out = ByteArrayOutputStream()
    private val cli = Stratafmt(PrintStream(out), PrintStream(ByteArrayOutputStream()))
    private val unformatted = "class A(\n    @A @B val a: Int,\n)\n"

    @Test
    fun `check reports violations and exits 1, format fixes them, check then exits 0`() {
        // given
        val file = File(dir, "A.kt").apply { writeText(unformatted) }

        // expect
        assertThat(cli.run(listOf("check", dir.path))).isEqualTo(1)
        assertThat(out.toString())
            .contains("A.kt:2:")
            .contains("stratastax:parameter-annotation-per-line")
        assertThat(cli.run(listOf("format", dir.path))).isEqualTo(0)
        assertThat(file.readText()).isEqualTo("class A(\n    @A\n    @B\n    val a: Int,\n)\n")
        assertThat(cli.run(listOf("check", file.path))).isEqualTo(0)
    }

    @Test
    fun `with a license header, format adds it where missing`() {
        // given
        val header = File(dir, "HEADER.txt").apply { writeText("/** Licensed */\n") }

        val src = File(dir, "src").apply { mkdirs() }

        val file = File(src, "A.kt").apply { writeText("class A\n") }

        // expect
        assertThat(cli.run(listOf("check", "--license-header", header.path, src.path))).isEqualTo(1)
        assertThat(out.toString()).contains("stratastax:license-header")
        assertThat(
            cli.run(listOf("format", "--license-header", header.path, src.path)),
        ).isEqualTo(0)
        assertThat(file.readText()).isEqualTo("/** Licensed */\n\nclass A\n")
    }

    @Test
    fun `build and gradle directories are skipped`() {
        // given
        File(dir, "build/generated").mkdirs()
        File(dir, "build/generated/G.kt").writeText(unformatted)
        File(dir, "src").mkdirs()
        File(dir, "src/A.kt").writeText("class A\n")

        // expect
        assertThat(Stratafmt.kotlinFiles(dir).map { it.name }).containsExactly("A.kt")
    }

    @Test
    fun `an unknown command prints the usage and exits 2`() {
        assertThat(cli.run(listOf("lint", dir.path))).isEqualTo(2)
    }
}
