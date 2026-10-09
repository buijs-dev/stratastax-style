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
package dev.buijs.stratastax.style

import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StratastaxStyleTest {

    private val style = StratastaxStyle()

    private fun formatted(source: String): String {
        val once = style.format(source.trimIndent() + "\n")
        // Every case doubles as an idempotency check: formatting the result changes nothing.
        assertThat(style.format(once)).isEqualTo(once)
        return once
    }

    @Test
    fun `every annotation of a constructor parameter gets a line of its own`() {
        assertThat(
            formatted(
                """
                    class Match(
                        @SearchFilter @PersistenceId @PersistenceColumn("ID") val id: MatchId,
                    )
                    """,
            ),
        ).isEqualTo(
            """
                |class Match(
                |    @SearchFilter
                |    @PersistenceId
                |    @PersistenceColumn("ID")
                |    val id: MatchId,
                |)
                |
            """.trimMargin(),
        )
    }

    @Test
    fun `annotated constructor parameters are separated by a blank line, plain ones are not`() {
        assertThat(
            formatted(
                """
                    class Match(
                        @SearchFilter
                        val id: MatchId,
                        @SearchSort
                        val playedAt: OffsetDateTime,
                        val note: String,
                    )

                    class Plain(
                        val a: String,
                        val b: String,
                    )
                    """,
            ),
        ).isEqualTo(
            """
                |class Match(
                |    @SearchFilter
                |    val id: MatchId,
                |
                |    @SearchSort
                |    val playedAt: OffsetDateTime,
                |
                |    val note: String,
                |)
                |
                |class Plain(
                |    val a: String,
                |    val b: String,
                |)
                |
            """.trimMargin(),
        )
    }

    @Test
    fun `an annotated parameter puts even a short parameter list on separate lines`() {
        // ktlint_official's own choice, in line with one annotation per line.
        assertThat(style.format("fun f(@Suppress(\"x\") a: Int) = a\n"))
            .isEqualTo("fun f(\n    @Suppress(\"x\")\n    a: Int,\n) = a\n")
    }

    @Test
    fun `comments next to annotations are kept where they are`() {
        val result =
            formatted(
                """
                class Match(
                    @SearchFilter // filterable
                    @PersistenceId
                    val id: MatchId,
                    // the game played
                    @SearchFilter
                    val game: GameRef,
                )
                """,
            )

        assertThat(result)
            .contains("    @SearchFilter // filterable\n    @PersistenceId\n    val id: MatchId,")
            .contains("    // the game played\n    @SearchFilter\n    val game: GameRef,")
    }

    @Test
    fun `a wrapped call chain puts one call per line and keeps short lambdas on one line`() {
        assertThat(
            formatted(
                """
                    fun top(x: List<Int>) = x.filter { it > 1 }.map { it * 2 }.sortedDescending().take(10).joinToString(separator = ", ")
                    """,
            ),
        ).isEqualTo(
            """
                |fun top(x: List<Int>) =
                |    x
                |        .filter { it > 1 }
                |        .map { it * 2 }
                |        .sortedDescending()
                |        .take(10)
                |        .joinToString(separator = ", ")
                |
            """.trimMargin(),
        )
    }

    @Test
    fun `a lambda with several statements is never collapsed`() {
        val result =
            formatted(
                """
                fun f(x: List<Int>) =
                    x.map {
                        val y = it * 2
                        y + 1
                    }
                """,
            )

        assertThat(result).contains("        val y = it * 2\n        y + 1\n")
    }

    @Test
    fun `check reports what format would fix`() {
        val source = "class A(\n    @A @B val a: Int,\n)\n"

        assertThat(style.check(source, "A.kt").map { it.ruleId })
            .contains("stratastax:parameter-annotation-per-line")
        assertThat(style.check(style.format(source, "A.kt"), "A.kt")).isEmpty()
    }

    @Test
    fun `empty line comments, the old workaround, are removed`() {
        assertThat(
            formatted(
                """
                    class Match(
                        @SearchFilter //
                        @PersistenceId //
                        val id: MatchId, //

                        //
                        @SearchFilter
                        val game: GameRef, // the game played
                    )
                    """,
            ),
        ).isEqualTo(
            """
                |class Match(
                |    @SearchFilter
                |    @PersistenceId
                |    val id: MatchId,
                |
                |    @SearchFilter
                |    val game: GameRef, // the game played
                |)
                |
            """.trimMargin(),
        )
    }

    @Test
    fun `function parameters get annotations per line but no blank lines`() {
        assertThat(
            formatted(
                """
                    interface Api {
                        fun thing(
                            @QueryParam("id") ids: String,
                            @QueryParam("type") type: String,
                        ): String
                    }
                    """,
            ),
        ).contains(
            "        @QueryParam(\"id\")\n        ids: String,\n        @QueryParam(\"type\")\n",
        )
    }

    // --- class header

    @Test
    fun `no blank line is added after a class header, an empty body is dropped by ktlint`() {
        assertThat(
            formatted(
                """
                    class MyClass {
                        val x = 1
                    }

                    interface Api {
                        fun a(): Int
                    }

                    class Empty {}
                    """,
            ),
        ).isEqualTo(
            """
                |class MyClass {
                |    val x = 1
                |}
                |
                |interface Api {
                |    fun a(): Int
                |}
                |
                |class Empty
                |
            """.trimMargin(),
        )
    }

    @Test
    fun `a blank line written after a class header is kept`() {
        val source = "class MyClass {\n\n    val x = 1\n}\n"

        assertThat(formatted(source)).isEqualTo(source)
    }

    @Test
    fun `without preserveLineBreaks a blank line after a class header is removed`() {
        val compact = StratastaxStyle(preserveLineBreaks = false)

        assertThat(compact.format("class MyClass {\n\n    val x = 1\n}\n"))
            .isEqualTo("class MyClass {\n    val x = 1\n}\n")
    }

    // --- blank-line-after-closing-brace

    @Test
    fun `a statement ending in a closing brace is followed by a blank line`() {
        assertThat(
            formatted(
                """
                    fun f(items: List<Int>): Int {
                        val doubled = items.map { it * 2 }
                        val sum = doubled.sum()
                        if (sum > 10) {
                            println(sum)
                        }
                        return sum
                    }
                    """,
            ),
        ).isEqualTo(
            """
                |fun f(items: List<Int>): Int {
                |    val doubled = items.map { it * 2 }
                |
                |    val sum = doubled.sum()
                |    if (sum > 10) {
                |        println(sum)
                |    }
                |
                |    return sum
                |}
                |
            """.trimMargin(),
        )
    }

    @Test
    fun `no blank line before the closing brace or inside one statement`() {
        val result =
            formatted(
                """
                fun f(x: Int): Int {
                    val y =
                        if (x > 1) {
                            x
                        } else {
                            0
                        }
                    return listOf(y).map { it }.first()
                }
                """,
            )

        assertThat(result)
            .contains("        } else {\n")
            .contains("    return listOf(y).map { it }.first()\n}\n")
    }

    // --- single-line-condition

    @Test
    fun `a condition over several lines is joined when it fits`() {
        assertThat(
            formatted(
                """
                    fun f(x: Int) {
                        if (x == 1 ||
                            x == 2 ||
                            x == 3
                        ) {
                            println(x)
                        }
                    }
                    """,
            ),
        ).contains("    if (x == 1 || x == 2 || x == 3) {\n")
    }

    @Test
    fun `a condition that does not fit on one line is reported for a hand to fix`() {
        val source =
            """
            fun f(firstVeryLongName: Int, secondVeryLongName: Int, thirdVeryLongName: Int) {
                if (firstVeryLongName == 1 ||
                    secondVeryLongName == 2 ||
                    thirdVeryLongName == 3 ||
                    firstVeryLongName + secondVeryLongName == thirdVeryLongName
                ) {
                    println(firstVeryLongName)
                }
            }
            """.trimIndent() + "\n"

        val violations = style.check(style.format(source))

        assertThat(violations.map { it.ruleId }).contains("stratastax:single-line-condition")
    }

    // --- review of the earlier rules

    @Test
    fun `use-site targets, multi-line annotation arguments, defaults and vararg`() {
        val result =
            formatted(
                """
                class Dto(
                    @field:Json("id") @Size(
                        min = 1,
                        max = 10,
                    ) val id: String = "x",
                    vararg val tags: String,
                )
                """,
            )

        assertThat(result)
            .contains("    @field:Json(\"id\")\n    @Size(\n")
            .contains("    val id: String = \"x\",\n\n    vararg val tags: String,\n")
    }

    @Test
    fun `lambdas with labels, destructuring and braces in strings`() {
        val result =
            formatted(
                """
                fun f(m: Map<String, Int>) =
                    m.entries.filter { (k, v) ->
                        k.isNotEmpty()
                    }.map { e -> "{" + e.key + "}" }.forEach inner@{
                        if (it.isEmpty()) return@inner
                    }
                """,
            )

        assertThat(result)
            // Written over several lines: kept that way.
            .contains(".filter { (k, v) ->\n            k.isNotEmpty()\n        }")
            .contains(".map { e -> \"{\" + e.key + \"}\" }")
    }

    @Test
    fun `a line comment with text stays, an empty one right after a brace goes`() {
        val result =
            formatted(
                """
                fun f() {
                    //
                    // explained
                    val x = 1
                }
                """,
            )

        assertThat(result).isEqualTo("fun f() {\n    // explained\n    val x = 1\n}\n")
    }

    // --- preserveLineBreaks

    @Test
    fun `line breaks and blank lines someone wrote are kept`() {
        val source =
            """
            fun f(items: List<Int>): Int {

                val doubled = items.map {
                    it * 2
                }

                val total = doubled
                    .map { it }

                    .sum()
                val kind = when (total) {
                    1 -> 1

                    else -> 2
                }

                return total + kind

            }

            fun g(): Int {
                return 1
            }
            """.trimIndent() + "\n"

        val result = formatted(source)

        assertThat(result)
            .contains("{\n\n    val doubled")
            // ktlint_official adds a break after `=` before a multi-line expression; it keeps ours.
            .contains("items.map {\n            it * 2\n        }")
            .contains(".map { it }\n\n            .sum()")
            .contains("1 -> 1\n\n            else -> 2")
            .contains("return total + kind\n\n}")
            .contains("fun g(): Int {\n    return 1\n}")
    }

    @Test
    fun `a lambda the formatter broke open is joined again, even when preserving`() {
        // The same chain as above, written on one line: every lambda one-line, so all repaired.
        val result =
            formatted(
                "fun top(x: List<Int>) = x.filter { it > 1 }.map { it * 2 }" +
                    ".sortedDescending().take(10).joinToString(separator = \", \")\n",
            )

        assertThat(result).contains("        .filter { it > 1 }\n        .map { it * 2 }\n")
    }

    @Test
    fun `without preserving, lambdas and blank lines are normalized`() {
        val compact = StratastaxStyle(preserveLineBreaks = false)
        val source =
            "fun f(): Int {\n\n    val x = listOf(1).map {\n        it\n    }\n" +
                "    return x.first()\n}\n"

        assertThat(compact.format(source))
            .contains("{\n    val x =\n        listOf(1).map { it }\n\n    return")
    }

    @Test
    fun `a missing license header is added on top`() {
        val licensed = StratastaxStyle(licenseHeader = HEADER)

        assertThat(licensed.format("package a\n\nclass A\n"))
            .isEqualTo("$HEADER\n\npackage a\n\nclass A\n")
    }

    @Test
    fun `a present license header is left alone`() {
        val licensed = StratastaxStyle(licenseHeader = HEADER)
        val source = "$HEADER\n\npackage a\n\nclass A\n"

        assertThat(licensed.format(source)).isEqualTo(source)
        assertThat(licensed.check(source, "A.kt")).isEmpty()
    }

    @Test
    fun `a missing license header is reported, build scripts are left without`() {
        val licensed = StratastaxStyle(licenseHeader = HEADER)

        assertThat(licensed.check("package a\n\nclass A\n", "A.kt").map { it.ruleId })
            .containsExactly(StratastaxStyle.LICENSE_HEADER_RULE_ID)
        assertThat(licensed.check("plugins { java }\n", "build.gradle.kts")).isEmpty()
        assertThat(licensed.format("plugins { java }\n", "build.gradle.kts"))
            .isEqualTo("plugins { java }\n")
    }

    @Test
    fun `without a license header nothing is added or reported`() {
        assertThat(style.format("package a\n\nclass A\n")).isEqualTo("package a\n\nclass A\n")
        assertThat(style.check("package a\n\nclass A\n", "A.kt")).isEmpty()
    }

    @Test
    fun `a function named in backticks may exceed the max line length`() {
        val name =
            "a test name that keeps going and going " +
                "well past the one hundred characters a line may have"
        val source = "class ATest {\n    fun `$name`() = Unit\n}\n"

        assertThat(style.check(source, "ATest.kt").map { it.ruleId })
            .doesNotContain("standard:max-line-length")
    }

    @Test
    fun `a blank license header counts as none`() {
        val blank = StratastaxStyle(licenseHeader = "  \n ")

        assertThat(blank.format("package a\n\nclass A\n")).isEqualTo("package a\n\nclass A\n")
        assertThat(blank.check("package a\n\nclass A\n", "A.kt")).isEmpty()
    }

    @Test
    fun `a violation tells where it is and what is wrong`() {
        val violation =
            style
                .check("class A(\n    @A @B val a: Int,\n)\n", "A.kt")
                .first { it.ruleId == "stratastax:parameter-annotation-per-line" }

        assertThat(violation.line).isEqualTo(2)
        assertThat(violation.column).isEqualTo(8)
        assertThat(violation.message).isEqualTo("Annotation or parameter not on its own line")
    }

    @Test
    fun `a file is formatted in place, and only written when it changed`(
        @TempDir
        dir: File,
    ) {
        val file = dir.resolve("A.kt").apply { writeText("class A(\n    @A @B val a: Int,\n)\n") }

        assertThat(style.format(file)).isTrue()
        assertThat(file.readText()).isEqualTo("class A(\n    @A\n    @B\n    val a: Int,\n)\n")
        assertThat(style.format(file)).isFalse()
    }

    @Test
    fun `without access to ktlint's internals formatting falls back to its public API`() {
        val fallback =
            StratastaxStyle(
                StratastaxStyle.DEFAULT_MAX_LINE_LENGTH,
                preserveLineBreaks = true,
                licenseHeader = null,
                ktlintInternals = "dev.buijs.missing",
            )

        assertThat(fallback.formatsWithMoreRuns).isFalse()
        assertThat(fallback.format("class A(\n    @A @B val a: Int,\n)\n"))
            .isEqualTo("class A(\n    @A\n    @B\n    val a: Int,\n)\n")
    }

    @Test
    fun `the rules turned off to preserve line breaks are standard rules`() {
        val standard = StandardRuleSetProvider().getRuleProviders().map { it.ruleId.value }

        assertThat(standard).containsAll(StratastaxStyle.PRESERVE_DISABLED_RULES)
    }

    private companion object {

        const val HEADER = "/**\n * Copyright (c) Buijs Software\n */"
    }
}
