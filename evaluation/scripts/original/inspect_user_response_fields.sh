#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"

for name in mini dsh pi; do
  file="$V/$name/backend/src/main/java/com/koolearn/bms/controller/LoginController.java"
  echo "================ $name ================"
  rg -n --no-heading -C 14 \
    '@GetMapping\("/page"\)|@GetMapping\("/info"\)|setPassword\(null\)' \
    "$file" || true
done
