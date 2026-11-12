# 0006: Package by feature, with the layers inside each feature

**Status:** Accepted (CLAUDE.md; confirmed in T1.2)

## Context
Clean Architecture (R16) needs three layers, and R18 asks for a modular, scalable codebase. The packages can be organised first by layer (`data/movies`, `domain/movies`, …) or first by feature (`movies/data`, `movies/domain`, …). Both keep the same dependency rule.

## Decision
**Package by feature**, with the layers inside each feature and shared code in `core/`:

```
feature/movies/{data, domain, presentation, di}
feature/details/{data, domain, presentation, di}
feature/settings/{data, domain, presentation, di}
core/{network, database, designsystem, navigation, domain, ui, common, di}
```

## Alternatives considered

| | Package by feature (chosen) | Package by layer |
|---|---|---|
| Where a change lives | Mostly in one folder (`feature/details/…`) | Spread across `data/`, `domain/`, `presentation/` |
| Seeing what the app does | The top level lists the features | The top level lists technical layers |
| Feature boundaries | Visible: one feature's internals are not imported by another | Blurred: everything of one layer sits together |
| Moving to Gradle modules (E16) | Each feature folder becomes a module | Needs a reshuffle first |
| Layer rule (data → domain ← presentation) | Same | Same |
| Shared code | `core/`, explicit | Tends to grow inside each layer |

## Consequences
- **A reviewer can follow one feature end to end in one folder**, matching the per-task feature docs.
- **Code that two features need goes to `core/`, not across features.** For example, the TMDb value validation moved to `core/network/TmdbValues.kt` in T2.2 when details needed the list's rules.
- **The structure is ready for a multi-module split** ([ADR 0005](0005-single-module.md)).
