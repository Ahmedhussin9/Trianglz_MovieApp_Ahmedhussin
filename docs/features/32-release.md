# 32: Release and submission (T4.5)

## 1. What was built
Version **1.0.0**, released through Git Flow (R31, R42, R43, R47):
- **Phase 4 PR.** `feature/phase-4-quality` was merged into `develop` through PR #5, like every earlier phase.
- **Release branch.** `release/1.0.0` was created from `develop`. It changes `versionName` from "1.0" to "1.0.0" (`versionCode` stays 1 for the first release) and finalizes PLAN.md and this document.
- **Release.** The branch was merged into `master` with a merge commit, `master` was tagged `v1.0.0`, and the branch was merged back into `develop`. All of them were pushed.
- **GitHub release `v1.0.0`.** It has release notes and the **debug APK** (`MovieApp-v1.0.0.apk`), built with the owner's TMDb token, so reviewers can install the app without creating a TMDb account (requirements Q18).
- **Public repository.** The repository is public, with the complete source code and documentation.

## 2. Files created or changed

| Area | File | Change |
|------|------|--------|
| Build | `app/build.gradle.kts` | `versionName = "1.0.0"` |
| Docs | `docs/PLAN.md` | T4.5 ticked; the tracker shows the release |
| Docs | `docs/features/32-release.md` | This document |

## 3. Release flow

```
feature/phase-4-quality ──PR #5──▶ develop ──▶ release/1.0.0 (version bump, final docs, full test run)
                                                  ├──merge──▶ master ──tag──▶ v1.0.0 ──▶ GitHub release (+ APK)
                                                  └──merge──▶ develop
```

## 4. Decisions made, and alternatives considered
- **A debug APK with the token, plus the token sent privately (Q18).** Reviewers can run the app immediately, either by installing the APK or by building with the token. The token is extractable from the APK, which is acceptable for TMDb's read-only token because it is **regenerated after the review**. A production app would keep the token on a backend.
- **Debug, not a minified release build.** R8 is disabled (a known limitation), and a debug build is what was tested on devices throughout.
- **History scanned before publishing.** Before anything was pushed to `master`, all 31 commits on every branch were searched for JWT-like strings and filled-in `TMDB_ACCESS_TOKEN=` lines. None were found, and `local.properties` was never committed and is git-ignored.
- **The GitHub CLI** was installed and authenticated as the repository owner, so the PR, the merge and the release were done from the same session as the code.

## 5. How it is tested
- On `release/1.0.0`, before tagging: `assembleDebug`, `testDebugUnitTest` (181 tests) and `connectedDebugAndroidTest` (57 tests) pass.
- **What the first full run on this branch hit:**
  - **Emulator crash:** the emulator had been running for over three hours under heavy load and crashed (`INSTRUMENTATION_ABORTED: System has crashed`) after 39 tests. The `MoviesScreenTest` case running at that moment failed with an empty message, as collateral damage.
  - **A real flaky test:** `MoviesRepositoryImplTest.onlineLaunchWithACacheEndsWithTheFreshMovies`. After a successful refresh, Room invalidates its `PagingSource` on a background thread, and `asSnapshot()` could return in that gap with the cached row. The test now waits, in real time with a 5 s limit, for the fresh rows, as a collecting UI does. It was a race in the test, not in the app.
- **After the fix:** the emulator was rebooted, and the full suite passed 57/57.
- The APK attached to the release is the one built from the tagged code.

## 6. Owner's follow-up
1. Send the repository link to the reviewers, and the TMDb token **privately** (never in the repository).
2. **Regenerate the TMDb token after the review.** The old token is inside the published APK.
