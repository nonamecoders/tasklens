# TaskLens Architecture

This document describes the architecture of the TaskLens IntelliJ Plugin.

It ensures contributors and AI tools understand how the plugin works and
avoid breaking architectural boundaries.

---

## System Overview

TaskLens analyzes Spring-based Java applications to discover execution flow
between scheduled tasks and persistence layers.

Execution flow:

@Scheduled Task
↓
Service Method
↓
Mapper / DAO / Repository
↓
MyBatis / JPA

TaskLens performs static analysis using IntelliJ PSI APIs.

Results are displayed inside a Tool Window within the IDE.

---

## High Level Architecture

ToolWindow (UI)
↓
ProjectService
↓
TaskFlowAnalyzer
↓
PSI Scanners

---

## Core Components

### Tool Window Layer

Responsible for displaying results.

Classes:

toolwindow/
- TaskFlowToolWindowFactory
- TaskFlowPanel

Responsibilities:

- render task flow tree
- trigger refresh
- navigate to source

Rule:

UI must NOT perform PSI scanning directly.

---

### Service Layer

service/
- TaskFlowProjectService

Responsibilities:

- store analysis results
- trigger analyzer execution
- provide data to UI

---

### Analysis Layer

scan/
- TaskFlowAnalyzer

Responsibilities:

- coordinate scanning pipeline
- assemble results

Flow:

ScheduledMethodScanner
↓
ServiceCallResolver
↓
DaoCallResolver

---

### PSI Scanning Layer

scan/
- ScheduledMethodScanner
- ServiceCallResolver
- DaoCallResolver

Responsibilities:

ScheduledMethodScanner
- detect `@Scheduled` methods

ServiceCallResolver
- resolve service method calls

DaoCallResolver
- resolve persistence layer calls

---

## Data Model

Analysis results are represented with simple data classes.

model/
- ScheduledTaskInfo
- ServiceCallInfo
- DaoCallInfo

These classes represent the discovered task execution flow.

---

## Detection Strategy

### Service

Detected when:

- class name ends with Service
- OR annotated with `@Service`

### Persistence Layer

Detected when:

- class name ends with Mapper
- Repository
- Dao
- OR annotated with `@Repository`

---

## Analysis Depth

TaskLens intentionally limits analysis depth:

@Scheduled
↓
Service
↓
DAO / Mapper / Repository

Full call graph tracing is intentionally avoided for performance reasons.

---

## Performance Strategy

To avoid IDE slowdowns:

- use manual refresh
- cache analysis results
- avoid repeated full-project scans
- keep PSI traversal shallow

---

## Error Handling

The plugin must gracefully handle:

- unresolved references
- incomplete code
- missing dependencies

Failures should not crash the plugin.

---

## Future Extensions

Potential future improvements:

- MyBatis XML mapping
- inspection rules for architecture violations
- cron validation
- transaction safety detection
- Markdown documentation export
- Kotlin support via UAST

---

## Architecture Summary

TaskLens uses a layered architecture:

ToolWindow (UI)
↓
ProjectService
↓
Analyzer
↓
PSI Scanners

This design keeps analysis logic isolated from UI and ensures maintainability.