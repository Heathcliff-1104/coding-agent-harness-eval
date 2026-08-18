#!/usr/bin/env bash
set -euo pipefail

VERIFY_ROOT=/workspace/material-system/agent-eval/verification/run-01
OUT="$VERIFY_ROOT/evidence/static-inventory.txt"

{
  echo '=== Requirement headings ==='
  grep -nE '^#{1,6} ' "$VERIFY_ROOT/baseline/requirements/requirements.md" || true

  for name in baseline mini dsh pi
  do
    repo="$VERIFY_ROOT/$name"
    echo
    echo "================ $name ================"
    echo '--- source counts ---'
    printf 'backend_java_files='
    find "$repo/backend/src" -type f -name '*.java' | wc -l
    printf 'frontend_source_files='
    find "$repo/frontend/src" -type f | wc -l
    printf 'backend_java_lines='
    find "$repo/backend/src" -type f -name '*.java' -print0 | xargs -0 cat | wc -l
    printf 'frontend_source_lines='
    find "$repo/frontend/src" -type f -print0 | xargs -0 cat | wc -l

    echo '--- controllers and endpoints ---'
    rg -n --glob '*.java' \
      '@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)' \
      "$repo/backend/src/main/java" || true

    echo '--- security/interceptor/config classes ---'
    find "$repo/backend/src/main/java" -type f \
      \( -iname '*security*' -o -iname '*auth*' -o -iname '*jwt*' \
         -o -iname '*interceptor*' -o -iname '*permission*' \
         -o -iname '*exception*' \) \
      -printf '%P\n' | sort

    echo '--- schemas and migrations ---'
    find "$repo/backend/src" -type f \
      \( -iname '*.sql' -o -iname '*schema*' -o -iname '*migration*' \) \
      -printf '%P\n' | sort

    echo '--- application configuration keys ---'
    find "$repo/backend/src" -type f \
      \( -name 'application*.properties' -o -name 'application*.yml' -o -name 'application*.yaml' \) \
      -print0 | while IFS= read -r -d '' file; do
        echo "file=${file#"$repo/"}"
        sed -E \
          -e 's#(^|[._-])(password|secret|api[-_.]?key)([=:])[[:space:]]*.*#\1\2\3<REDACTED>#I' \
          "$file"
      done

    echo '--- frontend API declarations ---'
    rg -n --glob '*.{js,ts,vue}' \
      '(url:|request\(|axios\.|method:|/user/|/material/|/inbound/|/outbound/|/inventory/|/statistics/|/backup/|/log/)' \
      "$repo/frontend/src" || true

    echo '--- tests: HTTP paths and assertions ---'
    rg -n --glob '*.java' \
      '(mockMvc|perform\(|post\(|get\(|put\(|delete\(|assert|Assertions|Expect)' \
      "$repo/backend/src/test" || true

    echo '--- suspicious tracked generated or secret-like paths ---'
    git -C "$repo" ls-files | rg \
      '(^|/)(node_modules|target|dist|logs?)(/|$)|(^|/)\.env($|\.)|\.(log|class|jar|war)$|password|secret|credential' || true
  done
} > "$OUT"

wc -l -c "$OUT"
echo "STATIC_INVENTORY=$OUT"
