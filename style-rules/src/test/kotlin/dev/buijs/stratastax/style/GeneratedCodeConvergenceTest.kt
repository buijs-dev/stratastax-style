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

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.slf4j.LoggerFactory

/**
 * Deeply nested generated code needs more format runs than ktlint's public API allows (3); then it
 * warns "Format was not able to resolve all violations ... in 3 consecutive runs". These fixtures
 * are raw stratastax codegen output that needs exactly that many.
 */
class GeneratedCodeConvergenceTest {

    // As the code generator formats: its line breaks aren't anyone's to keep.
    private val style = StratastaxStyle(preserveLineBreaks = false)

    @ParameterizedTest
    @ValueSource(strings = ["MatchRowMapper", "MatchPublicApiMapper"])
    fun `generated code is formatted in one call, without ktlint warning it gave up`(name: String) {
        val raw = javaClass.getResource("/generated/$name.kt.txt")!!.readText()
        val warnings = ListAppender<ILoggingEvent>().apply { start() }

        val logger = LoggerFactory.getLogger("com.pinterest.ktlint") as Logger
        logger.addAppender(warnings)
        try {
            val formatted = style.format(raw, "$name.kt")

            assertThat(style.formatsWithMoreRuns).isTrue()
            assertThat(warnings.list.map { it.formattedMessage })
                .noneMatch { it.startsWith("Format was not able") }

            assertThat(style.format(formatted, "$name.kt")).isEqualTo(formatted)
        } finally {
            logger.detachAppender(warnings)
        }
    }
}
