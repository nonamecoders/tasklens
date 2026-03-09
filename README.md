# TaskLens

An IntelliJ IDEA plugin that analyzes scheduled task execution flow in Spring-based Java applications.

<!-- Plugin description -->
**TaskLens** is an IntelliJ IDEA plugin that analyzes scheduled task execution flow in Spring-based Java applications.

It detects methods annotated with `@Scheduled`, traces calls into service and persistence layers, and displays the results in a Tool Window — with navigation to source code.

Designed for enterprise environments where static analysis must run locally without external AI services.
<!-- Plugin description end -->

## Features

- Detects methods annotated with `@Scheduled`
- Extracts schedule metadata (`cron`, `fixedDelay`, `fixedRate`)
- Resolves service calls from scheduled methods
- Resolves DAO / Mapper / Repository calls from service methods
- Displays results in an IntelliJ Tool Window
- Navigates to source code on click

## Analysis Flow

```
@Scheduled Method
      ↓
Service Method
      ↓
Mapper / DAO / Repository
```

## Detection Heuristics

**Service layer**
- Class name ends with `Service`
- OR annotated with `@Service`

**Persistence layer**
- Class name ends with `Mapper`, `Repository`, or `Dao`
- OR annotated with `@Repository`

## Usage

1. Open a Spring project in IntelliJ IDEA
2. Open the **TaskLens** tool window (bottom panel)
3. Click **Refresh** to scan for `@Scheduled` methods
4. Click any task or call to navigate to source code

## Tech Stack

- Kotlin
- IntelliJ Platform SDK
- PSI-based static analysis
- Gradle Kotlin DSL

## Building

```bash
./gradlew buildPlugin
```

## Running in IDE

```bash
./gradlew runIde
```