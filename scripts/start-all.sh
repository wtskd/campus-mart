#!/usr/bin/env bash
# ============================================================================
# CampusMart 启动脚本（Git Bash 版）
#   用法：  ./scripts/start-all.sh
#   停止：  ./scripts/stop-all.sh
#   说明：  会先检查 MySQL/Redis/Nacos，再拉起 6 个后端服务，日志写到 logs/
# ============================================================================
set -uo pipefail

# ---------- 路径（自动定位到本仓库；换机器可用环境变量覆盖） ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CM="$(cd "$SCRIPT_DIR/.." && pwd)"          # campus-mart/ 仓库根目录
WS="$(cd "$CM/.." && pwd)"                  # 上层工作区（可选，用于放 _env）
LOGS="$CM/logs"
UPLOAD="$CM/upload"

# 可覆盖：JAVA_EXE / REDIS_EXE / REDIS_CONF / NACOS_HOME / AI_ENV
JAVA="${JAVA_EXE:-D:/project/jdk17/bin/java.exe}"
REDIS_EXE="${REDIS_EXE:-D:/project/redis/redis-server.exe}"
REDIS_CONF="${REDIS_CONF:-$CM/scripts/redis.conf}"
NACOS_DIR="${NACOS_HOME:-$WS/_env/nacos}"
NACOS_WIN="$NACOS_DIR"
AI_ENV="${AI_ENV:-$WS/_env/ai-key.env}"

# java：优先 JAVA_HOME，其次 PATH，最后回退到常见安装目录
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java.exe" ]; then
  JAVA="$JAVA_HOME/bin/java.exe"
elif command -v java >/dev/null 2>&1; then
  JAVA="$(command -v java)"
fi
if [ -z "${JAVA_HOME:-}" ] && [ -x "D:/project/jdk17/bin/java.exe" ]; then
  export JAVA_HOME="D:/project/jdk17"
fi
# 保证 mvn 在 Git Bash 里可用（PATH 里没有时补上常见路径）
case ":$PATH:" in *":/d/project/jdk17/bin:"*) ;; *) PATH="/d/project/jdk17/bin:$PATH" ;; esac
case ":$PATH:" in *":/d/project/maven/bin:"*) ;; *) PATH="/d/project/maven/bin:$PATH" ;; esac
export PATH

mkdir -p "$LOGS" "$LOGS/redis-data" "$UPLOAD"

# ---------- 小工具 ----------
port_up() {  # $1=端口
  netstat -ano 2>/dev/null | tr -d '\r' | grep -qE "[:.]$1[[:space:]].*LISTENING"
}

port_pids() {  # $1=端口 -> 打印占用 PID
  netstat -ano 2>/dev/null | tr -d '\r' | grep -E "[:.]$1[[:space:]].*LISTENING" | awk '{print $5}' | sort -u
}

wait_port() {  # $1=端口 $2=名称 $3=最多等几秒
  local p=$1 name=$2 limit=${3:-120} i
  for ((i = 1; i <= limit; i++)); do
    if port_up "$p"; then
      printf '  [UP]   %-16s :%s\n' "$name" "$p"
      return 0
    fi
    sleep 1
  done
  printf '  [DOWN] %-16s :%s   <-- 看 logs/%s.out.log\n' "$name" "$p" "$name"
  return 1
}

need() {  # 检查可执行文件存在
  if [ ! -e "$1" ]; then
    echo "!! 找不到：$1"
    echo "   请确认路径，或修改本脚本顶部的路径变量"
    exit 1
  fi
}

need "$JAVA"
need "$CM/campus-gateway/target/campus-gateway-1.0.0.jar"

echo "=============================================="
echo " CampusMart 启动（Git Bash）"
echo "=============================================="

# ---------- 1. 基础设施 ----------
echo
echo "[1/3] 基础设施"

if port_up 3306; then
  echo "  [OK]   MySQL :3306"
else
  echo "  [ERR]  MySQL :3306 未监听。请先启动 Windows 服务 MySQL80："
  echo "         net start MySQL80     （需要管理员权限）"
  exit 1
fi

if port_up 6379; then
  echo "  [SKIP] Redis 已在 :6379"
else
  echo "  [START] Redis ..."
  ( cd "$CM" && nohup "$REDIS_EXE" "$REDIS_CONF" > "$LOGS/redis.out.log" 2>&1 & )
  sleep 1
fi

if port_up 8848; then
  echo "  [SKIP] Nacos 已在 :8848"
else
  echo "  [START] Nacos（约 15-25 秒）..."
  (
    cd "$NACOS_DIR" || exit 1
    nohup "$JAVA" -Xms512m -Xmx512m -Xmn256m \
      -Dnacos.standalone=true \
      "-Dnacos.home=$NACOS_WIN" \
      '-Dloader.path=plugins,plugins/health,plugins/cmdb,plugins/selector' \
      '-Dlogging.config=conf/nacos-logback.xml' \
      -jar target/nacos-server.jar \
      --spring.config.additional-location=file:conf/ \
      > "$LOGS/nacos.out.log" 2>&1 &
  )
fi

wait_port 6379 redis 30  || { echo "  Redis 启动失败，日志：" ; tail -n 3 "$LOGS/redis.out.log" ; exit 1; }
wait_port 8848 nacos 120 || { echo "  Nacos 起不来，看 logs/nacos.out.log"; exit 1; }

# ---------- 2. AI 配置 ----------
if [ -f "$AI_ENV" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$AI_ENV"
  set +a
  echo "  [OK]   已加载 AI 配置：$AI_ENV（provider=${AI_PROVIDER:-remote}）"
else
  echo "  [WARN] 未找到 $AI_ENV，AI 将走离线兜底（mock）"
fi

# ---------- 3. 后端服务 ----------
echo
echo "[2/3] 检查 jar 并启动 6 个后端服务"

JARS=(
  "campus-modules/campus-system/target/campus-system-1.0.0.jar"
  "campus-modules/campus-item/target/campus-item-1.0.0.jar"
  "campus-modules/campus-analysis/target/campus-analysis-1.0.0.jar"
  "campus-modules/campus-ai/target/campus-ai-1.0.0.jar"
  "campus-auth/target/campus-auth-1.0.0.jar"
  "campus-gateway/target/campus-gateway-1.0.0.jar"
)

# fat jar 都在 50MB 以上；小于 10MB 说明是构建失败的瘦 jar 或文件被占用后残留
jar_ok() {  # $1=绝对路径
  [ -f "$1" ] || return 1
  local size
  size=$(stat -c %s "$1" 2>/dev/null || echo 0)
  [ "$size" -gt 10485760 ]
}

NEED_BUILD=0
for j in "${JARS[@]}"; do
  if ! jar_ok "$CM/$j"; then
    echo "  [BAD]  $j 缺失或不是可执行的 fat jar"
    NEED_BUILD=1
  fi
done

# 源码比 jar 新（改过代码但没重新打包）也强制重建一次，避免跑旧包
SRC_NEW=$(find "$CM" -path "*/target" -prune -o -path "*/node_modules" -prune -o \( -name '*.java' -o -name '*.yml' -o -name '*.xml' \) -newer "$CM/${JARS[0]}" -print -quit 2>/dev/null)
if [ -n "$SRC_NEW" ]; then
  echo "  [STALE] 源码比 jar 新：${SRC_NEW#$CM/}"
  NEED_BUILD=1
fi

if [ "$NEED_BUILD" -eq 1 ]; then
  echo "  -> 自动执行构建（首次约 2-4 分钟）..."
  MVN_BIN=""
  if command -v mvn >/dev/null 2>&1; then
    MVN_BIN="mvn"
  elif [ -x /d/project/maven/bin/mvn ]; then
    MVN_BIN="/d/project/maven/bin/mvn"
  fi
  if [ -z "$MVN_BIN" ]; then
    echo "  !! 找不到 mvn，请手动执行： cd '$CM' && /d/project/maven/bin/mvn -DskipTests package"
    exit 1
  fi
  if ! ( cd "$CM" && "$MVN_BIN" -q -B -DskipTests package ); then
    echo "  !! 构建失败，看上面的 maven 输出"
    echo "     常见原因：某个 jar 正被 java 进程占用（先结束 java.exe 再试）"
    exit 1
  fi
  echo "  -> 构建完成"
fi

start_svc() {  # $1=名称 $2=端口 $3=jar 相对路径  其余参数为附加参数
  local name=$1 port=$2 jar="$CM/$3"
  shift 3
  if port_up "$port"; then
    echo "  [WARN] $name 的端口 :$port 已被占用，跳过启动"
    echo "         若是上次残留进程： bash scripts/stop-all.sh"
    echo "         或手动结束： netstat -ano | grep \":$port \" | grep LISTENING  然后 taskkill //F //PID <PID>"
    return 0
  fi
  if ! jar_ok "$jar"; then
    echo "  [ERR]  $jar 不可用（不是 fat jar）"
    return 1
  fi
  printf '  [START] %-16s :%s ...\n' "$name" "$port"
  nohup "$JAVA" \
    -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai \
    -Xms256m -Xmx768m \
    -jar "$jar" \
    "--campus.upload.path=$UPLOAD" \
    "$@" \
    > "$LOGS/$name.out.log" 2>&1 &
  echo $! > "$LOGS/$name.pid"
}

FAIL=0
start_svc campus-system   9201 campus-modules/campus-system/target/campus-system-1.0.0.jar       || FAIL=1
start_svc campus-item     9202 campus-modules/campus-item/target/campus-item-1.0.0.jar           || FAIL=1
start_svc campus-analysis 9203 campus-modules/campus-analysis/target/campus-analysis-1.0.0.jar   || FAIL=1
start_svc campus-ai       9204 campus-modules/campus-ai/target/campus-ai-1.0.0.jar               || FAIL=1
start_svc campus-auth     9200 campus-auth/target/campus-auth-1.0.0.jar                          || FAIL=1
start_svc campus-gateway  8081 campus-gateway/target/campus-gateway-1.0.0.jar                    || FAIL=1

if [ "$FAIL" -ne 0 ]; then
  echo
  echo "!! 有服务缺少 jar，先构建："
  echo "   cd '$CM' && mvn -DskipTests package"
  exit 1
fi

echo
echo "[3/3] 等待端口就绪（最多 120 秒）"
BAD=0
wait_port 9201 system   120 || BAD=1
wait_port 9202 item     120 || BAD=1
wait_port 9203 analysis 120 || BAD=1
wait_port 9204 ai       120 || BAD=1
wait_port 9200 auth     120 || BAD=1
wait_port 8081 gateway  120 || BAD=1

echo
if [ "$BAD" -ne 0 ]; then
  echo "!! 有服务没起来。排查："
  echo "   tail -n 40 '$LOGS/campus-gateway.out.log'"
  echo "   grep -iE 'error|exception' '$LOGS'/campus-*.out.log | tail -n 20"
  exit 1
fi

cat <<EOF

==============================================
 全部就绪 ✅
----------------------------------------------
 管理后台    http://127.0.0.1:5180/login      （需另开窗口跑 npm run dev）
 H5 用户端   http://127.0.0.1:5181            （需另开窗口跑 npm run dev:h5）
 网关 API    http://127.0.0.1:8081/api
 接口文档    http://127.0.0.1:8081/doc.html
 Nacos       http://127.0.0.1:8848/nacos
 日志目录    $LOGS
----------------------------------------------
 停止服务    ./scripts/stop-all.sh
 注意        不要关闭这个 Git Bash 窗口，关掉会带走后台进程
==============================================
EOF
