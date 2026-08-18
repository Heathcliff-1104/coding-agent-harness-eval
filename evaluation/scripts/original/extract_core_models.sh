#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"
OUT="$V/evidence/core-model-source.txt"

: > "$OUT"

for name in mini dsh pi; do
  root="$V/$name/backend/src/main/java/com/koolearn/bms"
  {
    echo "================ $name InboundOrderService ================"
    sed -n '1,260p' "$root/service/InboundOrderService.java"
    echo "================ $name OutboundOrderService ================"
    sed -n '1,300p' "$root/service/OutboundOrderService.java"
    echo "================ $name InboundOrderDTO ================"
    sed -n '1,360p' "$root/dto/InboundOrderDTO.java"
    echo "================ $name OutboundOrderDTO ================"
    sed -n '1,420p' "$root/dto/OutboundOrderDTO.java"
    echo "================ $name Material ================"
    sed -n '1,300p' "$root/entity/Material.java"
    echo "================ $name InboundOrder ================"
    sed -n '1,340p' "$root/entity/InboundOrder.java"
    echo "================ $name OutboundOrder ================"
    sed -n '1,340p' "$root/entity/OutboundOrder.java"
  } >> "$OUT"
done

wc -l -c "$OUT"
