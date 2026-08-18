#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"

show() {
  local title="$1"
  local path="$2"
  echo "================ $title ================"
  sed -n '1,460p' "$path"
}

show 'PI RegisterPrivilegeTest' "$V/pi/backend/src/test/java/com/koolearn/bms/service/RegisterPrivilegeTest.java"
show 'PI SecurityRegressionTest' "$V/pi/backend/src/test/java/com/koolearn/bms/service/SecurityRegressionTest.java"
show 'PI EngineerDataScopeTest' "$V/pi/backend/src/test/java/com/koolearn/bms/service/EngineerDataScopeTest.java"
show 'PI InboundConfirmStockTest' "$V/pi/backend/src/test/java/com/koolearn/bms/service/InboundConfirmStockTest.java"
show 'PI OutboundLockConfirmRejectTest' "$V/pi/backend/src/test/java/com/koolearn/bms/service/OutboundLockConfirmRejectTest.java"
show 'DSH SecurityFlowIntegrationTest' "$V/dsh/backend/src/test/java/com/koolearn/bms/flow/SecurityFlowIntegrationTest.java"
show 'DSH StockFlowIntegrationTest' "$V/dsh/backend/src/test/java/com/koolearn/bms/flow/StockFlowIntegrationTest.java"
show 'MINI InboundOutboundFlowTest' "$V/mini/backend/src/test/java/com/koolearn/bms/InboundOutboundFlowTest.java"
