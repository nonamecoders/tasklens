# TaskLens

TaskLens is an IntelliJ IDEA plugin that analyzes scheduled task execution flow
in Spring-based Java applications.

The plugin helps developers understand how scheduled tasks interact with
services and persistence layers.

Primary analysis path:

@Scheduled Task
↓
Service Method
↓
Mapper / DAO / Repository
↓
MyBatis or JPA

TaskLens is designed primarily for enterprise environments where:

- source code must remain inside internal networks
- external AI services cannot be used
- static analysis must be deterministic and explainable

---

## Critical Rules

These rules override other suggestions:

1. Keep the MVP scope narrow.
2. Prefer deterministic static analysis over heuristics.
3. Do NOT integrate external AI services.
4. All analysis must run locally.
5. Avoid large architectural refactors.
6. Implement features incrementally.

---

## Main Goal

Build an IntelliJ plugin MVP that:

1. Scans Java source code.
2. Detects methods annotated with `@Scheduled`.
3. Extracts schedule metadata (`cron`, `fixedDelay`, `fixedRate`).
4. Resolves service calls from scheduled methods.
5. Resolves DAO / Mapper / Repository calls from service methods.
6. Displays results inside an IntelliJ Tool Window.
7. Allows navigation to source code.

---

## MVP Scope

### Included

- Java PSI static analysis
- detection of `@Scheduled` methods
- schedule metadata extraction
- one-level service call tracing
- one-level persistence call tracing
- Tool Window UI
- manual refresh

### Excluded

- AI integrations
- Kotlin/UAST analysis for target code
- full call graph resolution
- background automatic scanning
- Spring Batch specific analysis
- MyBatis XML SQL parsing
- architecture diagram generation
- automatic fixes

---

## Tech Stack

TaskLens is implemented using:

- Kotlin
- IntelliJ Platform SDK
- Gradle Kotlin DSL
- PSI-based static analysis
- Swing Tool Window UI

---

## Detection Heuristics

### Service detection

A class may be treated as a service if:

- class name ends with `Service`
- OR annotated with `@Service`

### Persistence detection

A class may be treated as persistence layer if:

- class name ends with `Mapper`
- class name ends with `Repository`
- class name ends with `Dao`
- OR annotated with `@Repository`

---

## Implementation Order

Development should follow this order:

1. Plugin loads successfully
2. Tool Window appears
3. Detect `@Scheduled` methods
4. Display tasks in Tool Window
5. Extract schedule values
6. Resolve service calls
7. Resolve DAO / repository calls
8. Add navigation support

---

## Definition of Done (MVP)

The MVP is complete when:

- TaskLens runs in IntelliJ
- Tool Window opens successfully
- scheduled tasks are detected
- schedule metadata is displayed
- service calls are shown
- DAO / repository calls are shown
- navigation to source code works

---

## Performance Guidelines

To avoid slowing down IntelliJ:

- avoid automatic full-project scans
- prefer manual refresh
- keep analysis shallow
- avoid heavy work on UI thread

---

## Non Goals

The following are intentionally out of scope:

- AI-based analysis
- cloud code analysis
- full architecture graph generation
- deep call chain resolution
- SQL query inference

---

## Contributor Guidance

Prefer simple solutions for MVP.

If analysis certainty is low:
return partial but reliable results instead of speculative results.