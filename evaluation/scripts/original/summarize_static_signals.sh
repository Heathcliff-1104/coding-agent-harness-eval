#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"

for name in baseline mini dsh pi; do
  root="$V/$name"
  echo "================ $name ================"
  printf 'tracked_files='; git -C "$root" ls-files | wc -l
  printf 'main_java='; find "$root/backend/src/main/java" -type f -name '*.java' | wc -l
  printf 'candidate_tests='; find "$root/backend/src/test/java" -type f -name '*.java' ! -path '*/acceptance/*' | wc -l
  printf 'frontend_views='; find "$root/frontend/src/views" -type f -name '*.vue' | wc -l
  printf 'todo_fixme='; { rg -i -l 'TODO|FIXME|待实现|暂未实现|not implemented|unsupportedoperation' "$root/backend/src/main" "$root/frontend/src" 2>/dev/null || true; } | wc -l
  printf 'mock_demo_files='; { rg -i -l 'mock|demo|模拟' "$root/backend/src/main" "$root/frontend/src" 2>/dev/null || true; } | wc -l
  printf 'return_null_files='; { rg -l 'return null' "$root/backend/src/main" "$root/frontend/src" 2>/dev/null || true; } | wc -l
  printf 'frontend_test_files='; find "$root/frontend" -type f \( -name '*.test.*' -o -name '*.spec.*' \) ! -path '*/node_modules/*' | wc -l
  echo '-- suspicious files --'
  rg -i -l \
    'TODO|FIXME|待实现|暂未实现|not implemented|unsupportedoperation|return null' \
    "$root/backend/src/main" "$root/frontend/src" 2>/dev/null \
    | sed "s|$root/||" | sort || true
  echo
done
