#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"
OUT="$V/evidence/implementation-claims.txt"

: > "$OUT"

for name in mini dsh pi; do
  root="$V/$name"
  {
    echo "================ $name ================"
    if [ -f "$root/IMPLEMENTATION_STATUS.md" ]; then
      cat "$root/IMPLEMENTATION_STATUS.md"
    else
      echo 'MISSING IMPLEMENTATION_STATUS.md'
    fi
    echo
  } >> "$OUT"
done

wc -l -c "$OUT"
