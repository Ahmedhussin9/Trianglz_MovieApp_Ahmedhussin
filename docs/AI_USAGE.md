# How AI was used in this project

This project was built with **Claude Code** (Anthropic's Claude in the terminal) as a pair programmer, directed and reviewed by the developer. This document explains the setup, the working loop, what was checked by hand, the mistakes that were caught, and the limits (R39).

## 1. Setup: rules before code

Before any code was written, the developer set the ground rules in [`CLAUDE.md`](../CLAUDE.md), a file the assistant reads at the start of every session:
- **Architecture:** Clean Architecture in three layers, MVI with one `UiState` / `UiAction` / effect channel, typed domain errors, injected dispatchers, package by feature.
- **Code rules:**
  - no hard-coded strings, with Arabic translations;
  - RTL-safe layouts;
  - theme colours only in the theme layer;
  - stateless composables with previews;
  - Timber logging conventions that never log secrets;
  - the API key only in `local.properties`;
  - no dead code.
- **Workflow:**
  - one task at a time;
  - explain the approach and list the files **before** writing code;
  - a task is done only when it builds, the unit tests pass **and the developer has checked it on a device**;
  - write a feature doc for every task;
  - never commit unless asked;
  - ask instead of guessing when a requirement is unclear.

Official Android guidance was used where it applied, through installed skills (for example `navigation-3` for Hilt ViewModels with navigation arguments, and `edge-to-edge` for insets and the keyboard). Library versions were looked up from Google's Maven repository instead of being written from memory.

## 2. Requirements and plan before implementation

1. **Requirements.** The task PDF was turned into [`docs/requirements.md`](requirements.md): every requirement numbered (R1–R54), plus the open questions (Q1–Q18). **The developer answered each question** before planning. Examples: details are cached too (Q5); TMDb content follows the app language (Q14); the bonus backend is out of scope (Q10).
2. **Plan.** [`docs/PLAN.md`](PLAN.md) was then drafted, with phases and tasks, a coverage table from requirement to task, a design direction, and a testing strategy. It was reviewed and adjusted before Phase 0 started.

## 3. The loop for every task

```
1. The assistant proposes the approach and the files to change   ──▶  the developer approves, adjusts or redirects
2. The assistant implements, builds, and runs the unit tests (and instrumented tests where relevant)
3. The developer checks it on the emulator and their own phone   ──▶  problems are reported back and fixed in the same task
4. The feature doc is written, PLAN.md ticked, a commit message proposed
5. The developer asks to commit; pushing happens only after the developer has verified the work
```

- **One task, one commit.** Each task is a single commit (Conventional Commits) carrying a `Co-Authored-By: Claude` trailer, so the AI's contribution is visible in the history. Each phase is one pull request.
- **Pushing was tightened after one early mistake.** In Phase 1 a task was pushed right after its automated checks, before the developer had reviewed it. After that, the rule became "commit locally per task; push only after the developer has verified it".

## 4. What the developer decided and caught

The developer did not only review the output; several of the project's decisions and fixes came directly from them:

| Where | What the developer did |
|---|---|
| Phase 0 | Asked for structured **logging** (Timber, token redacted), added as task T0.11 |
| T1.5 device check | Found that cards **shrank during fast scrolls** and **slid around** when the list reloaded; both were fixed. Asked for a **scroll-to-top** button |
| T2.1 | Decided to **raise minSdk to 26** so the details screen could show a full localized date |
| T2.6 | Pulled the **language and theme settings** forward from the "extras" list, so Arabic and dark mode could be checked on every later task |
| T2.6 / Phase 3 | Found on their own phone that **Arabic digits** behaved differently with a per-app language. This led to the digit fixes and a locale sync; the device verification is still logged as open in the [quality review](features/28-quality-review.md) |
| Phase 3 | Reported that the **keyboard didn't open on two emulators**. It was traced to the emulators reporting a hardware keyboard, not an app bug |
| T4.1 | Confirmed the large-text and landscape fixes on their phone |
| Phase 4 | Asked for deeper documentation (run-time walkthroughs, a testing guide, a design FAQ) |

## 5. How the output was verified

- **Automated tests:** 181 unit tests and 57 instrumented tests ([`TESTING.md`](TESTING.md)). No test calls the real API, so results are repeatable.
- **Device checks for every UI task**, by the developer and also by the assistant on an emulator:
  - the assistant installed builds, drove the app with `adb`, and **read the screenshots** to check layout, RTL, dark mode and error states;
  - it reported what it saw, including problems it had caused.
- **A dedicated error and accessibility review** (T4.1). It covered no internet, an invalid token, a forced timeout, 200 % text, landscape and RTL, and found four layout bugs, which were fixed and covered by a regression test.
- **Documentation checked against the code.** Every factual claim in the architecture docs, the decision records and the walkthroughs was checked against the source before it was written. This is how a wrong code comment was found and corrected (see §6).

## 6. Mistakes the AI made, and how they were caught

| Mistake | Caught by | Outcome |
|---|---|---|
| A test assumed fake paging data would show "Empty"; it showed the loading skeleton (no load states) | The test run itself | Explicit load states, later a shared test helper |
| The "Show less" button was missing when the synopsis started expanded | A UI test written for the screen | Fixed before the task was finished |
| A KDoc said the paging key table had "one row per movie" (it is one row per list) | Checking the docs against the code | Corrected |
| Search text would have been written to the logs through `toString()` | Review of the logging rules while implementing search | `QueryChanged.toString()` prints only the length |
| Running the build, the unit tests and the device tests at once froze the developer's PC | The developer | Heavy jobs now run one at a time with `--max-workers=2` |
| Missing imports and similar compile errors | The build | Fixed before reporting |

## 7. Handling secrets and privacy

- **The TMDb token** lives only in the git-ignored `local.properties`. It never appeared in prompts, logs or commits.
- **The wrong-token test (T4.1):** the file was backed up, the token was swapped for an invalid value without printing either, and the original was restored and verified byte for byte, then the backup was deleted.
- **No personal data** was sent to external services. The developer's email and details were not used anywhere outside git authorship.

## 8. Limits and responsibility

- **The assistant cannot judge some things on its own.** How the app feels on a real phone, behavior that depends on a particular device or Android version (such as per-app language on the developer's Android 13+ phone), and product decisions (what to build, what to defer) needed the developer.
- **Generated code is the developer's responsibility.** Every task was reviewed and checked on a device before it was accepted, and the reasoning behind every design choice is documented ([`adr/`](adr/README.md), [`features/`](features/)) so it can be explained and defended without the tool.
- **Known gaps are stated openly,** not hidden: the README's and [FAQ's](FAQ.md#known-limitations) limitations, and the open device item in the quality review.
