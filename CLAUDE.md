# Movie App

Android assessment task: a movie app. Deadline: [fill in date]

The full requirements live in `docs/requirements.md` (extracted from the task PDF). That file is the source of truth for scope. If anything in this file conflicts with it, the requirements win, and you tell me about the conflict.

## How we work
- `docs/PLAN.md` is the master plan and progress tracker. Read it at the start of every session to see what is done and what is next.
- Work on one task at a time. Don't start the next task until I confirm.
- Before writing code for a task, explain the approach in a few lines and list the files you will create or change.
- When I say "I'll write this part", give me only the steps and function signatures, then review my code instead of writing it.
- A task is done only when: the project builds (Gradle wrapper, `gradlew.bat` on Windows, `assembleDebug`), unit tests pass (`testDebugUnitTest`), and I have confirmed it works on a device or emulator.
- After each finished task: tick its checkbox in PLAN.md, write its feature doc, and suggest a commit message. Don't commit unless I ask.
- If a requirement is unclear, ask me. Never silently guess; list assumptions explicitly.
- Use the installed Android skills when they apply (e.g. navigation-3, edge-to-edge, jetpack-compose-performance, adaptive, testing-setup). Look up current stable library versions instead of relying on memory.

## Architecture
- Clean Architecture with three layers: data → domain → presentation. The domain layer is pure Kotlin with no Android imports.
- Flow for every feature: API service (DTOs) → remote data source → repository implementation (maps DTOs to domain models) → use case → ViewModel → Compose screen.
- Presentation: MVVM with unidirectional data flow (MVI style). Each screen has one immutable `UiState` data class exposed as `StateFlow`, user actions as a sealed `UiAction` handled by a single `onAction()` function, and one-time effects (navigation, snackbars) through a Channel-based Flow.
- Errors: the data layer converts exceptions into a domain error type. ViewModels never see HTTP or Retrofit exceptions. Every screen handles Loading, Content, Empty and Error (with retry).
- Coroutines: structured concurrency, `viewModelScope`, injected dispatchers (never hardcode `Dispatchers.IO`).
- Package by feature, with the layers inside each feature. Shared code goes in `core/`.

## Default tech stack (can be changed during plan review)
- Kotlin, Jetpack Compose, Material 3, Gradle version catalog
- Hilt for dependency injection
- Retrofit + OkHttp + kotlinx.serialization for networking
- Coil for images
- Navigation 3 for navigation
- Paging 3 if the movie lists are paginated
- Room only if the requirements need offline data or favorites
- DataStore for user preferences (theme, language)
- Tests: JUnit, MockK, Turbine, kotlinx-coroutines-test

## Code rules
- No hardcoded UI strings. Everything goes in `strings.xml`, with Arabic translations in `values-ar`.
- RTL-safe layouts: use start/end, never left/right. Directional icons must auto-mirror.
- Light and dark themes are defined in the theme layer only. No hardcoded colors inside composables.
- Composables are small and stateless where possible (state hoisting). The screen composable receives the state and `onAction`.
- Add previews for main composables (light, dark, RTL).
- The API key is stored in `local.properties` and read through BuildConfig. It is never committed. The README explains how to set it.
- Clear names, no dead or commented-out code. Comments explain "why", not "what".

## Documentation
- `docs/requirements.md`: every requirement from the task, numbered.
- `docs/PLAN.md`: the master plan with a checklist tracker.
- `docs/features/NN-feature-name.md`: one per finished task, with these sections:
    1. What was built
    2. Files created or changed, grouped by layer
    3. Data flow walkthrough (API → repository → use case → ViewModel → UI) with the key code snippets
    4. Decisions made, and alternatives considered
    5. How it is tested
    6. How to build it from scratch, step by step, in the right order
- Docs are written in clear, professional English for someone reviewing this repository. They only discuss this project.