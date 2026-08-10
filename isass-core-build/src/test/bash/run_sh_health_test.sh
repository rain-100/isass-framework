#!/usr/bin/env bash

# SPDX-License-Identifier: LGPL-3.0-only

set -euo pipefail

script_dir=$(cd "$(dirname "$0")/../.." && pwd)
run_script="$script_dir/main/resources/run.sh"
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT

# 去掉脚本入口后加载函数，避免测试启动实际服务。
sed '/^run \$\*$/d' "$run_script" > "$temp_dir/run.sh"
# shellcheck source=/dev/null
source "$temp_dir/run.sh"

cat > "$temp_dir/application.yml" <<'YAML'
spring:
  application:
    name: demo-service
server:
  port: "20380" # service port
YAML

test "$(get_server_port "$temp_dir/application.yml")" = "20380"

cat > "$temp_dir/no-port.yml" <<'YAML'
server:
  address: 127.0.0.1
YAML

if get_server_port "$temp_dir/no-port.yml" >/dev/null; then
    echo "get_server_port should fail when server.port is absent" >&2
    exit 1
fi
