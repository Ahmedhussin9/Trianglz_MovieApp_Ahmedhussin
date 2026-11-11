# 30: README (T4.3)

## 1. What was built
The repository's front page, `README.md` (R44, R45, R40), replacing the draft from T0.4. It covers:
- **What the app does:** a short overview and a screenshot gallery (list, details, search, Arabic, dark theme, offline).
- **Features:** the full list.
- **How it is built:** an architecture summary with a table linking every document, and the tech stack with exact versions from the version catalog.
- **Getting started:** requirements, getting a TMDb token, the three ways to configure it, and running the app.
- **Day to day:** the tests (with counts), the Logcat filters, and the project structure.
- **Honesty and workflow:** known limitations and the Git workflow.

## 2. Files created or changed

| Area | File | Change |
|------|------|--------|
| Docs | `README.md` | Rewritten |
| Docs | `docs/screenshots/*.jpg` | New: six screenshots from the device checks, 360 px wide JPEG, about 300 KB in total |
| Docs | `docs/PLAN.md` | T4.3 ticked |

## 3. Structure of the README
1. Overview and screenshot gallery
2. Features
3. Architecture summary and documentation index
4. Tech stack (versions from `gradle/libs.versions.toml`)
5. Getting started: token, configuration, run
6. Tests
7. Debugging with Logcat
8. Project structure
9. Known limitations
10. Git workflow

## 4. Decisions made, and alternatives considered
- **Screenshots included (extra E9).** They were taken during the device checks anyway, and the gallery shows the RTL, dark and offline behavior faster than text. They are resized to 360 px JPEGs, so the repository stays small.
- **The README summarizes and links; the details live in `docs/`.** A reviewer gets the essentials in one page and can follow links for depth, without the README duplicating `ARCHITECTURE.md`.
- **Versions are copied from the version catalog,** not written from memory, so the table matches the build.
- **Known limitations are stated on the front page.** A reviewer sees them as deliberate trade-offs, not discoveries.
- **One wording fix from the draft:** it said instrumented tests "run against a Hilt test graph". Only the navigation test does. The others use in-memory databases and fixed paging data, and `AppGraphTest` checks the production graph.

## 5. How it is tested
- Every command, path, filter and version in the README was checked against the build file, the version catalog and the docs.
- The image paths are relative to the repository root, so they render on GitHub.

## 6. How to build it from scratch
1. List the features from the requirements and the feature docs.
2. Copy the versions from `gradle/libs.versions.toml`.
3. Keep and extend the configuration section from T0.4.
4. Take screenshots on a device, resize them (360 px JPEG), and put them in `docs/screenshots/`.
5. Link every document in `docs/`, and list the known limitations from `docs/FAQ.md`.
