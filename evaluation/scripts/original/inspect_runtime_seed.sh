#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"

for name in mini dsh pi; do
  root="$V/$name/backend"
  echo "================ $name TEST CONFIG ================"
  find "$root/src/test/resources" -maxdepth 2 -type f -printf '%P\n' 2>/dev/null | sort
  for file in "$root"/src/test/resources/application*.properties; do
    [ -f "$file" ] && { echo "---- ${file#$root/}"; cat "$file"; }
  done
  echo "================ $name SEED SIGNALS ================"
  rg -n --no-heading -C 3 \
    'INSERT INTO sys_user|admin|DataInitializer|Admin@|Abc@|admin123|Warehouse@' \
    "$root/src/main" "$root/src/test" || true
  echo
done
