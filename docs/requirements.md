# Requirements

Source: *Movie App Task.pdf* (4 pages). Every point in the PDF is listed below as one requirement, numbered R1, R2, and so on, and is never merged with another. The **Source** column says which section of the PDF it comes from. When the *Evaluation Criteria* section repeats a requirement listed elsewhere, that is noted on the same row ("Also evaluated"), so each point is counted once and none is lost.

This file is the source of truth for scope (see `CLAUDE.md`).

## Functional

| ID | Requirement | Source |
|----|-------------|--------|
| R1 | Fetch a list of movies from a remote API. | Features › 1. Preview Movies List |
| R2 | Display the list on the home screen with basic info: **title**, **poster image** and **release year**. | Features › 1 |
| R3 | Implement pagination (for example Paging 3 or a custom scroll listener) to load data sequentially, page by page. | Features › 1 |
| R4 | Properly handle **Loading** and **Error** states at the **bottom of the list** during pagination. | Features › 1 |
| R5 | Let users tap a movie item to view its details. | Features › 2. Show Movie Detail |
| R6 | Display detailed information about the selected movie, including **synopsis**, **rating**, **genre**, "etc.". | Features › 2 |
| R7 | Implement caching to store retrieved movie data locally. | Features › 3. Caching Flow |
| R8 | When the app starts, check whether cached data is available. | Features › 3 |
| R9 | If cached data exists, display it while the latest data is fetched in the background. | Features › 3 |
| R10 | Update the UI when the latest data is retrieved. | Features › 3 |
| R11 | Implement a search bar **on the movie list screen**. | Features › 4. Search Feature |
| R12 | Let users search for movies **by title**. | Features › 4 |
| R13 | Display search results dynamically as the user types. | Features › 4 · Also evaluated: "search feature with real-time updates" |

## Technical

| ID | Requirement | Source |
|----|-------------|--------|
| R14 | Use Kotlin for development. | Requirements |
| R15 | Use coroutines and Flow for asynchronous programming. | Requirements · Also evaluated: "effective use of coroutines and Flow" |
| R16 | Implement Clean Architecture. | Requirements, Instructions · Also evaluated: "adherence to Clean Architecture principles" |
| R17 | Use the MVI pattern. | Task Description, Requirements, Instructions · Also evaluated: "adherence to … the MVI pattern" |
| R18 | Keep the codebase modular and scalable. | Requirements |
| R19 | Use Jetpack Compose for UI development. | Requirements |
| R20 | Use a dependency injection framework (for example Hilt) to manage dependencies and separate concerns. | Requirements ("must") |
| R21 | Implement a robust error-handling layer that maps Network/HTTP exceptions to clear **Domain Errors**, so the UI can handle states cleanly without complex logic. | Requirements ("must") · Instructions: "ensure proper error handling" |
| R22 | Ensure proper data validation. | Instructions |
| R23 | Use the TMDb (The Movie Database) API to fetch movie data. | API Resource · Also evaluated: "usage of TMDb API" |
| R24 | Every TMDb request **must** include `include_adult=false`. | API Resource › Required API Configuration |
| R25 | Every TMDb request **must** include `certification_country=US`. | API Resource › Required API Configuration |
| R26 | Every TMDb request **must** include `certification.lte=PG-13` (only G, PG and PG-13). | API Resource › Required API Configuration |
| R27 | Unit tests for the **Domain** layer. | Instructions · Also evaluated: "unit tests for domain/data layers" |
| R28 | Unit tests for the **Data** layer. | Instructions · Also evaluated: "unit tests for domain/data layers" |
| R29 | Unit tests for **ViewModels**. | Instructions |
| R30 | Instrumentation/UI tests. The PDF sentence is cut off ("Instrumentation/UI tests are strictly"), see Q1. | Instructions · Also evaluated: "Instrumentation/UI tests" |
| R31 | Use the Git Flow workflow (for example feature branches). | Instructions · Also evaluated: "compliance with Git Flow principles" |

## Non-functional

| ID | Requirement | Source |
|----|-------------|--------|
| R32 | Follow the **Material 3** design guidelines, keeping the UI consistent, clear and responsive. | Instructions › UI Quality · Evaluation › UI Quality |
| R33 | Exceptional visual polish and delightful interactions that go **beyond default Material components** while staying usable. | Instructions › UI Quality · Evaluation › UI Quality |
| R34 | Document the code. | Instructions |
| R35 | Document architectural decisions. | Instructions |
| R36 | Consider the scalability and maintainability of the solution. | Instructions |
| R37 | Code quality, readability and maintainability. | Evaluation Criteria |
| R38 | Caching must improve the app's performance. | Task Description · Also evaluated: "integration of caching mechanism for improved performance" |
| R39 | Follow best practices in using and applying AI. | Evaluation Criteria |
| R40 | Documentation must be clear and complete. | Evaluation Criteria |
| R41 | Keep the app's scope "simple": a movie list, details and search. | Task Description |

## Deliverables / Submission

| ID | Requirement | Source |
|----|-------------|--------|
| R42 | Submit the solution as a **public GitHub repository**. | Submission |
| R43 | Include the complete source code. | Submission |
| R44 | Include instructions for running locally (README). | Submission |
| R45 | Include API key and configuration instructions. | Submission |
| R46 | Include documentation of architectural decisions. This is the deliverable form of R35. | Submission |
| R47 | Share the repository link for review once it is complete. | Submission |

## Bonus (optional): out of scope by decision

The user decided on 2026-11-10 not to build the backend bonus (see Q10). The points are listed here so that every part of the PDF is accounted for.

| ID | Requirement | Source | Status |
|----|-------------|--------|--------|
| R48 | Build a custom backend API (Ruby on Rails, Python, C#, …) instead of calling TMDb directly, serving the movie list. | Bonus: Custom Full-Stack API | Out of scope (decided) |
| R49 | The backend handles pagination. | Bonus | Out of scope (decided) |
| R50 | The backend manages the search functionality. | Bonus | Out of scope (decided) |
| R51 | The backend either fetches and caches data from TMDb internally, or uses its own database filled with dummy data. | Bonus | Out of scope (decided) |
| R52 | The backend provides an "Edit Movie" endpoint (for example `PATCH /api/movies/{id}`), called from Postman. The mobile app does not handle editing. | Bonus | Out of scope (decided) |
| R53 | Calling the edit endpoint (from Postman or Swagger UI) makes the backend send an FCM push notification to the app, testing its real-time cache update end to end. | Bonus | Out of scope (decided) |
| R54 | Extra bonus: an admin portal for editing movies only. | Bonus | Out of scope (decided) |

## Open questions

### Resolved

| # | Question | Decision |
|---|----------|----------|
| Q9 | Git Flow: should `develop` be created now, and who creates branches and commits? | `develop` was created from `master` on 2026-11-10 with the user's permission. Commits still happen only when the user asks (CLAUDE.md). |
| Q10 | Is the bonus backend (R48–R54) in scope? | No. The whole backend flow is skipped. |
| Q14 | TMDb content language: always `en-US`, or follow the app locale? | Follow the app locale (`en-US` / `ar-SA`). When the locale changes, the content is fetched again in the new language. |
| Q17 | Is `minSdk 24` acceptable? | Yes, it stays at 24. |
| Q18 | How do reviewers run the app without our token? | All four options were chosen on 2026-11-10. **(1)** Every automated test uses MockWebServer or fakes, so no token is needed to run them. **(2)** The README explains how to get a free token. **(3)** A debug APK built with our token is attached to the GitHub release `v1.0.0`, and the token is regenerated after the review. **(4)** The token is also sent privately with the submission, never in the repo. Covered by T4.3 and T4.5. |

### Assumptions to validate

The user chose to resolve these through implementation and testing. Each assumption below is what the plan follows until something proves it wrong.

| # | Question | Working assumption |
|---|----------|--------------------|
| Q1 | The sentence "Instrumentation/UI tests are strictly …" is cut off. | Instrumentation/UI tests are **strictly required**. |
| Q2 | TMDb applies `certification_country` / `certification.lte` only on `/discover/movie`. `/search/movie` and `/movie/{id}` ignore them, so search results can only be filtered for adult content. | All three parameters (R24–R26) are added to **every** request by one OkHttp interceptor, as the PDF requires. The list uses `/discover/movie`, where the certification filter applies. The search limitation is documented in an ADR. |
| Q3 | Which movie list should the home screen show? | `/discover/movie?sort_by=popularity.desc` (popular movies, filtered). |
| Q4 | Should search results be cached or work offline? | No. Search is network only and shows a clear offline error with Retry. |
| Q5 | Should movie details be cached too? | Yes. Details are cached in Room: cached data is shown first, then refreshed from the network. |
| Q6 | "Modular": one module packaged by feature, or Gradle multi-module? | One `:app` module, packaged by feature, with strict layer boundaries (CLAUDE.md). A multi-module split is listed as extra E16. |
| Q7 | MVI naming: CLAUDE.md's `UiState` / `UiAction` / Effect, or Intent and a Reducer? | Keep the CLAUDE.md names and document how they map to MVI (Intent = `UiAction`, Model = `UiState`, side effects = Effect, reduction inside `onAction`). |
| Q8 | R33 ("beyond default Material components") may justify making shimmer and the shared-element transition required instead of extras. | They stay as extras. The required work already includes custom cards and nine specific animations. This will be reviewed again after Phase 3. |
| Q11 | TMDb authentication: v4 Read Access Token (Bearer header) or v3 `api_key` query parameter? | v4 Read Access Token sent as a Bearer header. |
| Q12 | R39 (AI best practices): how should this be shown? | A `docs/AI_USAGE.md` describing how AI was used: CLAUDE.md rules, planning, task-by-task review, and human verification on a device. |
| Q13 | The deadline is still a placeholder in CLAUDE.md. | Unknown. The phases are ordered so that a complete required scope exists before any extras are started. |
| Q15 | Cache policy: expiry time or size limit? | Always refresh in the background on app start (R9). No expiry time. A refresh replaces the cached list pages. |
| Q16 | What counts as "data validation" (R22)? | **Input:** the search query is trimmed, a blank query means idle, and the length is capped. **Responses:** movies with a missing ID or title are dropped. Missing posters show a placeholder. Blank or invalid dates mean no year is shown. Ratings are limited to 0–10. |
