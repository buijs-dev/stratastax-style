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
package dev.buijs.stratastax.style.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * A lambda holding one single-line expression stays on one line when it fits: `{ it > 1 }`, never
 * `{\n it > 1\n }` just because the surrounding call was too long before the call chain got
 * wrapped. With that, a wrapped chain reads one call per line.
 */
class SingleLineLambdaRule(
    /** Lambdas, by [key], to leave over several lines: written that way, not broken open. */
    private val keepMultiLine: () -> Set<String> = { emptySet() },
) : Rule(
        RuleId(ID),
        About(),
        usesEditorConfigProperties = setOf(MAX_LINE_LENGTH_PROPERTY),
    ),
    RuleAutocorrectApproveHandler {

    private var maxLineLength = Int.MAX_VALUE

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected: Boolean,
        ) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.FUNCTION_LITERAL || !node.text.contains('\n')) return
        if (key(node) in keepMultiLine()) return
        // ktlint only formats code that parses, so a lambda has its braces and a block.
        val lbrace = node.findChildByType(ElementType.LBRACE)!!
        val rbrace = node.findChildByType(ElementType.RBRACE)!!
        val block = node.findChildByType(ElementType.BLOCK)!!
        val statement =
            block.children20
                .filterNot { it.isWhiteSpace20 }
                .toList()
                .singleOrNull() ?: return
        if (statement.isComment || statement.text.contains('\n')) return
        val parameters = node.findChildByType(ElementType.VALUE_PARAMETER_LIST)
        if (parameters != null && parameters.text.contains('\n')) return
        val (before, after) = lbrace.lineAround(rbrace)
        val arrow = if (parameters == null) "" else " ${parameters.text} ->"
        if ("$before{$arrow ${statement.text} }$after".length > maxLineLength) return
        emit(lbrace.startOffset, "A single-expression lambda that fits stays on one line", true)
            .ifAutocorrectAllowed {
                listOfNotNull(
                    block.treePrev,
                    block.firstChildNode,
                    block.lastChildNode,
                    rbrace.treePrev,
                ).filter { it.isWhiteSpace20 }
                    .forEach { it.replaceWhitespace(" ") }
            }
    }

    companion object {

        const val ID = "$RULE_SET_ID:single-line-lambda"

        /**
         * A lambda's text without any whitespace: the same key before and after formatting, which
         * only ever changes whitespace inside a single-expression lambda (`it*2` -> `it * 2`).
         */
        fun key(node: ASTNode): String = node.text.replace(Regex("\\s+"), "")
    }
}
