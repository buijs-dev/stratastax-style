# Stratastax Style
[![](https://img.shields.io/badge/Buijs-Software-blue)](https://buijs.dev/)
[![GitHub](https://img.shields.io/github/license/buijs-dev/stratastax-style?color=black)](https://github.com/buijs-dev/stratastax-style/blob/main/LICENSE)

The stratastax Kotlin style: ktlint's `ktlint_official` code style with a little extra.

| Rule                                                 | What it does                                                                                                                    |
|------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `stratastax:parameter-annotation-per-line`           | In a multi-line parameter list, every annotation and the parameter itself on a line of its own.                                 |
| `stratastax:blank-line-between-annotated-parameters` | In a multi-line primary constructor, one blank line between parameters when either is annotated.                                |
| `stratastax:single-line-lambda`                      | A lambda holding one single-line expression stays on one line when it fits, so a wrapped call chain reads one call per line.    |
| `stratastax:no-empty-line-comment`                   | Removes empty `//` comments, the old workaround to stop ktfmt from joining lines.                                               |
| `stratastax:blank-line-after-closing-brace`          | A statement ending in `}` - a block, a lambda however short - is followed by a blank line before the next statement.            |
| `stratastax:single-line-condition`                   | The condition of an `if`/`while` sits on one line. One that doesn't fit is reported for a hand to fix: extract it into a `val`. |

Four standard rules are off, each undoing or undone by a stratastax rule: `no-blank-line-in-list`,
`condition-wrapping`, and `kdoc` and `no-consecutive-comments` (both reject the `/** license */` header every stratastax file opens with). Max line length is 100; a function named in backticks (a test) doesn't count against it.

Formatting keeps every line break and blank line someone wrote; it only adds them (wrapping a
line that's too long, the blank lines above). Six more standard rules are off for that:
`function-expression-body`, `no-empty-first-line-in-method-block`,
`no-empty-first-line-in-class-body`, `no-blank-line-before-rbrace`,
`no-blank-lines-in-chained-method-calls` and `blank-line-between-when-conditions`, and
`single-line-lambda` only joins a lambda the formatter broke open itself. `preserveLineBreaks` off
(as for generated code) normalizes them instead.

ktlint's public API gives up after 3 format runs; deeply nested code (generated code in
particular) needs more, so `StratastaxStyle` formats with up to 10.

```kotlin
@PersistenceEntity(MatchColumns.TABLE)
internal data class Match(
    @SearchSort(tiebreaker = true)
    @SearchFilter
    @PersistenceColumn(MatchColumns.PUBLIC_ID)
    val id: MatchId,

    @SearchFilter
    @PersistenceJoinOne(MatchColumns.GAME_ID)
    val game: GameRef,
)

fun top(x: List<Int>) =
    x
        .filter { it > 1 }
        .map { it * 2 }
        .sortedDescending()
        .take(10)
        .joinToString(separator = ", ")
```

## Modules
- `style-rules` - the rules and `StratastaxStyle`, the one entry point the CLI, the Gradle plugin and
  the stratastax code generator share. Also a ktlint rule set (`META-INF/services`) for the ktlint
  CLI and IDE plugins.
- `style-gradle-plugin` - `dev.buijs.stratastax.style`.
- `style-cli` - `stratafmt`.

## Gradle

```kotlin
plugins {
    id("dev.buijs.stratastax.style")
}

stratastaxStyle {
    maxLineLength.set(100) // the default
    applyOnBuild.set(true) // the default
    preserveLineBreaks.set(true) // the default
    licenseHeader.set(file("LICENSE_HEADER.txt").readText()) // off by default
}
```

- `./gradlew stratastaxStyleApply` formats every Kotlin source set and the `*.gradle.kts` scripts,
  then fails on what is left - what only a hand can fix (a condition too long for one line, a
  wildcard import, a file not named after its class). `build` runs it before compiling.
- `./gradlew stratastaxStyleCheck` changes nothing and fails on anything not in style, for CI.

With `licenseHeader` set, every `.kt` file opens with that header: `stratastaxStyleApply` adds it
where it is missing, `stratastaxStyleCheck` reports it (`stratastax:license-header`). Build scripts
are left without.

Anything under the build directory, generated code included, is left alone. The rules run in an
isolated worker process: ktlint brings its own Kotlin compiler, which never reaches the build
classpath or the Gradle daemon.

## CLI

```shell
./gradlew :style-cli:fatJar
java -jar style-cli/build/libs/stratafmt.jar check src     # exit 1 on violations
java -jar style-cli/build/libs/stratafmt.jar format src
java -jar style-cli/build/libs/stratafmt.jar format --no-preserve-line-breaks src
java -jar style-cli/build/libs/stratafmt.jar format --license-header HEADER.txt src
```

`build/` and `.gradle/` directories are skipped. Handy as a pre-commit hook.

## IntelliJ
With the ktlint IntelliJ plugin, add the `style-rules` jar as an external rule set and use the
`ktlint_official` code style: format-on-save then formats like the build.

## Generated code
The stratastax code generator ([stratastax-codegen](https://github.com/buijs-dev/stratastax-codegen),
plugin `dev.buijs.stratastax.codegen`) formats its own output with this style;
`stratastaxCodegen { formatGeneratedSources.set(false) }` turns that off.

## Migrating from ktfmt/spotless
Replace spotless with the `dev.buijs.stratastax.style` plugin (its `licenseHeader` takes over
spotless' `licenseHeader`) and run
`stratastaxStyleApply` (or `build`) once: the empty `//` comments go, annotations stack, chains wrap one call per line.
What it still reports afterwards needs a hand: wildcard imports, lines too long to wrap
automatically (long string literals), a file not named after its single class.

## Related Stratastax projects
- [stratastax-codegen](https://github.com/buijs-dev/stratastax-codegen) - formats its generated sources with `style-rules`