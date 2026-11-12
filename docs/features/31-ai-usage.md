# 31: AI usage document (T4.4)

## 1. What was built
[`docs/AI_USAGE.md`](../AI_USAGE.md) explains how Claude Code was used to build the app (R39):
- the ground rules in `CLAUDE.md`;
- requirements and planning before code;
- the per-task loop: approach → approval → implementation → tests → device check → doc → commit;
- the decisions and catches that came from the developer;
- how the output was verified;
- the mistakes the AI made and how they were caught;
- how secrets were handled;
- the limits.

The README's documentation table links to it.

## 2. Files created or changed

| Area | File | Change |
|------|------|--------|
| Docs | `docs/AI_USAGE.md` | New |
| Docs | `README.md` | Link to the AI usage document |
| Docs | `docs/PLAN.md` | T4.4 ticked |

## 3. Sources
Every example comes from the project's history:
- `CLAUDE.md`;
- the requirements' answered questions;
- the feature docs (for example the T1.5 device-check fixes and the T4.1 review);
- the commit messages;
- the workflow rules agreed during the project (push only after the developer's verification).

## 4. Decisions made, and alternatives considered
- **Name the tool.** The developer chose to name Claude Code. The commits already carry a `Co-Authored-By: Claude` trailer, so the document and the history agree.
- **Include the mistakes.** A list of what went wrong and how it was caught shows the verification worked better than a description of the process alone.
- **Credit specific human decisions.** The table of developer decisions and catches makes the division of work concrete instead of generic.

## 5. How it is tested
- Each example was checked against the feature docs, the commit history and the quality review.
- The wording uses neutral pronouns throughout.

## 6. How to build it from scratch
1. Collect the rules (`CLAUDE.md`), the process (`PLAN.md`) and the history (feature docs, commits).
2. Describe the loop, then list concrete developer decisions, verification steps and caught mistakes.
3. State how secrets were handled and what the tool cannot do.
4. Link it from the README.
