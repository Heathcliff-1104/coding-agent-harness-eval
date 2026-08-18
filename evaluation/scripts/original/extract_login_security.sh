#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"
OUT="$V/evidence/login-security-source.txt"

: > "$OUT"

for name in mini dsh pi; do
  root="$V/$name/backend/src/main/java/com/koolearn/bms"
  {
    echo "================ $name LoginController ================"
    sed -n '1,460p' "$root/controller/LoginController.java"
    echo "================ $name UserServiceImpl ================"
    sed -n '1,300p' "$root/service/impl/UserServiceImpl.java"
    echo "================ $name CorsConfig ================"
    sed -n '1,240p' "$root/config/CorsConfig.java"
    echo "================ $name LoginInterceptor ================"
    sed -n '1,300p' "$root/config/LoginInterceptor.java"
    echo "================ $name RoleInterceptor ================"
    sed -n '1,340p' "$root/config/RoleInterceptor.java"
    echo "================ $name Captcha symbols ================"
    rg -n --no-heading -C 4 'class .*Captcha|captchaStore|CAPTCHA|validateCaptcha|verifyCaptcha' "$root" || true
  } >> "$OUT"
done

wc -l -c "$OUT"
