# TaskLens

An IntelliJ IDEA plugin that analyzes scheduled task execution flow in Spring-based Java applications.

<!-- Plugin description -->
**TaskLens** is an IntelliJ IDEA plugin that analyzes scheduled task execution flow in Spring-based Java applications.

It detects methods annotated with `@Scheduled`, traces calls into service and persistence layers, and displays the results in a Tool Window — with navigation to source code.

Designed for enterprise environments where static analysis must run locally without external AI services.
<!-- Plugin description end -->

## Features

- Detects methods annotated with `@Scheduled` using `AnnotatedMembersSearch`
- Extracts schedule metadata (`cron`, `fixedDelay`, `fixedRate`)
- Resolves service calls from scheduled methods
  - Supports `ServiceImpl` naming pattern in addition to `Service`
  - Traces into same-class delegate methods for DAO detection
  - Follows service-to-service calls
- Resolves DAO / Mapper / Repository calls from service methods
  - Resolves MyBatis XML SQL statements and navigates to XML
- Displays results in an IntelliJ Tool Window
  - Tree grouped by class, then method
  - `@Scheduled` methods sorted by file path and source offset
- Navigates to source code on click (Cmd+B / Go To Declaration)
  - Java method navigation
  - MyBatis XML `id` → Java mapper method navigation

## Analysis Flow

```
@Scheduled Method
      ↓
Service Method
      ↓
Mapper / DAO / Repository
      ↓
MyBatis XML SQL (if applicable)
```

## Detection Heuristics

**Service layer**
- Class name ends with `Service` or `ServiceImpl`
- OR annotated with `@Service`

**Persistence layer**
- Class name ends with `Mapper`, `Repository`, or `Dao`
- OR annotated with `@Repository`

## Tree Structure

```
[ClassName]
└── methodName() — cron: 0 * * * * *
    └── ServiceClass.serviceMethod()
        └── [call site] MapperClass.selectData()
            └── MyBatisStatement (XML)
```

## Usage

1. Open a Spring project in IntelliJ IDEA
2. Open the **TaskLens** tool window (bottom panel)
3. Click **Refresh** to scan for `@Scheduled` methods
4. Click any node to navigate to source code
5. Press **Cmd+B** on a MyBatis XML `id` to jump to the Java mapper method

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