# 01: Git Flow conventions (T0.1)

## 1. What was built
The version-control conventions for the project, as the task's Git Flow requirement asks (R31):
- a long-lived `develop` branch created from `master`;
- naming rules for feature, release and hotfix branches, linked to the task IDs in `PLAN.md`;
- a commit message format (Conventional Commits with task and requirement references);
- a per-task definition of done that must pass before a feature is merged.

## 2. Files created or changed

| Layer | File | Change |
|-------|------|--------|
| Docs | `docs/GIT_FLOW.md` | New: the branching model, workflow, release steps and commit format. |
| Docs | `docs/features/01-git-flow.md` | New: this document. |
| Git | `develop` branch | Created from `master` (`b577c84`). |

No source or Gradle files were changed.

## 3. Data flow walkthrough
Not applicable. This task contains no application code.

## 4. Decisions made, and alternatives considered
- **Plain `git` instead of the `git-flow` extension.** The extension isn't installed on the development machine, and plain commands show exactly what happens. The branching model is the same.
- **`--no-ff` merges (or merge-commit PRs).** Each feature stays visible as one unit in the history, which makes it easy for reviewers to check Git Flow compliance. Squash merges were rejected because they hide the feature branches.
- **One branch per phase, one commit per task** (revised after Phase 0). The first version used one branch per task. During Phase 0 the work wasn't committed per task, so the rule was changed to what reviewers need: one pull request per runnable feature (a phase), with one commit per `PLAN.md` task inside it, and every commit building. Phase 0 itself went in as a single commit, as explained in `GIT_FLOW.md`.
- **Conventional Commits with a `Refs:` footer.** Each commit can be traced back to a task and its requirements.

## 5. How it is tested
- `git branch` lists `master` and `develop`.
- The rules are checked again at each merge, using the definition-of-done checklist in `GIT_FLOW.md`.

## 6. How to build it from scratch
1. From the initial commit on `master`, run `git switch -c develop`.
2. Push it: `git push -u origin develop`.
3. Write `docs/GIT_FLOW.md` with the branch table, naming rules, task workflow, release steps and commit format.
4. Start each phase with `git switch -c feature/phase-<n>-<slug>` from `develop`. Commit once per task, and open one pull request into `develop` per phase.
