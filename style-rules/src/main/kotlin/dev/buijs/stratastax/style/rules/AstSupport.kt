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

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

internal val ASTNode.isComment: Boolean
    get() =
        elementType == ElementType.EOL_COMMENT ||
            elementType == ElementType.BLOCK_COMMENT ||
            elementType == ElementType.KDOC

/** A parameter list spread over several lines, the shape the stratastax rules apply to. */
internal val ASTNode.isMultilineParameterList: Boolean
    get() = elementType == ElementType.VALUE_PARAMETER_LIST && text.contains('\n')

/** The annotation entries of a declaration's modifier list. */
internal fun ASTNode.annotationEntries(): List<ASTNode> =
    findChildByType(ElementType.MODIFIER_LIST)
        ?.children20
        ?.filter { it.elementType == ElementType.ANNOTATION_ENTRY }
        ?.toList()
        .orEmpty()

/** The first sibling after this node that is neither whitespace nor a comment. */
internal fun ASTNode.nextCodeSibling(): ASTNode? =
    generateSequence(treeNext) { it.treeNext }.firstOrNull { !it.isWhiteSpace20 && !it.isComment }

/** Replaces the text of this whitespace leaf. */
internal fun ASTNode.replaceWhitespace(text: String) {
    (this as LeafPsiElement).rawReplaceWithText(text)
}

/** The full text of the line this node starts on, split at the node: before and after it. */
internal fun ASTNode.lineAround(end: ASTNode): Pair<String, String> {
    var root = this
    while (root.treeParent != null) root = root.treeParent
    val text = root.text
    val start = startOffset
    val lineStart = text.lastIndexOf('\n', start - 1) + 1
    val endOffset = end.startOffset + end.textLength
    val lineEnd = text.indexOf('\n', endOffset).let { if (it < 0) text.length else it }

    return text.substring(lineStart, start) to text.substring(endOffset, lineEnd)
}
