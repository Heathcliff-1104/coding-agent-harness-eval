#!/usr/bin/env bash
set -euo pipefail

INPUT="/workspace/material-system/agent-eval/verification/run-01/evidence/candidate-core-tests.txt"

rg -n \
  '^================|void [A-Za-z]|@Test|andExpect|assertThat|assertEquals|assertThrows' \
  "$INPUT"
