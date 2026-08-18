#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"

for name in baseline mini dsh pi; do
  echo "================ $name ================"
  sed -n '1,320p' "$V/$name/backend/src/main/resources/application.properties"
done
