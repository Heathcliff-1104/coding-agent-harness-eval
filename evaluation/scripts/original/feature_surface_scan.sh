#!/usr/bin/env bash
set -euo pipefail

V="/workspace/material-system/agent-eval/verification/run-01"
OUT="$V/evidence/feature-surface.txt"

: > "$OUT"

for name in baseline mini dsh pi; do
  root="$V/$name"
  {
    echo "================ $name SOURCE COUNTS ================"
    printf 'backend_main_java='; find "$root/backend/src/main/java" -type f -name '*.java' | wc -l
    printf 'backend_test_java='; find "$root/backend/src/test/java" -type f -name '*.java' ! -path '*/acceptance/*' | wc -l
    printf 'frontend_vue='; find "$root/frontend/src" -type f -name '*.vue' | wc -l
    printf 'frontend_js='; find "$root/frontend/src" -type f -name '*.js' | wc -l
    echo

    echo "================ $name FEATURE FILES ================"
    find "$root/backend/src/main/java" "$root/frontend/src" -type f \
      | sed "s|$root/||" \
      | rg -i 'bom|cis|role|permission|replen|restock|backup|loginlog|syslog|stockalert|stockflow|stagnant|export|dingtalk|captcha|rate|security' \
      | sort || true
    echo

    echo "================ $name TODO / PLACEHOLDER / MOCK SIGNALS ================"
    rg -n --no-heading -i \
      'TODO|FIXME|not implemented|unsupportedoperation|待实现|暂未实现|占位|placeholder|mock|demo|模拟|return null|return false' \
      "$root/backend/src/main" "$root/frontend/src" || true
    echo

    echo "================ $name DANGEROUS / SECRET SIGNALS ================"
    rg -n --no-heading -i \
      'password[[:space:]]*=|secret[[:space:]]*=|api[_-]?key[[:space:]]*=|Runtime\.getRuntime|ProcessBuilder|mysqldump|mysqlbinlog|localStorage.*password|remember.*password|Access-Control-Allow-Origin|allowedOrigin' \
      "$root/backend/src/main" "$root/frontend/src" || true
    echo

    echo "================ $name EXPORT / BACKUP / SCHEDULER SIGNALS ================"
    rg -n --no-heading -i \
      '@Scheduled|ZipOutputStream|SXSSFWorkbook|XSSFWorkbook|Content-Disposition|mysqldump|mysqlbinlog|retention|cleanup|备份|导出' \
      "$root/backend/src/main" "$root/frontend/src" || true
    echo
  } >> "$OUT"
done

wc -l -c "$OUT"
