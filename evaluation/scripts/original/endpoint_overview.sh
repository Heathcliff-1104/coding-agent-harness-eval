#!/usr/bin/env bash
set -euo pipefail

VERIFY_ROOT="/workspace/material-system/agent-eval/verification/run-01"
OUT="$VERIFY_ROOT/evidence/endpoint-overview.txt"

: > "$OUT"

for name in baseline mini dsh pi; do
  root="$VERIFY_ROOT/$name"
  {
    echo "================ $name CONTROLLERS ================"
    find "$root/backend/src/main/java" -type f -path '*/controller/*.java' -printf '%P\n' | sort
    echo
    echo "================ $name ENDPOINT ANNOTATIONS ================"
    rg -n --no-heading \
      '@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)' \
      "$root/backend/src/main/java" || true
    echo
    echo "================ $name SECURITY / AUTH SYMBOLS ================"
    rg -n --no-heading \
      'addInterceptors|addPathPatterns|excludePathPatterns|Authorization|Bearer|JWT|Jwt|ROLE_|hasRole|hasAuthority|permission|currentUser|userId' \
      "$root/backend/src/main/java" || true
    echo
  } >> "$OUT"
done

wc -l -c "$OUT"
echo "$OUT"
