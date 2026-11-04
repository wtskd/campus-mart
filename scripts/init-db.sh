#!/usr/bin/env bash
# ============================================================================
# CampusMart 数据库初始化（Git Bash 版，只需要跑一次）
#   用法： ./scripts/init-db.sh
#   前置： MySQL 服务已启动（MySQL80），root 密码 123456
# ============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CM="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL="$CM/sql"
# mysql 客户端：优先 PATH，其次常见安装目录，可用 MYSQL_EXE 覆盖
MYSQL="${MYSQL_EXE:-D:/project/mysql/bin/mysql.exe}"
if command -v mysql >/dev/null 2>&1; then MYSQL="$(command -v mysql)"; fi
# 数据库账号：可用 MYSQL_USER / MYSQL_PASSWORD 覆盖
MYSQL_USER="${MYSQL_USER:-root}"
PWD_DB="${MYSQL_PASSWORD:-123456}"

if [ ! -e "$MYSQL" ]; then
  echo "!! 找不到 mysql 客户端：$MYSQL"
  exit 1
fi

echo "=============================================="
echo " CampusMart 数据库初始化"
echo "=============================================="

# 连通性检查（密码错误会立刻暴露，而不是等到导入一半）
if ! "$MYSQL" -u"$MYSQL_USER" -p"$PWD_DB" -e "select 1" >/dev/null 2>&1; then
  echo "!! 连不上 MySQL（root/$PWD_DB）。请确认："
  echo "   1) MySQL80 服务在运行：net start MySQL80"
  echo "   2) root 密码是 123456（不是的话改本脚本的 PWD_DB）"
  exit 1
fi

run_sql() {  # $1=sql 文件
  local f="$SQL/$1"
  if [ ! -f "$f" ]; then
    echo "!! 找不到 $f"
    exit 1
  fi
  printf '  导入 %-24s ... ' "$1"
  if "$MYSQL" -u"$MYSQL_USER" -p"$PWD_DB" --default-character-set=utf8mb4 < "$f" >/tmp/cm_sql.log 2>&1; then
    echo "OK"
  else
    echo "失败"
    echo "----- 错误输出 -----"
    cat /tmp/cm_sql.log
    exit 1
  fi
}

# 01 脚本内部会 CREATE DATABASE campus_mart，所以这里不用先建库
run_sql 01-schema.sql
run_sql 02-seed-business.sql
run_sql 03-perms-extra.sql
run_sql 04-perms-item-trade.sql
run_sql 05-rich-data.sql
# 前端状态/性别下拉用的字典（sys_normal_disable、sys_user_gender），幂等
run_sql 06-dict-fix.sql
# 首页轮播点击跳转（item:/category:/search:/pages/ 约定），幂等
run_sql 07-banner-links.sql

echo
echo "校验数据："
"$MYSQL" -u"$MYSQL_USER" -p"$PWD_DB" -N -e "
select concat('  商品 biz_item      = ', count(*)) from campus_mart.biz_item;
select concat('  订单 biz_order     = ', count(*)) from campus_mart.biz_order;
select concat('  用户 sys_user      = ', count(*)) from campus_mart.sys_user;
select concat('  菜单 sys_menu      = ', count(*)) from campus_mart.sys_menu;
select concat('  AI 日志 ai_call_log = ', count(*)) from campus_mart.ai_call_log;
select concat('  分类 biz_category   = ', count(*)) from campus_mart.biz_category;
select concat('  同城统计 stat_overview_daily = ', count(*)) from campus_mart.stat_overview_daily;
" 2>/dev/null

echo
echo "初始化完成。接下来： ./scripts/start-all.sh"
