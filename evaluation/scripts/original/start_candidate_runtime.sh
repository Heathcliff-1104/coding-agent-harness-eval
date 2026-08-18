#!/usr/bin/env bash
set -euo pipefail

name="${1:?candidate name required: mini|dsh|pi}"
case "$name" in mini|dsh|pi) ;; *) echo "unknown candidate: $name" >&2; exit 2 ;; esac

V="/workspace/material-system/agent-eval/verification/run-01"
ROOT="$V/$name"
OUT="$V/evidence/runtime/$name"
MAVEN_REPO="/workspace/material-system/agent-eval/cache/maven/repository"

mkdir -p "$OUT"

if ss -ltn | grep -q ':8080 '; then
  echo 'port 8080 is already occupied' >&2
  exit 3
fi
if ss -ltn | grep -q ':5173 '; then
  echo 'port 5173 is already occupied' >&2
  exit 3
fi

(
  cd "$ROOT/backend"
  mvn -q \
    -Dmaven.repo.local="$MAVEN_REPO" \
    -DincludeScope=test \
    dependency:build-classpath \
    -Dmdep.outputFile="$OUT/dependencies.classpath"
) > "$OUT/classpath-build.log" 2>&1

runtime_classpath="$ROOT/backend/target/test-classes:$ROOT/backend/target/classes:$(cat "$OUT/dependencies.classpath")"

(
  cd "$ROOT/backend"
  export SPRING_DATASOURCE_DRIVER_CLASS_NAME='org.h2.Driver'
  export SPRING_DATASOURCE_URL="jdbc:h2:mem:runtime_${name};MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
  export SPRING_DATASOURCE_USERNAME='sa'
  export SPRING_DATASOURCE_PASSWORD=''
  export SPRING_SQL_INIT_MODE='always'
  export LOGGING_FILE_NAME=''
  export DINGTALK_MOCK_ENABLED='true'
  export DINGTALK_MODE='mock'
  if [ "$name" = 'dsh' ]; then
    export SPRING_SQL_INIT_SCHEMA_LOCATIONS="file:$ROOT/backend/src/test/resources/schema-h2.sql"
  fi
  exec setsid java \
    -cp "$runtime_classpath" \
    com.koolearn.bms.BmsApplication \
    --spring.profiles.active=test \
    --server.port=8080
) > "$OUT/backend.log" 2>&1 < /dev/null &
backend_pid=$!
printf '%s\n' "$backend_pid" > "$OUT/backend.pid"

backend_ready=0
for _ in $(seq 1 180); do
  if curl -fsS --max-time 2 http://127.0.0.1:8080/health > "$OUT/health.json" 2>/dev/null; then
    backend_ready=1
    break
  fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then break; fi
  sleep 1
done

if [ "$backend_ready" -ne 1 ]; then
  echo "BACKEND_READY=NO"
  tail -n 100 "$OUT/backend.log"
  exit 4
fi

(
  cd "$ROOT/frontend"
  exec setsid npm run dev -- --host 0.0.0.0 --port 5173
) > "$OUT/frontend.log" 2>&1 < /dev/null &
frontend_pid=$!
printf '%s\n' "$frontend_pid" > "$OUT/frontend.pid"

frontend_ready=0
for _ in $(seq 1 90); do
  if curl -fsS --max-time 2 http://127.0.0.1:5173/ > "$OUT/index.html" 2>/dev/null; then
    frontend_ready=1
    break
  fi
  if ! kill -0 "$frontend_pid" 2>/dev/null; then break; fi
  sleep 1
done

if [ "$frontend_ready" -ne 1 ]; then
  echo "FRONTEND_READY=NO"
  tail -n 100 "$OUT/frontend.log"
  exit 5
fi

{
  echo "CANDIDATE=$name"
  echo "BACKEND_PID=$backend_pid"
  echo "FRONTEND_PID=$frontend_pid"
  echo "BACKEND_READY=YES"
  echo "FRONTEND_READY=YES"
  echo "STARTED=$(date --iso-8601=seconds)"
  echo "URL=http://127.0.0.1:5173/"
  echo "HEALTH=$(cat "$OUT/health.json")"
} | tee "$OUT/status.txt"
