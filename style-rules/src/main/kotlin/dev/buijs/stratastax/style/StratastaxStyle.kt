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

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.ruleset.standard.StandardRuleSetProvider
import com.pinterest.ktlint.ruleset.standard.rules.MaxLineLengthRule
import dev.buijs.stratastax.style.rules.SingleLineLambdaRule
import dev.buijs.stratastax.style.rules.StratastaxRuleSetProvider
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import java.io.File
import java.nio.file.Paths

/**
 * The stratastax Kotlin style: ktlint's `ktlint_official` code style plus the stratastax rules,
 * without four standard rules: `no-blank-line-in-list` (it would undo the blank lines between
 * annotated parameters), `condition-wrapping` (one operand per line, where a condition sits on
 * one), and `kdoc` and `no-consecutive-comments` (both reject the `/** license */` header every
 * stratastax file opens with).
 * The one entry point the CLI, the Gradle plugin and the code generator share, so all three formats are
 * alike.
 *
 * With [preserveLineBreaks] (the default, for handwritten code) formatting only ever adds line
 * breaks and blank lines, never removes one someone put there: six more rules are off (see
 * [PRESERVE_DISABLED_RULES]) and a lambda is only joined back onto one line when the formatter
 * itself broke it open, never when it was written over several lines. Generated code has no
 * breaks worth keeping and is formatted without.
 *
 * With a [licenseHeader], a `.kt` file that does not open with it gets it added on top; build
 * scripts (`.kts`) are left without.
 */
class StratastaxStyle(
    maxLineLength: Int = DEFAULT_MAX_LINE_LENGTH,
    private val preserveLineBreaks: Boolean = true,
    licenseHeader: String? = null,
) {

    private val licenseHeader: String? = licenseHeader?.trim()?.takeIf { it.isNotEmpty() }

    /** One style violation: a 1-based [line]/[column] and the rule that reported it. */
    data class Violation(
        val line: Int,
        val column: Int,
        val ruleId: String,
        val message: String,
    )

    /** The lambdas written over several lines in the code being formatted, see [writtenMultiLine]. */
    private val multiLineLambdas = ThreadLocal<Set<String>>()

    private val engine =
        KtLintRuleEngine(
            ruleProviders =
                (
                    StandardRuleSetProvider().getRuleProviders() +
                        StratastaxRuleSetProvider().getRuleProviders()
                ).filterNot {
                    it.ruleId.value in DISABLED_STANDARD_RULES ||
                        (preserveLineBreaks && it.ruleId.value in PRESERVE_DISABLED_RULES) ||
                        it.ruleId.value == SingleLineLambdaRule.ID
                }.toSet() +
                    RuleProvider {
                        SingleLineLambdaRule(
                            keepMultiLine = { multiLineLambdas.get().orEmpty() },
                        )
                    },
            editorConfigOverride =
                EditorConfigOverride.from(
                    CODE_STYLE_PROPERTY to CodeStyleValue.ktlint_official,
                    MAX_LINE_LENGTH_PROPERTY to maxLineLength,
                    // A test named in backticks can't be wrapped; it doesn't count against the length.
                    MaxLineLengthRule.IGNORE_BACKTICKED_IDENTIFIER_PROPERTY to true,
                ),
        )

    private val formatWithMoreRuns: ((Code) -> String)? =
        runCatching {
            val formatter =
                KtLintRuleEngine::class
                    .java
                    .getDeclaredField("codeFormatter")
                    .apply { isAccessible = true }
                    .get(engine)
            val internal = "com.pinterest.ktlint.rule.engine.internal"
            val handlerType = Class.forName("$internal.AutocorrectHandler")
            val formatAll =
                Class
                    .forName(
                        "$internal.AllAutocorrectHandler",
                    ).getField("INSTANCE")
                    .get(null)
            val format =
                formatter.javaClass.getMethod(
                    "format",
                    Code::class.java,
                    handlerType,
                    Function2::class.java,
                    Int::class.javaPrimitiveType,
                )
            val ignore: (Any, Boolean) -> Unit = { _, _ -> }

            val run: (Code) -> String = { code ->
                format.invoke(formatter, code, formatAll, ignore, MAX_FORMAT_RUNS) as String
            }

            run
        }.getOrNull()

    /** Whether formatting runs up to [MAX_FORMAT_RUNS] times, not ktlint's public 3. */
    val formatsWithMoreRuns: Boolean
        get() = formatWithMoreRuns != null

    /**
     * [content] formatted. [fileName] decides between a `.kt` and a `.kts` parse and is what
     * file-level rules (e.g., a file named after its single class) judge.
     */
    fun format(
        content: String,
        fileName: String = "File.kt",
    ): String {
        val code = code(withLicenseHeader(content, fileName), fileName)
        return keepingLineBreaksOf(code) {
            formatWithMoreRuns?.invoke(code)
                ?: engine.format(code) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }
        }
    }

    /** Formats [file] in place; `true` when it changed. */
    fun format(file: File): Boolean {
        val original = file.readText()
        val formatted = format(original, file.path)
        if (formatted == original) return false
        file.writeText(formatted)
        return true
    }

    /** Every violation in [content] that formatting would not fix on its own, or would fix. */
    fun check(
        content: String,
        fileName: String = "File.kt",
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        if (missesLicenseHeader(content, fileName)) {
            violations += Violation(1, 1, LICENSE_HEADER_RULE_ID, "Missing license header")
        }

        val code = code(content, fileName)
        keepingLineBreaksOf(code) {
            engine.lint(code) {
                violations += Violation(it.line, it.col, it.ruleId.value, it.detail)
            }
        }

        return violations
    }

    /** Runs [block] knowing which lambdas [code] writes over several lines, see [multiLineLambdas]. */
    private fun <T> keepingLineBreaksOf(
        code: Code,
        block: () -> T,
    ): T {
        if (!preserveLineBreaks) return block()
        multiLineLambdas.set(writtenMultiLine(code))
        try {
            return block()
        } finally {
            multiLineLambdas.remove()
        }
    }

    /** Every lambda [code] writes over several lines, as [SingleLineLambdaRule.key]. */
    private fun writtenMultiLine(code: Code): Set<String> {
        val lambdas = mutableSetOf<String>()

        fun visit(node: ASTNode) {
            if (node.elementType == ElementType.FUNCTION_LITERAL && node.text.contains('\n')) {
                lambdas += SingleLineLambdaRule.key(node)
            }

            generateSequence(node.firstChildNode) { it.treeNext }.forEach(::visit)
        }

        visit(engine.transformToAst(code))
        return lambdas
    }

    /** [content] opening with the [licenseHeader] when it [missesLicenseHeader]. */
    private fun withLicenseHeader(
        content: String,
        fileName: String,
    ): String =
        if (missesLicenseHeader(content, fileName)) {
            "$licenseHeader\n\n${content.trimStart()}"
        } else {
            content
        }

    private fun missesLicenseHeader(
        content: String,
        fileName: String,
    ): Boolean =
        licenseHeader != null &&
            !fileName.endsWith(".kts") &&
            !content.trimStart().startsWith(licenseHeader)

    private fun code(
        content: String,
        fileName: String,
    ): Code = Code.fromSnippetWithPath(content, Paths.get(fileName))

    companion object {

        const val DEFAULT_MAX_LINE_LENGTH = 100

        /** The rule id a file without the configured license header is reported under. */
        const val LICENSE_HEADER_RULE_ID = "stratastax:license-header"

        /**
         * Off with [preserveLineBreaks]: each removes a line break or blank line someone wrote. The
         * stratastax `single-line-lambda` stays on, but only for lambdas the formatter broke open.
         */
        val PRESERVE_DISABLED_RULES =
            setOf(
                "standard:function-expression-body",
                "standard:no-empty-first-line-in-method-block",
                "standard:no-empty-first-line-in-class-body",
                "standard:no-blank-line-before-rbrace",
                "standard:no-blank-lines-in-chained-method-calls",
                "standard:blank-line-between-when-conditions",
            )

        /** Enough for generated code nested several levels deep; more means rules oscillate. */
        const val MAX_FORMAT_RUNS = 10

        // Each undone by, or undoing, a stratastax rule - see the class kdoc.
        private val DISABLED_STANDARD_RULES =
            setOf(
                "standard:no-blank-line-in-list",
                "standard:kdoc",
                // A KDoc right below the `/** license */` header is a KDoc after a KDoc.
                "standard:no-consecutive-comments",
                "standard:condition-wrapping",
            )
    }
}
