Analyze failing tests and suggest fixes.

## Steps

1. Run `./gradlew check 2>&1` and capture the output.
2. If all tests pass (BUILD SUCCESSFUL), output: "All tests pass." and stop.
3. Parse the failure log to identify:
   - Which test classes/methods failed
   - The exception type and message
   - The stack trace location (file:line)
4. For each failure, read the relevant source file at the indicated line.
5. Output a structured report:

```
=== Test Failure Analysis ===

Failure 1: <TestClass.methodName>
  Error   : <ExceptionType: message>
  Location: <file>:<line>
  Analysis: <root cause in one sentence>
  Fix     : <concrete suggestion — what to change and where>

Failure 2: ...
```

6. If the failure is a compilation error rather than a runtime test failure, say so explicitly and show the compiler error lines.

## Notes

- Do not run `./gradlew check` more than once.
- Focus on the most actionable fix — prefer pinpointing the exact line over broad suggestions.
- Do not modify source files; only analyze and suggest.
