# Git Flow

This repository follows the [Git Flow](https://nvie.com/posts/a-successful-git-branching-model/) branching model, as the task requires (R31). Everything below uses plain `git` commands, so no `git-flow` extension is needed.

## Branches

| Branch | Created from | Merged into | Purpose |
|--------|--------------|-------------|---------|
| `master` | — | — | Released code only. Every commit on `master` is a tagged release (`v1.0.0`, …). Nobody commits to it directly. |
| `develop` | `master` | — | The integration branch. It always builds and its unit tests pass. The next release is prepared here. |
| `feature/phase-<n>-<slug>` | `develop` | `develop` (via PR) | One branch per phase in [`PLAN.md`](PLAN.md), with **one commit per task**. Example: `feature/phase-1-movies-list`. |
| `release/<version>` | `develop` | `master` **and** `develop` | Prepares a release: version bump, last fixes and docs. No new features. Example: `release/1.0.0`. |
| `hotfix/<version>` | `master` | `master` **and** `develop` | Urgent fix to a released version. Example: `hotfix/1.0.1`. |

### Naming rules
- Use the phase number from `PLAN.md` and a short slug in lowercase kebab-case: `feature/phase-2-movie-details`.
- Extras done after the required phases get their own branch: `feature/E4-shimmer`.
- Versions follow [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`). Tags are prefixed with `v`.

### Why one branch per phase with one commit per task
A phase (for example "Movies list end to end") is the smallest unit a reviewer can run and judge as a feature, so it gets one pull request. Inside it, every task from `PLAN.md` is **one commit** made once that task is done, so the history still shows the step-by-step build-up, and **every commit builds and passes its tests**.

## Workflow for one phase

```bash
# 1. Start the phase from an up-to-date develop
git switch develop
git pull origin develop
git switch -c feature/phase-1-movies-list

# 2. For each task: implement → build + tests → verified on a device → one commit
git add <files of the task>
git commit            # message format: see "Commit messages"
git push -u origin feature/phase-1-movies-list

# 3. When every task of the phase is done: open a pull request
#    feature/phase-1-movies-list → develop  (base branch: develop, not master)

# 4. Merge the PR with "Create a merge commit" (not squash or rebase), so each phase
#    stays visible as one unit and its task commits are kept.

# 5. Update the local develop and delete the finished branch
git switch develop
git pull origin develop
git branch -d feature/phase-1-movies-list
```

### Definition of done (for each task commit)
- [ ] `gradlew.bat assembleDebug` passes
- [ ] `gradlew.bat testDebugUnitTest` passes
- [ ] Verified on a device or emulator
- [ ] Feature doc written in `docs/features/NN-name.md`
- [ ] Task ticked in `docs/PLAN.md`, and the progress tracker updated

### Pull request checklist (for each phase)
- [ ] All tasks of the phase are ticked in `PLAN.md`
- [ ] `gradlew.bat connectedDebugAndroidTest` passes on an emulator
- [ ] The PR description lists the tasks, the requirement IDs covered, and how to test
- [ ] No secrets in the diff (`local.properties` stays ignored)

> **Phase 0 note:** Phase 0 (setup) was built before this per-phase rule was in place, without intermediate commits. Its tasks touched shared files (`app/build.gradle.kts`, `strings.xml`, `MainActivity.kt`) several times, so per-task commits could not be recreated in a state that builds. It was therefore committed as **one** commit on `feature/phase-0-setup` and merged through a pull request.

## Releases

```bash
git switch develop
git switch -c release/1.0.0
# bump versionName/versionCode in app/build.gradle.kts, final doc fixes
git commit -am "build(release): bump version to 1.0.0"

git switch master
git merge --no-ff release/1.0.0
git tag -a v1.0.0 -m "Release 1.0.0"

git switch develop
git merge --no-ff release/1.0.0

git push origin master develop --tags
git branch -d release/1.0.0
```

Hotfixes follow the same pattern, starting from `master` instead of `develop`.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <summary in imperative mood, max ~72 chars>

<optional body: why the change was made, not what the diff shows>

Refs: <task-id>, <requirement IDs>
```

| Type | Use for |
|------|---------|
| `feat` | New user-facing behaviour |
| `fix` | Bug fix |
| `refactor` | Code change with no behaviour change |
| `test` | Adding or changing tests only |
| `docs` | Documentation only |
| `build` | Gradle, dependencies, version catalog |
| `ci` | CI workflows |
| `chore` | Anything else (tooling, config) |

**Scopes:** `core`, `network`, `database`, `designsystem`, `navigation`, `movies`, `details`, `search`, `release`.

Example:

```
feat(movies): cache movie pages with RemoteMediator

Cached pages are shown immediately on start while page 1 refreshes
in the background, so the list never starts empty when offline.

Refs: T1.3, R7, R8, R9, R10
```

## Rules
- `local.properties` and any API key or token are **never** committed. They are already listed in `.gitignore`.
- Never force-push `master` or `develop`.
- Keep each feature branch to one task, so every merge into `develop` can be reviewed on its own.
