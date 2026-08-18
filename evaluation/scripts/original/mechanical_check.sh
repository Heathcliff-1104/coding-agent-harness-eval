#!/usr/bin/env bash
set -u

VERIFY_ROOT=/workspace/material-system/agent-eval/verification/run-01
RESULT_ROOT="$VERIFY_ROOT/evidence/mechanical"
MAVEN_REPO=/workspace/material-system/agent-eval/cache/maven/repository
NPM_CACHE=/mnt/d/agent-lab/cache/npm-pi

mkdir -p "$RESULT_ROOT" "$MAVEN_REPO" "$NPM_CACHE"

{
  echo "started=$(date --iso-8601=seconds)"
  echo "node=$(node --version)"
  echo "npm=$(npm --version)"
  echo "java=$(java -version 2>&1 | head -n 1)"
  echo "maven=$(mvn -version 2>&1 | head -n 1)"
  echo "git=$(git --version)"
} > "$RESULT_ROOT/environment.txt"

for name in baseline mini dsh pi
do
  repo="$VERIFY_ROOT/$name"
  out="$RESULT_ROOT/$name"
  mkdir -p "$out"

  echo "=== $name: inventory ==="
  {
    echo "head=$(git -C "$repo" rev-parse HEAD)"
    echo "tree=$(git -C "$repo" rev-parse 'HEAD^{tree}')"
    echo "backend_test_files=$(find "$repo/backend/src/test" -type f 2>/dev/null | wc -l)"
    echo "frontend_test_files=$(find "$repo/frontend" -path '*/node_modules' -prune -o -type f \( -name '*.test.*' -o -name '*.spec.*' \) -print 2>/dev/null | wc -l)"
    echo 'frontend_scripts:'
    node -e '
      const p = require(process.argv[1]);
      console.log(JSON.stringify(p.scripts || {}, null, 2));
    ' "$repo/frontend/package.json"
  } > "$out/inventory.txt" 2>&1

  echo "=== $name: backend clean package ==="
  backend_start=$(date +%s)
  (
    cd "$repo/backend" || exit 90
    timeout --signal=TERM --kill-after=2m 60m \
      mvn -B -ntp \
        -Dmaven.repo.local="$MAVEN_REPO" \
        clean package
  ) > "$out/backend-package.log" 2>&1
  backend_exit=$?
  backend_seconds=$(( $(date +%s) - backend_start ))
  printf '%s\n' "$backend_exit" > "$out/backend-exit.txt"

  {
    echo "exit=$backend_exit"
    echo "seconds=$backend_seconds"
    echo "test_report_files=$(find "$repo/backend/target/surefire-reports" -type f -name '*.txt' 2>/dev/null | wc -l)"
    echo 'surefire_summaries:'
    grep -hE 'Tests run:' "$repo/backend/target/surefire-reports"/*.txt 2>/dev/null || true
    echo 'artifacts:'
    find "$repo/backend/target" -maxdepth 1 -type f \( -name '*.jar' -o -name '*.war' \) -printf '%f %s bytes\n' 2>/dev/null || true
    echo 'log_tail:'
    tail -n 30 "$out/backend-package.log"
  } > "$out/backend-summary.txt"
  echo "$name backend exit=$backend_exit seconds=$backend_seconds"

  echo "=== $name: frontend clean install ==="
  frontend_install_start=$(date +%s)
  (
    cd "$repo/frontend" || exit 90
    timeout --signal=TERM --kill-after=2m 45m \
      env NPM_CONFIG_CACHE="$NPM_CACHE" \
      npm ci --no-audit --no-fund
  ) > "$out/frontend-install.log" 2>&1
  frontend_install_exit=$?
  frontend_install_seconds=$(( $(date +%s) - frontend_install_start ))
  printf '%s\n' "$frontend_install_exit" > "$out/frontend-install-exit.txt"
  echo "$name frontend install exit=$frontend_install_exit seconds=$frontend_install_seconds"

  echo "=== $name: frontend production build ==="
  frontend_build_start=$(date +%s)
  if [ "$frontend_install_exit" -eq 0 ]; then
    (
      cd "$repo/frontend" || exit 90
      timeout --signal=TERM --kill-after=2m 30m npm run build
    ) > "$out/frontend-build.log" 2>&1
    frontend_build_exit=$?
  else
    frontend_build_exit=125
    echo 'Skipped because npm ci failed.' > "$out/frontend-build.log"
  fi
  frontend_build_seconds=$(( $(date +%s) - frontend_build_start ))
  printf '%s\n' "$frontend_build_exit" > "$out/frontend-build-exit.txt"

  {
    echo "install_exit=$frontend_install_exit"
    echo "install_seconds=$frontend_install_seconds"
    echo "build_exit=$frontend_build_exit"
    echo "build_seconds=$frontend_build_seconds"
    echo "dist_files=$(find "$repo/frontend/dist" -type f 2>/dev/null | wc -l)"
    echo "dist_bytes=$(du -sb "$repo/frontend/dist" 2>/dev/null | awk '{print $1}')"
    echo 'install_log_tail:'
    tail -n 20 "$out/frontend-install.log"
    echo 'build_log_tail:'
    tail -n 30 "$out/frontend-build.log"
  } > "$out/frontend-summary.txt"
  echo "$name frontend build exit=$frontend_build_exit seconds=$frontend_build_seconds"

  git -C "$repo" status --porcelain=v1 > "$out/status-after-build.txt"
  git -C "$repo" diff --check > "$out/diff-check-after-build.txt" 2>&1

  {
    echo "candidate=$name"
    echo "backend_exit=$backend_exit"
    echo "backend_seconds=$backend_seconds"
    echo "frontend_install_exit=$frontend_install_exit"
    echo "frontend_install_seconds=$frontend_install_seconds"
    echo "frontend_build_exit=$frontend_build_exit"
    echo "frontend_build_seconds=$frontend_build_seconds"
    echo "tracked_status_lines=$(wc -l < "$out/status-after-build.txt")"
  } | tee "$out/result.txt"
done

echo "finished=$(date --iso-8601=seconds)" >> "$RESULT_ROOT/environment.txt"
echo 'MECHANICAL_CHECKS_COMPLETE'
