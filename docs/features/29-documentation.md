# 29: Code and architecture documentation (T4.2)

## 1. What was built
The documentation that explains the codebase as a whole (R34, R35, R40, R46, R18):
- **[`docs/ARCHITECTURE.md`](../ARCHITECTURE.md)** covers:
  - the layers and the dependency rule (diagram);
  - the actual package structure;
  - how MVI names map to the code;
  - the data flow of the list (`RemoteMediator` sequence diagram), details, search and settings;
  - networking and the error path, navigation, the UI system and testing;
  - an index of the decision records.
- **[`docs/adr/`](../adr/README.md)**: eight architecture decision records, each with context, decision, alternatives and consequences:
  0001 MVI naming · 0002 `PagingData` outside the `UiState` · 0003 `RemoteMediator` cache and the finite details flow · 0004 the search certification-filter limitation · 0005 one Gradle module · 0006 package by feature (with a comparison table) · 0007 Navigation 3 · 0008 typed domain errors.
- **A KDoc pass:** a scan of every non-private top-level declaration in `app/src/main` found 27 without documentation. Each now has a short KDoc that states purpose or behavior rather than repeating the name: DAOs, the DI module, theme schemes, contract types, mappers, the settings use cases and `toFooterStatus`.

## 2. Files created or changed

| Area | File | Change |
|------|------|--------|
| Docs | `docs/ARCHITECTURE.md` | New |
| Docs | `docs/adr/README.md`, `0001`–`0008` | New |
| Code (comments only) | 27 declarations across `core/` and `feature/*` (DAOs, `DatabaseModule`, `Color.kt`, `Type.kt`, contracts, mappers, use cases, `ListStatus.kt`, `NightModeApplier.kt`) | KDoc added |
| Docs | `docs/PLAN.md` | T4.2 ticked |

## 3. How the documentation fits together

```
README.md (T4.3) ──▶ docs/ARCHITECTURE.md ──▶ docs/adr/NNNN-*.md        (why)
                                         └──▶ docs/features/NN-*.md     (what each task built, step by step)
docs/requirements.md ◀── docs/PLAN.md (coverage table: requirement → task)
```

## 4. Decisions made, and alternatives considered
- **ADRs as separate short files, not a section of the architecture doc.** Each decision can be linked, reviewed and later superseded on its own, which is the usual ADR practice. The architecture doc stays about "how it works".
- **One extra record (0008, typed domain errors)** beyond the seven planned. The error model shapes every layer and every screen, so it deserves its own record.
- **The package-by-feature comparison was written fresh.** The table mentioned in the plan was only discussed during T1.2 and never saved to the docs. ADR 0006 now holds a complete version.
- **KDoc only where it adds information.** Declarations whose purpose is already documented as a group (for example the mapper pairs or the contract files' header comment) got a single line that adds the missing detail, such as the `Unknown` fallback for stored statuses, not a restatement of the name.
- **Every factual claim was checked against the code before writing it**: the `remote_keys` table, the `IGNORE` conflict strategy, the token redaction in logs, `ErrorMapperTest` covering every status, and the domain layer's imports (only coroutines, `java.time`, `javax.inject` and Paging's pure-Kotlin types).

## 5. How it is tested
- `gradlew.bat assembleDebug` and `testDebugUnitTest` (181 tests) pass after the KDoc pass, which changed comments only.
- The Mermaid diagrams use GitHub's built-in renderer, the same as `PLAN.md`.

## 6. How to build it from scratch
1. List the real package tree and the domain layer's imports, so the doc describes the code as it is.
2. Write `ARCHITECTURE.md`: overview, layers and rules, packages, MVI, one section per data flow, errors, navigation, UI system, testing, and an ADR index.
3. Write one ADR per decision with context, decision, alternatives and consequences, plus an index README.
4. Scan for undocumented non-private declarations and add purpose-level KDoc.
5. Check each factual claim against the code, then build and run the tests.
