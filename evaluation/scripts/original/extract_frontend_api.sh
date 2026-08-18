#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"
OUT="$V/evidence/frontend-api-source.txt"

: > "$OUT"

for name in mini dsh pi; do
  root="$V/$name/frontend/src"
  {
    echo "================ $name API FILES ================"
    find "$root" -type f -path '*/api/*.js' -printf '%P\n' | sort
    echo "================ $name API SOURCE ================"
    for file in $(find "$root" -type f -path '*/api/*.js' -printf '%p\n' | sort); do
      echo "---------------- ${file#$V/$name/} ----------------"
      cat "$file"
    done
    echo "================ $name DIRECT REQUEST URLS ================"
    rg -n --no-heading \
      'url:|request\.(get|post|put|delete)\(|axios\.(get|post|put|delete)\(' \
      "$root" || true
    echo
  } >> "$OUT"
done

wc -l -c "$OUT"
