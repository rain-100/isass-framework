#!/bin/sh

# SPDX-License-Identifier: LGPL-3.0-only

set -eu

script_dir=$(cd "$(dirname "$0")/../.." && pwd)
run_script="$script_dir/main/resources/run.sh"
temp_dir=$(mktemp -d)
trap 'rm -rf "$temp_dir"' EXIT

# 去掉脚本入口后加载函数，避免测试启动实际服务。
sed '/^run "\$@"$/d' "$run_script" > "$temp_dir/run.sh"
. "$temp_dir/run.sh"

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

test_port_validation() {
    is_valid_port 0
    is_valid_port 65535

    for invalid_port in '' -1 65536 abc 12.3; do
        if is_valid_port "$invalid_port"; then
            echo "is_valid_port should reject [$invalid_port]" >&2
            exit 1
        fi
    done
}

test_option_parsing() {
    command=''
    AUTO_TAIL_LOG=true
    RUN_AS_NOHUP=true
    DEBUG_PORT=''
    JMX_HOSTNAME=''
    JMX_PORT=''

    parse_options start --auto_tail_log=false -n false --debug_port=5005 \
        --jmx_hostname 127.0.0.1 --jmx_port 9010 >/dev/null

    test "$command" = start
    test "$AUTO_TAIL_LOG" = false
    test "$RUN_AS_NOHUP" = false
    test "$DEBUG_PORT" = 5005
    test "$JMX_HOSTNAME" = 127.0.0.1
    test "$JMX_PORT" = 9010

    if (command=''; parse_options start --debug_port invalid >/dev/null 2>&1); then
        echo "parse_options should reject an invalid debug port" >&2
        exit 1
    fi
    if (command=''; parse_options start --jmx_port >/dev/null 2>&1); then
        echo "parse_options should reject a missing option value" >&2
        exit 1
    fi
    if (command=''; parse_options start extra >/dev/null 2>&1); then
        echo "parse_options should reject multiple commands" >&2
        exit 1
    fi
}

test_health_response() {
    is_healthy_response '{"status":"UP"}'
    if is_healthy_response '{"status":"DOWN"}'; then
        echo "is_healthy_response should reject a DOWN response" >&2
        exit 1
    fi
}

test_nohup_auto_tail_branch() {
    mock_bin="$temp_dir/bin"
    mkdir -p "$mock_bin"
    printf '%s\n' '#!/bin/sh' 'exit 0' > "$mock_bin/nohup"
    chmod +x "$mock_bin/nohup"

    PATH="$mock_bin:$PATH"
    export PATH
    RUN_AS_NOHUP=true
    AUTO_TAIL_LOG=true
    RM_LOG=false
    KEEP_DOCKER_RUNNING=false
    JVM_MEMORY_VARS='-Xms64M -Xmx64M'
    tail_marker="$temp_dir/tail-called"

    check_jdk() { :; }
    sleep() { :; }
    print_log() { : > "$tail_marker"; }

    start >/dev/null
    test -f "$tail_marker"
}

test_port_validation
test_option_parsing
test_health_response
test_nohup_auto_tail_branch
