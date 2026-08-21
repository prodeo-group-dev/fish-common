# fish-common

Shared foundational value types for the FiSH ecosystem: `Money` (amount + currency, minor-unit rounded, currency-mismatch-safe arithmetic) and `ValidationResult` (success/failure/warnings, used instead of throwing for expected domain-rule violations).

## Why this repo exists

Before it existed, GL, SOP, POP, IM, and HR each carried their own independent, hand-ported copy of both types — every one of them "ported verbatim... the same way X already ported it." Confirmed (2026-08-21) to have drifted zero lines apart across five copies, but with nothing enforcing that would stay true — a bug fix or a currency-handling improvement had to be manually reapplied in every copy, with no guarantee any two stayed identical.

**Resolved deliberately in favour of both DRY and loose coupling, not one traded for the other**: this repo is consumed as a **git submodule**, not a published/versioned Maven artifact. Each consumer (GL/SOP/POP/IM/HR) adds `fish-common` as its own submodule and points a Gradle `sourceSet` at its `src/main/kotlin` directory directly — there is no runtime dependency, no package registry, no coordinated-release requirement across repos that are otherwise meant to stay independently buildable and independently deployable. Each repo still compiles fully standalone from a clone (`git submodule update --init` pulls this repo's source, same as any other submodule already in use across this ecosystem); upgrading to a newer `fish-common` commit is a deliberate, explicit, per-repo action (bumping the submodule pointer), exactly the same discipline already used for `FiSH`'s own `GL`/`SOP`/`POP`/`IM`/`HR` pointers.

## What's here

- `com.theprodeogroup.common.Money` — canonical, ported from `fish-fish-gl-engine`'s original (`domain.ledger.Money`).
- `com.theprodeogroup.common.ValidationResult` — canonical, ported from `fish-fish-gl-engine`'s original (`domain.common.ValidationResult`), including `warnings`/`withWarnings()`/`combine()`/`hasWarnings()`/`hasErrors()` — the full original surface, not the trimmed-down subset each downstream repo had independently re-derived.

No publishing plugin, no version-numbered releases — this is source to be included, not a binary to be depended on.

## Consuming this repo

1. `git submodule add https://github.com/prodeo-group-dev/fish-common.git common`
2. In the consumer's `build.gradle.kts`:
   ```kotlin
   sourceSets {
       main {
           kotlin.srcDir("common/src/main/kotlin")
       }
   }
   ```
3. Delete the consumer's own `Money`/`ValidationResult` copies and update imports to `com.theprodeogroup.common.Money`/`com.theprodeogroup.common.ValidationResult`.
