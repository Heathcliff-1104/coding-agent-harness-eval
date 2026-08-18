#!/usr/bin/env bash
set -euo pipefail

EVAL_ROOT=/workspace/material-system/agent-eval
VERIFY_ROOT="$EVAL_ROOT/verification/run-01"
EVIDENCE="$VERIFY_ROOT/evidence"

BASE_COMMIT=3bf29e545f9461128ea4d3c589f501ad374f59c1
MINI_COMMIT=65ee84e78844018ca10e3062415e2d691ca87384
DSH_COMMIT=240c420664ce3354c798c46a74aa9843ad221af3
PI_COMMIT=4a59164c9eceae5ab2b6828b30cd4a5df4be72e5

if [ -e "$VERIFY_ROOT" ]; then
  echo "Verification root already exists; refusing to overwrite: $VERIFY_ROOT" >&2
  exit 20
fi

mkdir -p "$EVIDENCE"

git clone --local --no-hardlinks "$EVAL_ROOT/baseline" "$VERIFY_ROOT/baseline"
git -C "$VERIFY_ROOT/baseline" checkout --detach "$BASE_COMMIT"

clone_bundle() {
  local name=$1
  local bundle=$2
  local commit=$3
  local target="$VERIFY_ROOT/$name"

  git -C "$VERIFY_ROOT/baseline" bundle verify "$bundle" \
    > "$EVIDENCE/$name-bundle-verify.txt" 2>&1
  git clone --no-checkout "$bundle" "$target"
  git -C "$target" checkout --detach "$commit"
}

clone_bundle \
  mini \
  "$EVAL_ROOT/results/mini-full-run/mini-full-run.bundle" \
  "$MINI_COMMIT"

clone_bundle \
  dsh \
  "$EVAL_ROOT/results/dsh-native-01/dsh-native-01.bundle" \
  "$DSH_COMMIT"

clone_bundle \
  pi \
  "$EVAL_ROOT/results/pi-native-01/pi-native-01.bundle" \
  "$PI_COMMIT"

{
  echo '=== Verification copies ==='
  for name in baseline mini dsh pi; do
    repo="$VERIFY_ROOT/$name"
    printf '%s head=' "$name"
    git -C "$repo" rev-parse HEAD
    printf '%s tree=' "$name"
    git -C "$repo" rev-parse 'HEAD^{tree}'
    printf '%s status-lines=' "$name"
    git -C "$repo" status --porcelain | wc -l
  done

  echo
  echo '=== Original versus restored tree hashes ==='
  for spec in \
    "mini:$EVAL_ROOT/worktrees/mini-fix-01:$VERIFY_ROOT/mini" \
    "dsh:$EVAL_ROOT/worktrees/dsh-native-01:$VERIFY_ROOT/dsh" \
    "pi:$EVAL_ROOT/workspaces/pi-native-01:$VERIFY_ROOT/pi"
  do
    name=${spec%%:*}
    rest=${spec#*:}
    original=${rest%%:*}
    restored=${rest#*:}
    original_tree=$(git -C "$original" rev-parse 'HEAD^{tree}')
    restored_tree=$(git -C "$restored" rev-parse 'HEAD^{tree}')
    printf '%s original=%s restored=%s match=' \
      "$name" "$original_tree" "$restored_tree"
    if [ "$original_tree" = "$restored_tree" ]; then
      echo YES
    else
      echo NO
      exit 21
    fi
  done
} | tee "$EVIDENCE/reconstruction.txt"

sha256sum \
  "$EVAL_ROOT/results/mini-full-run/mini-full-run.bundle" \
  "$EVAL_ROOT/results/mini-full-run/changes.patch" \
  "$EVAL_ROOT/results/mini-full-run/task.txt" \
  "$EVAL_ROOT/results/mini-full-run/trajectory.json" \
  "$EVAL_ROOT/results/dsh-native-01/dsh-native-01.bundle" \
  "$EVAL_ROOT/results/dsh-native-01/changes.patch" \
  "$EVAL_ROOT/results/dsh-native-01/task.txt" \
  "$EVAL_ROOT/results/pi-native-01/pi-native-01.bundle" \
  "$EVAL_ROOT/results/pi-native-01/changes.patch" \
  "$EVAL_ROOT/results/pi-native-01/task.txt" \
  "$EVAL_ROOT/results/pi-native-01/trajectory.jsonl" \
  > "$EVIDENCE/frozen-artifacts.sha256"

echo "VERIFY_ROOT=$VERIFY_ROOT"
echo 'PREPARATION=OK'
