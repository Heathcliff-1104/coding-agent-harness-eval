#!/usr/bin/env bash
set -uo pipefail

VERIFY_ROOT="/workspace/material-system/agent-eval/verification/run-01"
EVIDENCE="$VERIFY_ROOT/evidence/hidden-inventory-v2"
SOURCE="/mnt/c/Users/1/Documents/miniSWE/ExternalInventoryAcceptanceTest.java"
MAVEN_REPO="/workspace/material-system/agent-eval/cache/maven/repository"

mkdir -p "$EVIDENCE"
sha256sum "$SOURCE" > "$EVIDENCE/test-source.sha256"
cp "$SOURCE" "$EVIDENCE/ExternalInventoryAcceptanceTest.java"

for name in mini dsh pi; do
  root="$VERIFY_ROOT/$name"
  target="$root/backend/src/test/java/com/koolearn/bms/acceptance/ExternalInventoryAcceptanceTest.java"
  log="$EVIDENCE/$name.log"
  summary="$EVIDENCE/$name-summary.txt"

  mkdir -p "$(dirname "$target")"
  cp "$SOURCE" "$target"

  {
    echo "CANDIDATE=$name"
    echo "COMMIT=$(git -C "$root" rev-parse HEAD)"
    echo "TREE=$(git -C "$root" rev-parse HEAD^{tree})"
    echo "TEST_SHA256=$(sha256sum "$target" | awk '{print $1}')"
    echo "STARTED=$(date --iso-8601=seconds)"
  } > "$summary"

  SECONDS=0
  (
    cd "$root/backend"
    timeout --signal=TERM --kill-after=30s 8m \
      mvn \
        -Dmaven.repo.local="$MAVEN_REPO" \
        -Dtest=ExternalInventoryAcceptanceTest \
        test
  ) > "$log" 2>&1
  exit_code=$?

  {
    echo "EXIT=$exit_code"
    echo "SECONDS=$SECONDS"
    echo "FINISHED=$(date --iso-8601=seconds)"
    grep -E 'Tests run:|BUILD SUCCESS|BUILD FAILURE' "$log" | tail -n 8 || true
  } >> "$summary"
done

for name in mini dsh pi; do
  cat "$EVIDENCE/$name-summary.txt"
done
