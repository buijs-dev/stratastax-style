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
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * An empty `//` comment says nothing; it only ever served to stop another formatter from joining
 * lines (annotations stacked under each other, a blank line between parameters), which the
 * stratastax rules now do themselves. Removed, behind code and on a line of its own alike.
 */
class NoEmptyLineCommentRule :
    Rule(RuleId("$RULE_SET_ID:no-empty-line-comment"), About()),
    RuleAutocorrectApproveHandler {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected: Boolean,
        ) -> AutocorrectDecision,
    ) {
        if (node.elementType != ElementType.EOL_COMMENT || node.text.trim() != "//") return
        emit(node.startOffset, "Empty line comment", true).ifAutocorrectAllowed {
            val before = node.treePrev?.takeIf { it.isWhiteSpace20 }

            // A line break, or nothing at the end of the file.
            val after = node.treeNext

            val parent = node.treeParent
            when {
                // `code //`: drop the comment and the spaces before it.
                before != null && !before.text.contains('\n') -> parent.removeChild(before)
                // On a line of its own: drop the line, keeping the indent of what follows.
                before != null && after != null -> parent.removeChild(before)
            }

            parent.removeChild(node)
        }
    }
}
