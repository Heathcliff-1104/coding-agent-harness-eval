#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"

for name in mini dsh pi; do
  root="$V/$name/backend/src/main/java/com/koolearn/bms"
  echo "================ $name REGISTER / CAPTCHA ================"
  rg -n --no-heading -C 10 \
    '@PostMapping\("/register"\)|captchaStore|verifyCaptcha|validateCaptcha|captchaKey|captchaCode' \
    "$root/controller/LoginController.java" || true
  echo "================ $name USER SERVICE VALIDATION ================"
  rg -n --no-heading -C 8 \
    'void register|setRole|PasswordPolicy|password.*matches|BCrypt|passwordEncoder' \
    "$root/service/impl/UserServiceImpl.java" || true
  echo "================ $name AUTHORIZATION HEADER ================"
  rg -n --no-heading -C 8 \
    'Authorization|getHeader|parse|userId|role|status|setAttribute|setStatus' \
    "$root/config/LoginInterceptor.java" "$root/config/RoleInterceptor.java" || true
done
