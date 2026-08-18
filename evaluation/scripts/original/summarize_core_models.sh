#!/usr/bin/env bash
set -euo pipefail

INPUT="/workspace/material-system/agent-eval/verification/run-01/evidence/core-model-source.txt"

rg -n \
  '^================|interface |class |^[[:space:]]+[A-Za-z].*\);|private ' \
  "$INPUT"
