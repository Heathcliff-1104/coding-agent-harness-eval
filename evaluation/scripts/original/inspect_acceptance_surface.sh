#!/usr/bin/env bash
set -euo pipefail

VERIFY_ROOT="/workspace/material-system/agent-eval/verification/run-01"

for name in baseline mini dsh pi; do
  root="$VERIFY_ROOT/$name"
  echo "================ $name ================"
  echo "-- controllers --"
  find "$root/backend/src/main/java" -type f -path '*/controller/*.java' -printf '%f\n' | sort
  echo "-- login/register mappings --"
  rg -n --no-heading -C 2 \
    '(/login|/register|login\(|register\()' \
    "$root/backend/src/main/java" || true
  echo "-- security config/interceptors --"
  find "$root/backend/src/main/java" -type f \
    \( -iname '*security*.java' -o -iname '*interceptor*.java' -o -iname '*webconfig*.java' -o -iname '*jwt*.java' \) \
    -printf '%P\n' | sort
  echo "-- test configuration --"
  find "$root/backend/src/test" -type f \
    \( -name 'application*.properties' -o -name 'schema.sql' -o -name 'data.sql' \) \
    -printf '%P\n' 2>/dev/null | sort
  echo
done
