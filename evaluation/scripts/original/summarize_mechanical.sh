#!/usr/bin/env bash
set -euo pipefail

VERIFY_ROOT=/workspace/material-system/agent-eval/verification/run-01
RESULT_ROOT="$VERIFY_ROOT/evidence/mechanical"
REPORT="$VERIFY_ROOT/evidence/mechanical-summary.txt"

{
  printf '%-10s %8s %8s %8s %8s %10s %10s\n' \
    candidate tests failures errors skipped backend frontend

  for name in baseline mini dsh pi
  do
    repo="$VERIFY_ROOT/$name"
    out="$RESULT_ROOT/$name"
    read -r tests failures errors skipped < <(
      python3 - "$repo/backend/target/surefire-reports" <<'PY'
import glob
import os
import sys
import xml.etree.ElementTree as ET

root = sys.argv[1]
totals = dict(tests=0, failures=0, errors=0, skipped=0)
for path in glob.glob(os.path.join(root, 'TEST-*.xml')):
    suite = ET.parse(path).getroot()
    for key in totals:
        totals[key] += int(float(suite.attrib.get(key, 0)))
print(totals['tests'], totals['failures'], totals['errors'], totals['skipped'])
PY
    )
    backend=$(cat "$out/backend-exit.txt")
    frontend=$(cat "$out/frontend-build-exit.txt")
    printf '%-10s %8s %8s %8s %8s %10s %10s\n' \
      "$name" "$tests" "$failures" "$errors" "$skipped" "$backend" "$frontend"
  done

  echo
  echo '=== Test source files ==='
  for name in baseline mini dsh pi
  do
    echo "-- $name --"
    find "$VERIFY_ROOT/$name/backend/src/test" -type f \
      -printf '%P\n' 2>/dev/null | sort
    echo 'frontend test-like files:'
    find "$VERIFY_ROOT/$name/frontend" \
      -path '*/node_modules' -prune -o \
      -type f \( -name '*.test.*' -o -name '*.spec.*' \) \
      -printf '%P\n' 2>/dev/null | sort
  done

  echo
  echo '=== Baseline frontend build failure ==='
  cat "$RESULT_ROOT/baseline/frontend-build.log"

  echo
  echo '=== Candidate frontend build tails ==='
  for name in mini dsh pi
  do
    echo "-- $name --"
    tail -n 20 "$RESULT_ROOT/$name/frontend-build.log"
  done

  echo
  echo '=== Maven summary tails ==='
  for name in baseline mini dsh pi
  do
    echo "-- $name --"
    tail -n 20 "$RESULT_ROOT/$name/backend-package.log"
  done
} | tee "$REPORT"
