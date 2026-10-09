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
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * In a multi-line parameter list (a class constructor, typically), every annotation of a parameter
 * and the parameter itself start a line of their own:
 * ```
 * @SearchFilter
 * @PersistenceColumn(MatchColumns.PUBLIC_ID)
 * val id: MatchId,
 * ```
 * A trailing comment stays where it is.
 */
class ParameterAnnotationPerLineRule :
    Rule(RuleId("$RULE_SET_ID:parameter-annotation-per-line"), About()),
    RuleAutocorrectApproveHandler {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected: Boolean,
        ) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.VALUE_PARAMETER) return
        if (!node.treeParent.isMultilineParameterList) return
        val annotations = node.annotationEntries().ifEmpty { return }

        // Another modifier, `val`/`var` or the name: whatever follows the last annotation.
        val afterAnnotations =
            annotations.last().nextCodeSibling() ?: annotations.last().treeParent.nextCodeSibling()
        val indent = node.indent20
        (annotations.drop(1) + listOfNotNull(afterAnnotations)).forEach { element ->
            // Never the first child: an annotation follows another, the rest the annotations.
            val before = element.treePrev
            if (before.isComment) return@forEach
            if (!before.isWhiteSpace20 || !before.text.contains('\n')) {
                emit(element.startOffset, "Annotation or parameter not on its own line", true)
                    .ifAutocorrectAllowed { element.upsertWhitespaceBeforeMe(indent) }
            }
        }
    }
}
