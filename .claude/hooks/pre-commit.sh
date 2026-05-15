#!/usr/bin/env bash
# TaskLens pre-commit hook
# Source of truth: .claude/hooks/pre-commit.sh
# Install via: ./gradlew installGitHooks
#
# Skippable: git commit --no-verify

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# Collect staged .kt files
STAGED_KT=$(git diff --cached --name-only | grep '\.kt$' || true)

if [ -z "$STAGED_KT" ]; then
  exit 0
fi

KT_COUNT=$(echo "$STAGED_KT" | wc -l | tr -d ' ')
echo "[pre-commit] $KT_COUNT staged Kotlin file(s) detected."

# Step 1: Compile check — hard block on error
echo "[pre-commit] Running compileKotlin..."
if ! ./gradlew compileKotlin --quiet 2>&1; then
  echo ""
  echo "✗ Compile failed. Commit aborted."
  echo "  Fix the errors above, then re-commit."
  echo "  To skip this check: git commit --no-verify"
  exit 1
fi
echo "[pre-commit] Compile OK."

# Step 2: Claude review — only for 5+ changed files
if [ "$KT_COUNT" -lt 5 ]; then
  echo "[pre-commit] < 5 Kotlin files changed, skipping Claude review."
  exit 0
fi

if ! command -v claude &>/dev/null; then
  echo "[pre-commit] claude CLI not found, skipping review."
  exit 0
fi

echo "[pre-commit] Running Claude code review on $KT_COUNT files..."
FILE_LIST=$(echo "$STAGED_KT" | tr '\n' ' ')

REVIEW_OUTPUT=$(claude --print \
  "Review the following staged Kotlin files for CRITICAL issues only (null safety violations, UI thread blocking, CLAUDE.md rule violations). Files: $FILE_LIST. Output only CRITICAL issues in the format: [CRITICAL] file:line — description. If none, output: NO_CRITICAL_ISSUES" \
  2>/dev/null || echo "REVIEW_FAILED")

if echo "$REVIEW_OUTPUT" | grep -q "REVIEW_FAILED\|NO_CRITICAL_ISSUES"; then
  exit 0
fi

CRITICAL_ISSUES=$(echo "$REVIEW_OUTPUT" | grep '^\[CRITICAL\]' || true)

if [ -z "$CRITICAL_ISSUES" ]; then
  exit 0
fi

echo ""
echo "⚠  Claude found CRITICAL issues:"
echo "$CRITICAL_ISSUES"
echo ""
echo "Continue committing anyway? [y/N]"
read -r ANSWER </dev/tty
case "$ANSWER" in
  [yY][eE][sS]|[yY]) exit 0 ;;
  *) echo "Commit aborted. Fix issues or use: git commit --no-verify"; exit 1 ;;
esac
