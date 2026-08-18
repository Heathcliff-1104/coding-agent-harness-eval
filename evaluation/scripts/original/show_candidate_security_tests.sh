#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"

echo '================ MINI SecurityFlowTest ================'
sed -n '1,320p' "$V/mini/backend/src/test/java/com/koolearn/bms/SecurityFlowTest.java"

echo '================ DSH AuthIntegrationTest ================'
sed -n '1,380p' "$V/dsh/backend/src/test/java/com/koolearn/bms/flow/AuthIntegrationTest.java"

echo '================ PI SecurityRegressionTest ================'
sed -n '1,420p' "$V/pi/backend/src/test/java/com/koolearn/bms/SecurityRegressionTest.java"
