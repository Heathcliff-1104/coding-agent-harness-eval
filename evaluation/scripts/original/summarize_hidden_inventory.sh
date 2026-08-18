#!/usr/bin/env bash
set -euo pipefail

ROOT="/workspace/material-system/agent-eval/verification/run-01/evidence/hidden-inventory-v2"

for name in mini dsh pi; do
  echo "================ $name ================"
  rg -n \
    '<<< FAILURE|AssertionFailedError|Tests run:|BUILD SUCCESS|BUILD FAILURE' \
    "$ROOT/$name.log" || true
done
