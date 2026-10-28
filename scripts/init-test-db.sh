#!/usr/bin/env bash
# 初始化测试数据库：把 sql/01~17 全部导入 medical_test
# 用法：DB_PASSWORD=yourpassword bash scripts/init-test-db.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB_NAME="${TEST_DB_NAME:-medical_test}"
MYSQL_USER="${DB_USERNAME:-root}"
: "${DB_PASSWORD:?请通过 DB_PASSWORD 环境变量提供 MySQL 密码}"

export MYSQL_PWD="$DB_PASSWORD"

mysql -u"$MYSQL_USER" -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

for f in "$ROOT"/sql/*.sql; do
  sed -e "s/CREATE DATABASE IF NOT EXISTS medical /CREATE DATABASE IF NOT EXISTS $DB_NAME /" \
      -e "s/^USE medical;/USE $DB_NAME;/" "$f" | mysql -u"$MYSQL_USER"
done

echo "测试数据库 $DB_NAME 初始化完成"
