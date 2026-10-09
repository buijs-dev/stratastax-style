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

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

internal const val RULE_SET_ID = "stratastax"

/** The stratastax rules as a ktlint rule set, for the ktlint CLI and IDE plugins. */
class StratastaxRuleSetProvider : RuleSetProviderV3(RuleSetId(RULE_SET_ID)) {

    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { ParameterAnnotationPerLineRule() },
            RuleProvider { BlankLineBetweenAnnotatedParametersRule() },
            RuleProvider { SingleLineLambdaRule() },
            RuleProvider { NoEmptyLineCommentRule() },
            RuleProvider { BlankLineAfterClosingBraceRule() },
            RuleProvider { SingleLineConditionRule() },
        )
}
