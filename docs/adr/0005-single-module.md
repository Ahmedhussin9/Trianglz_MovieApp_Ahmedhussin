# 0005: One Gradle module

**Status:** Accepted (planning, Q6; extra E16 kept for later)

## Context
The task asks for a "modular and scalable" codebase (R18). On Android, "modular" can mean separate Gradle modules (`:core:network`, `:feature:movies`, …) or clear modular boundaries inside one module. The app has three features and a small shared core, and it is built under a deadline.

## Decision
Keep **one application module**, with the boundaries expressed in code:
- **Package by feature** ([ADR 0006](0006-package-by-feature.md)), with the three layers inside each feature.
- **A pure-Kotlin domain layer**, checked as part of every task (no Android imports in `domain/`).
- **One Hilt module per feature**, binding its repository interfaces, so a feature's wiring sits with the feature.
- **Shared code only in `core/`**, and features do not import each other's data or presentation code.

Splitting into Gradle modules is recorded as extra **E16**, to do if the app grows.

## Alternatives considered
**Multi-module now (`:core:*`, `:feature:*`):**
- *What it gives:* the compiler enforces the layer rules, and builds can be parallel and incremental per module.
- *What it costs:* a build-logic setup (convention plugins), a module per layer or feature, wiring Hilt and KSP across modules, and slower progress on the actual features. For three features, the build-time benefit is negligible.

## Consequences
- **Boundaries rely on convention plus review, not the compiler.** CLAUDE.md rules and the per-task feature docs make them visible.
- **Splitting later is mechanical.** Each `feature/<name>/` folder already has its own data, domain, presentation and DI, and `core/` is self-contained, so E16 mostly means moving folders into modules and adding dependencies.
