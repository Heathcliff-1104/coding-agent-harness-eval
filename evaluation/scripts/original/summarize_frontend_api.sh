#!/usr/bin/env bash
set -euo pipefail

INPUT="/workspace/material-system/agent-eval/verification/run-01/evidence/frontend-api-source.txt"

rg -n \
  '^================|^export |url:|request\.(get|post|put|delete)|axios\.' \
  "$INPUT"
