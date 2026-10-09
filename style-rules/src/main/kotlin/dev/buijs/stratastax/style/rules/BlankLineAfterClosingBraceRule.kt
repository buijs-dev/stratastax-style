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
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * A statement or declaration ending in `}`, a block, a class, a lambda, is followed
 * by a blank line before the next statement of the same block: `val x = foo.map { it }`, blank
 * line, `val y = ...`. Not before the block's own closing `}`, and only between statements on
 * separate lines.
 */
class BlankLineAfterClosingBraceRule :
    Rule(RuleId("$RULE_SET_ID:blank-line-after-closing-brace"), About()),
    RuleAutocorrectApproveHandler {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected: Boolean,
        ) -> AutocorrectDecision,
    ) {
        if (node.elementType !in CONTAINERS) return
        val statements =
            node
                .children20
                .filterNot {
                    it.isWhiteSpace20 ||
                        it.isComment ||
                        it.elementType in
                        setOf(ElementType.LBRACE, ElementType.RBRACE, ElementType.SEMICOLON)
                }.toList()
        statements.zipWithNext().forEach { (previous, next) ->
            if (previous.lastCodeLeaf()?.elementType != ElementType.RBRACE) return@forEach
            // The line break right after `previous`, past a comment trailing it on the same line.
            val lineBreak =
                generateSequence(previous.treeNext) { it.treeNext }
                    .takeWhile { it != next }
                    .firstOrNull { it.isWhiteSpace20 && it.text.contains('\n') } ?: return@forEach
            if (lineBreak.text.count { it == '\n' } >= 2) return@forEach
            emit(lineBreak.startOffset, "Expected a blank line after a closing brace", true)
                .ifAutocorrectAllowed { lineBreak.replaceWhitespace("\n" + lineBreak.text) }
        }
    }

    private fun ASTNode.lastCodeLeaf(): ASTNode? {
        var leaf = lastChildNode ?: return this
        while (true) {
            while (leaf.isWhiteSpace20 || leaf.isComment) leaf = leaf.treePrev ?: return null
            leaf = leaf.lastChildNode ?: return leaf
        }
    }

    private companion object {

        val CONTAINERS = setOf(ElementType.BLOCK, ElementType.CLASS_BODY, ElementType.FILE)
    }
}
