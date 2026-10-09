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
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * In a multi-line primary constructor, one blank line separates two parameters when either is
 * annotated, so a stack of annotations reads as one block per parameter. Only the whitespace right
 * after the comma is touched; comments between parameters keep their own spacing.
 */
class BlankLineBetweenAnnotatedParametersRule :
    Rule(RuleId("$RULE_SET_ID:blank-line-between-annotated-parameters"), About()),
    RuleAutocorrectApproveHandler {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected: Boolean,
        ) -> AutocorrectDecision,
    ) {
        if (!node.isMultilineParameterList) return
        // Class constructors only: a function's parameters stay compact.
        if (node.treeParent.elementType != ElementType.PRIMARY_CONSTRUCTOR) return
        val parameters =
            node.children20.filter { it.elementType == ElementType.VALUE_PARAMETER }.toList()
        parameters.zipWithNext().forEach { (previous, next) ->
            if (previous.annotationEntries().isEmpty() && next.annotationEntries().isEmpty()) {
                return@forEach
            }

            // Comments may sit before the comma, but code never: the comma is next's code sibling.
            val comma = next.prevCodeSibling20!!
            val after = comma.treeNext.takeIf { it.isWhiteSpace20 } ?: return@forEach
            // A comment trailing the comma on the same line keeps its place.
            if (after.treeNext.isComment && !after.text.contains('\n')) return@forEach
            if (after.text.count { it == '\n' } != 2) {
                emit(
                    after.startOffset,
                    "Expected one blank line between annotated parameters",
                    true,
                ).ifAutocorrectAllowed {
                    after.replaceWhitespace("\n" + next.indent20)
                }
            }
        }
    }
}
