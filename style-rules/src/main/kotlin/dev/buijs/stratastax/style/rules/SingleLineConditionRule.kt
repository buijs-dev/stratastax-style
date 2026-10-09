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
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * The condition of an `if`, `while` or `do-while` sits on one line: `if (x == 1 || x == 2)`, never
 * one operand per line. A condition that doesn't fit, or holds a comment, a multi-statement lambda
 * or a `when`, is reported for a hand to fix - extract it into a `val`.
 */
class SingleLineConditionRule :
    Rule(
        RuleId("$RULE_SET_ID:single-line-condition"),
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
        if (node.elementType !in setOf(ElementType.IF, ElementType.WHILE, ElementType.DO_WHILE)) {
            return
        }

        val lpar = node.findChildByType(ElementType.LPAR) ?: return
        val rpar = node.findChildByType(ElementType.RPAR) ?: return
        val parts =
            generateSequence(
                lpar.treeNext,
            ) { it.treeNext }.takeWhile { it != rpar }.toList()
        if (parts.none { it.text.contains('\n') }) return
        val leaves = parts.flatMap { it.leaves() }

        val unjoinable =
            leaves.any { it.isComment } ||
                parts.any { part ->
                    part.descendants().any {
                        it.elementType == ElementType.WHEN ||
                            it.elementType == ElementType.OBJECT_LITERAL ||
                            (it.elementType == ElementType.BLOCK && it.statementCount() > 1)
                    }
                }

        // Line breaks become a single space; right inside the parentheses they go.
        val joined =
            leaves.joinToString("") { leaf ->
                when {
                    !leaf.isWhiteSpace20 || !leaf.text.contains('\n') -> leaf.text
                    leaf.treePrev == lpar || leaf.treeNext == rpar -> ""
                    else -> " "
                }
            }

        val (before, after) = lpar.lineAround(rpar)
        if (unjoinable || "$before($joined)$after".length > maxLineLength) {
            emit(
                lpar.startOffset,
                "Condition doesn't fit on one line; extract it into a val",
                false,
            )
            return
        }

        emit(lpar.startOffset, "Condition spread over several lines", true).ifAutocorrectAllowed {
            leaves
                .filter { it.isWhiteSpace20 && it.text.contains('\n') }
                .forEach { leaf ->
                    if (leaf.treePrev == lpar || leaf.treeNext == rpar) {
                        leaf.treeParent.removeChild(leaf)
                    } else {
                        leaf.replaceWhitespace(" ")
                    }
                }
        }
    }

    private fun ASTNode.leaves(): List<ASTNode> =
        if (firstChildNode == null) {
            listOf(this)
        } else {
            descendants().filter { it.firstChildNode == null }.toList()
        }

    private fun ASTNode.descendants(): Sequence<ASTNode> =
        generateSequence(
            firstChildNode,
        ) { it.treeNext }.flatMap { sequenceOf(it) + it.descendants() }

    private fun ASTNode.statementCount(): Int =
        generateSequence(firstChildNode) { it.treeNext }
            .count {
                !it.isWhiteSpace20 && !it.isComment && it.elementType != ElementType.SEMICOLON
            }
}
