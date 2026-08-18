#!/usr/bin/env bash
set -u

EVAL_ROOT=/workspace/material-system/agent-eval

printf '=== roots ===\n'
find "$EVAL_ROOT" -mindepth 1 -maxdepth 2 -type d -printf '%p\n' | sort

printf '\n=== baseline ===\n'
git -C "$EVAL_ROOT/baseline" rev-parse HEAD
git -C "$EVAL_ROOT/baseline" status --short

printf '\n=== candidate identities ===\n'
for pair in \
  "mini:$EVAL_ROOT/worktrees/mini-fix-01" \
  "dsh:$EVAL_ROOT/worktrees/dsh-native-01" \
  "pi:$EVAL_ROOT/workspaces/pi-native-01"
do
  name=${pair%%:*}
  path=${pair#*:}
  printf '%s\n' "-- $name --"
  printf 'path=%s\n' "$path"
  if git -C "$path" rev-parse --git-dir >/dev/null 2>&1; then
    printf 'head='
    git -C "$path" rev-parse HEAD
    printf 'branch='
    git -C "$path" branch --show-current
    printf 'status-lines='
    git -C "$path" status --porcelain | wc -l
    printf 'git-dir='
    git -C "$path" rev-parse --git-dir
    printf 'common-dir='
    git -C "$path" rev-parse --git-common-dir
  else
    echo GIT_UNAVAILABLE
  fi
done

printf '\n=== result artifacts ===\n'
for name in mini-full-run dsh-native-01 pi-native-01
do
  dir="$EVAL_ROOT/results/$name"
  printf '%s\n' "-- $name --"
  if [ -d "$dir" ]; then
    du -sh "$dir"
    find "$dir" -maxdepth 1 -type f -printf '%f %s bytes\n' | sort
  else
    echo MISSING
  fi
done

printf '\n=== toolchain ===\n'
printf 'node='
node --version
printf 'npm='
npm --version
printf 'java='
java -version 2>&1 | head -n 1
printf 'maven='
mvn -version 2>&1 | head -n 1
printf 'git='
git --version
printf 'disk='
df -h /mnt/d | tail -n 1
