# 0001: MVI with `UiState`, `UiAction` and `Effect`

**Status:** Accepted (Phase 1, T1.4)

## Context
The task requires the MVI pattern (R17). The project's working rules (CLAUDE.md) describe the same idea as "MVVM with unidirectional data flow": one immutable state, user actions through one function, one-time effects through a channel. Reviewers may look for MVI by name, so the mapping between the two vocabularies has to be explicit and consistent on every screen.

## Decision
Every screen uses the same three types, defined together in a `…Contract.kt` file:

| MVI | Type | Delivered as |
|---|---|---|
| Model | `XxxUiState`, an immutable data class | `StateFlow<XxxUiState>` |
| Intent | `XxxUiAction`, a sealed interface | `onAction(action)`, the single entry point |
| Side effect | `XxxEffect`, a sealed interface | `Flow` from a `Channel` (each event delivered once) |

The ViewModel is the reducer: `onAction` updates the state or sends an effect. Screens are split into a stateful `…Route` and a stateless `…Screen(state, onAction)`.

## Alternatives considered
- **"Intent" as the type name.** It is the textbook MVI term, but it clashes with `android.content.Intent` in imports and reviews. `UiAction` says the same thing without the ambiguity.
- **An MVI library (Orbit, MVIKotlin).** It adds a dependency and a learning step for three small screens, while a sealed type, a `StateFlow` and a `Channel` express the same contract in plain Kotlin.
- **Effects as state** (for example `snackbarMessage: String?` plus a "consumed" call). Rotation could replay them, and every effect needs clean-up code. A channel delivers each event exactly once.

## Consequences
- **One shape for every screen.** All four screens (Movies, Details, Settings, plus search inside Movies) look the same, so each is quick to read and test.
- **Testable.** ViewModel tests send actions and assert states and effects with Turbine; UI tests render the stateless screen with a fixed state and record actions.
- **One exception, by design:** Retry on paged lists calls `LazyPagingItems.retry()` directly instead of going through `onAction`, because Paging owns that load state ([ADR 0002](0002-pagingdata-outside-uistate.md)).
