#!/usr/bin/env bash
# ============================================================================
# CampusMart 停止脚本（Git Bash 版）
#   用法： ./scripts/stop-all.sh [--with-infra]
#     --with-infra  连 Redis 和 Nacos 一起停（默认不停，方便下次快速启动）
# ============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CM="$(cd "$SCRIPT_DIR/.." && pwd)"
LOGS="$CM/logs"

kill_port() {  # $1=端口 $2=名称
  local port=$1 name=$2 pids pid
  pids=$(netstat -ano 2>/dev/null | tr -d '\r' | grep -E "[:.]$port[[:space:]].*LISTENING" | awk '{print $5}' | sort -u)
  if [ -z "$pids" ]; then
    printf '  [--]   %-16s :%s 未在运行\n' "$name" "$port"
    return 0
  fi
  for pid in $pids; do
    if [ -n "$pid" ] && [ "$pid" != "0" ]; then
      # Git Bash 里参数要写双斜杠，MSYS 会转成 /F /PID
      if taskkill //F //PID "$pid" >/dev/null 2>&1; then
        printf '  [OK]   %-16s :%s 已停止 (PID %s)\n' "$name" "$port" "$pid"
      else
        printf '  [ERR]  %-16s :%s 停止失败 (PID %s)，可能需要管理员权限\n' "$name" "$port" "$pid"
      fi
    fi
  done
}

echo "=============================================="
echo " CampusMart 停止（Git Bash）"
echo "=============================================="

kill_port 8081 campus-gateway
kill_port 9200 campus-auth
kill_port 9201 campus-system
kill_port 9202 campus-item
kill_port 9203 campus-analysis
kill_port 9204 campus-ai

if [ "${1:-}" = "--with-infra" ]; then
  echo
  echo "同时停止基础设施："
  kill_port 8848 nacos
  kill_port 6379 redis
fi

echo
echo "完成。"
