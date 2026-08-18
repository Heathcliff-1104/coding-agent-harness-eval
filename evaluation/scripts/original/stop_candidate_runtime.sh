#!/usr/bin/env bash
set -euo pipefail

name="${1:?candidate name required: mini|dsh|pi}"
OUT="/workspace/material-system/agent-eval/verification/run-01/evidence/runtime/$name"

for kind in frontend backend; do
  pid_file="$OUT/$kind.pid"
  if [ -f "$pid_file" ]; then
    pid="$(cat "$pid_file")"
    if kill -0 "$pid" 2>/dev/null; then
      kill -TERM -- "-$pid" 2>/dev/null || kill -TERM "$pid" 2>/dev/null || true
      for _ in $(seq 1 20); do
        kill -0 "$pid" 2>/dev/null || break
        sleep 0.5
      done
      if kill -0 "$pid" 2>/dev/null; then
        kill -KILL -- "-$pid" 2>/dev/null || kill -KILL "$pid" 2>/dev/null || true
      fi
    fi
  fi
done

echo "STOPPED=$name"
