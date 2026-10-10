# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [UNRELEASED]

## [1.0.0] 2026-10-09

### Added
- `StratastaxStyle`: ktlint's `ktlint_official` code style with stratastax rules, max line length 100, up to 10 format runs and optional license header.
- Rule `stratastax:parameter-annotation-per-line`: every annotation and parameter on its own line in a multi-line parameter list.
- Rule `stratastax:blank-line-between-annotated-parameters`: blank line between annotated parameters in a multi-line primary constructor.
- Rule `stratastax:single-line-lambda`: a lambda with one single-line expression stays on one line when it fits.
- Rule `stratastax:no-empty-line-comment`: removes empty `//` comments.
- Rule `stratastax:blank-line-after-closing-brace`: blank line after a statement ending in `}`.
- Rule `stratastax:single-line-condition`: `if`/`while` conditions on one line.
- Rule `stratastax:license-header`: every `.kt` file opens with the configured license header.
- Line break preservation (`preserveLineBreaks`), on by default.
- ktlint rule set (`META-INF/services`) for the ktlint CLI and IntelliJ plugin.
- Gradle plugin `dev.buijs.stratastax.style` with `stratastaxStyleApply` (runs on build) and `stratastaxStyleCheck` tasks, executed in an isolated worker process.
- CLI `stratafmt` with `check` and `format` commands.
