# Architecture decision records

Each record explains one decision that shapes the codebase: the situation, what was decided, what else was considered, and what follows from it. They are short on purpose. The overall picture is in [`ARCHITECTURE.md`](../ARCHITECTURE.md).

| # | Title | Status |
|---|---|---|
| [0001](0001-mvi-naming.md) | MVI with `UiState`, `UiAction` and `Effect` | Accepted |
| [0002](0002-pagingdata-outside-uistate.md) | Paged lists live next to the `UiState`, not inside it | Accepted |
| [0003](0003-remotemediator-cache.md) | Room with a `RemoteMediator` for the list; a finite cache-then-network flow for details | Accepted |
| [0004](0004-search-certification-filter.md) | Search sends the certification filter, which TMDb ignores there | Accepted |
| [0005](0005-single-module.md) | One Gradle module | Accepted |
| [0006](0006-package-by-feature.md) | Package by feature, with the layers inside each feature | Accepted |
| [0007](0007-navigation-3.md) | Navigation 3 | Accepted |
| [0008](0008-typed-domain-errors.md) | A closed `DomainError` type for every failure | Accepted |

**Format.** Each record has *Context*, *Decision*, *Alternatives considered* and *Consequences*. A record is never rewritten when a decision changes: a new record supersedes it and both stay.
