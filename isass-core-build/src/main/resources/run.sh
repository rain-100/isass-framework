#!/bin/sh

# SPDX-License-Identifier: LGPL-3.0-only

#-------------------------------------------------------------------
# 定义变量，可修改以下变量
# 变量值的优先级：运行脚本的 options 参数 > 环境变量 > 变量默认值
#-------------------------------------------------------------------

# 模块名
project_name="@project.artifactId@"

# 运行包名
project_jar="@project.artifactId@-exec.jar"

# 主机环境下的JVM内存参数
JVM_HOST_MEMORY_VARS="-Xms3G -Xmx6G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512M"

# docker 环境下的JVM内存参数
JVM_DOCKER_MEMORY_VARS="-XX:MaxRAMPercentage=88.0 -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512M"

# 默认JVM内存参数，如果设置了环境变量 JVM_MEMORY_VARS ，则会被环境变量覆盖
: "${JVM_MEMORY_VARS:=}"
echo "JVM_MEMORY_VARS=$JVM_MEMORY_VARS"

# 默认JVM非内存参数，如果设置了环境变量 JVM_VARS ，则会被环境变量覆盖
: "${JVM_VARS:=-server -XX:+PrintCommandLineFlags}"
echo "JVM_VARS=$JVM_VARS"

# 是否打印gc信息
: "${JVM_PRINT_GC:=false}"
echo "JVM_PRINT_GC=$JVM_PRINT_GC"

# java 远程调试端口。当设置此值时，本 java 应用会监听此端口，提供给开发人员进行连接，从而实现代码级别调试
: "${DEBUG_PORT:=}"
echo "DEBUG_PORT=$DEBUG_PORT"

# jmx hostname。当设置此值时，本 java 应用会使用本参数设置 jmx 的 hostname。一般设置为本机 ip
: "${JMX_HOSTNAME:=}"
echo "JMX_HOSTNAME=$JMX_HOSTNAME"

# jmx 端口。当设置此值时，本 java 应用会监听此端口，从而对 java 程序进行性能监控
: "${JMX_PORT:=}"
echo "JMX_PORT=$JMX_PORT"

# 启动后是否自动打印日志，如果设置了环境变量 AUTO_TAIL_LOG，则会被环境变量覆盖
: "${AUTO_TAIL_LOG:=true}"
echo "AUTO_TAIL_LOG=$AUTO_TAIL_LOG"

# 启动前是否先删除所有日志文件，如果设置了环境变量 AUTO_TAIL_LOG，则会被环境变量覆盖
: "${RM_LOG:=false}"
echo "RM_LOG=$RM_LOG"

# 启动 java 的命令是否结合 nohup 进行不挂断运行，如果设置了环境变量 RUN_AS_NOHUP，则会被环境变量覆盖
: "${RUN_AS_NOHUP:=true}"
echo "RUN_AS_NOHUP=$RUN_AS_NOHUP"

# 当在 docker 环境中，启动 java 报错后，会导致容器退出，可配置此参数，阻止容器退出，便于进入容器调试问题
: "${KEEP_DOCKER_RUNNING:=false}"
echo "KEEP_DOCKER_RUNNING=$KEEP_DOCKER_RUNNING"

# 打印日志到控制台
: "${WRITE_LOG_STDOUT:=false}"
echo "WRITE_LOG_STDOUT=$WRITE_LOG_STDOUT"

# 打印日志到日志文件
: "${WRITE_LOG_TO_FILE:=true}"
echo "WRITE_LOG_TO_FILE=$WRITE_LOG_TO_FILE"

# java 程序生成日志文件的目录，不能随便改
LOG_PATH="./logs/"
echo "LOG_PATH=$LOG_PATH"

#-------------------------------------------------------------------
# 以下内容请不要修改
#-------------------------------------------------------------------

pid=''
command=''
CURRENT_SCRIPT_DIR=$(
    CDPATH= cd "$(dirname "$0")" || exit 1
    pwd -P
)

print_usage() {
    echo "usage:"
    echo "  run.sh [command] [options]"
    echo ""
    echo "command:"
    echo "  start                     [default command] start the server"
    echo "  stop                      stop the server"
    echo "  status                    status the server"
    echo "  health                    health check"
    echo "  log                       print log"
    echo "  h, help                   print help information"

    echo ""
    echo "options:"
    echo "  -h, --help                        print help information."
    echo "  -l, --auto_tail_log true|false    whether to output logs in current process, default to true"
    echo "  -n, --run_as_nohup true|false     running server using nohup, default to true"
    echo "  -d, --debug_port [0-65535]        java remote debug listening port, must be port range[0-65535]"
    echo "  -r, --rm_log                      remove all log files before startup"
    echo "  --print_gc                        print gc info"
    echo "  --jmx_hostname                    jmx hostname. Generally, set this parameter to the server IP address"
    echo "  --jmx_port                        listening jmx port"
}

get_pid() {
    if [ ! -f "application.pid" ]; then
        pid=''
    else
        pid=$(head -n 1 application.pid)
        pid="${pid}"
    fi
}

start() {
    check_jdk

    get_pid

    if [ -n "$pid" ]; then
        if [ -d "/proc/${pid}" ]; then
            echo "found pid file './application.pid', ${project_name} is running, pid is ${pid}, can not start repeatedly!"
            exit 1
        fi
    fi

    echo "try to start ${project_name} ..."
    echo ""

    # 判断 JVM_MEMORY_VARS 是否为空，为空内根据当前环境是否docker来使用JVM_DOCKER_MEMORY_VARS 或 JVM_HOST_MEMORY_VARS 来赋值给 JVM_MEMORY_VARS
    if [ -z "$JVM_MEMORY_VARS" ]; then
        if [ -f /.dockerenv ]; then
            # "Running in Docker"
            JVM_MEMORY_VARS="${JVM_DOCKER_MEMORY_VARS}"
        else
            # "Not running in Docker"
            JVM_MEMORY_VARS="${JVM_HOST_MEMORY_VARS}"
        fi
        echo "reset JVM_MEMORY_VARS=${JVM_MEMORY_VARS}"
        echo ""
    fi

    jvm_params="${JVM_VARS} ${JVM_MEMORY_VARS}"
    if [ "$JVM_PRINT_GC" = "true" ]; then
        jvm_params="${jvm_params} -XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:${LOG_PATH}/gc.log"
    fi
    if [ -n "$DEBUG_PORT" ]; then
        jvm_params="${jvm_params} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
    fi
    if [ -n "$JMX_PORT" ]; then
        jvm_params="${jvm_params} -Djava.rmi.server.hostname=${JMX_HOSTNAME} -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=$JMX_PORT -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
    fi

    if [ -n "$WRITE_LOG_STDOUT" ]; then
        jvm_params="${jvm_params} -DWRITE_LOG_STDOUT=$WRITE_LOG_STDOUT"
    fi

    if [ -n "$WRITE_LOG_TO_FILE" ]; then
        jvm_params="${jvm_params} -DWRITE_LOG_TO_FILE=$WRITE_LOG_TO_FILE"
    fi

    cmd="java ${jvm_params} -jar ${project_jar}"
    if [ "$RUN_AS_NOHUP" = "true" ]; then
        cmd="nohup $cmd 1>/dev/null 2>&1 &"
    fi

    if [ "$RM_LOG" = "true" ]; then
        echo "deleting all log files..."
        rm -rf "${LOG_PATH}"/*
    fi

    echo "executing cmd:"
    echo "$cmd"
    echo ""
    eval "$cmd"

    if [ "$AUTO_TAIL_LOG" = "true" ] && [ "$RUN_AS_NOHUP" = "true" ]; then
        echo 'log will printing after 5 second using command "tail -f -n 500" automatic.'
        echo 'you can use "ctrl+c" to exit log printing, and will not close the application.'
        echo ''

        sleep 5
        print_log
    else
        if [ "$RUN_AS_NOHUP" != "true" ]; then
            echo "app started, use './run.sh status' to check status"
        fi
    fi

    if [ "$KEEP_DOCKER_RUNNING" = "true" ]; then
        tail -f /dev/null
    fi
}

stop() {
    echo "try to stop ${project_name} ..."

    get_pid

    if [ -z "$pid" ]; then
        echo "${project_name} is not running!"
        return 0
    fi

    if [ -d "/proc/${pid}" ]; then
        echo "${project_name} is running, pid is ${pid}"
        kill "$pid"
        if [ $? -ne 0 ]; then
            echo "failed to stop ${project_name}!"
            return 1
        else
            echo "${project_name} stopped."
            return 0
        fi
    else
        echo "${project_name} is not running!"
    fi
}

status() {
    get_pid
    if [ -z "$pid" ]; then
        echo "${project_name} is not running."
    else
        if [ -d "/proc/${pid}" ]; then
            echo "${project_name} is running. pid ${pid}."
        else
            echo "can not found running pid ${pid}, ${project_name} is not running!"
        fi
    fi
}

print_log() {
    if [ ! -d "$LOG_PATH" ]; then
        echo "print log error, can not found log folder [${LOG_PATH}], please try print log later."
        return 1
    fi

    filename=$(ls -t "${LOG_PATH}"/log* 2>/dev/null | sed -n '1p')
    if [ -z "$filename" ]; then
        echo "print log error, can not found log file in [${LOG_PATH}], please try print log later."
        return 1
    fi
    tail -n 500 -f "$filename"
}

check_jdk() {
    if command -v java >/dev/null 2>&1; then
        java_version=$(java -version 2>&1 | sed '1!d' | sed -e 's/"//g' -e 's/version//')
        echo "java_version: ${java_version}"
    else
        echo "jdk is not install, please install first!"
        exit 1
    fi
}

get_server_port() {
    config_file="${1:-config/application.yml}"
    if [ ! -f "$config_file" ]; then
        return 1
    fi

    port=$(awk '
        /^[[:space:]]*#/ { next }
        /^[[:space:]]*server:[[:space:]]*(#.*)?$/ { in_server = 1; next }
        in_server && /^[^[:space:]]/ { in_server = 0 }
        in_server && /^[[:space:]]+port:[[:space:]]*/ {
            value = $0
            sub(/^[[:space:]]*port:[[:space:]]*/, "", value)
            sub(/[[:space:]]*(#.*)?$/, "", value)
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
            if (value ~ /^".*"$/) {
                sub(/^"/, "", value)
                sub(/"$/, "", value)
            }
            if (value ~ /^[0-9]+$/) {
                print value
                exit
            }
        }
    ' "$config_file")
    if [ -z "$port" ]; then
        return 1
    fi
    echo "$port"
}

health_check() {
    get_pid
    if [ -z "$pid" ]; then
        echo "${project_name} is not running."
        echo "${project_name} unhealthy"
        exit 1
    else
        if [ -d "/proc/${pid}" ]; then
            port=$(get_server_port)
            if [ -z "$port" ]; then
                echo "cannot read a numeric server.port from config/application.yml"
                echo "${project_name} unhealthy"
                exit 1
            fi

            microService=${project_name##*-service-}
            url="http://localhost:${port}/${microService}/actuator/health"

            echo "$url"
            if ! resp=$(http_get "$url"); then
                echo "health request failed; install curl or wget and check the service endpoint"
                echo "${project_name} unhealthy"
                exit 1
            fi
            echo "$resp"
            if is_healthy_response "$resp"; then
                echo "${project_name} health"
                exit 0
            fi
            echo "${project_name} unhealthy"
            exit 1
        else
            echo "can not found running pid ${pid}, ${project_name} is not running!"
            echo "${project_name} unhealthy"
            exit 1
        fi
    fi
}

http_get() {
    if command -v curl >/dev/null 2>&1; then
        curl --silent --show-error --connect-timeout 5 --max-time 5 "$1"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -T 5 -O - "$1"
    else
        echo "health check requires curl or wget" >&2
        return 127
    fi
}

is_healthy_response() {
    case "$1" in
    *'"UP"'*) return 0 ;;
    *) return 1 ;;
    esac
}

is_valid_port() {
    case "$1" in
    '' | *[!0-9]*) return 1 ;;
    esac
    [ "$1" -le 65535 ] 2>/dev/null
}

set_run_command() {
    if [ -n "$command" ]; then
        echo "unexpected argument: $1" >&2
        exit 1
    fi
    command=$1
}

parse_options() {
    while [ "$#" -gt 0 ]; do
        case "$1" in
        -h | --help)
            print_usage
            exit 0
            ;;
        --print_gc)
            echo "$1"
            JVM_PRINT_GC="true"
            shift
            ;;
        -r | --rm_log)
            echo "$1"
            RM_LOG="true"
            shift
            ;;
        -l | --auto_tail_log)
            option_name=$1
            if [ "$#" -lt 2 ]; then
                echo "missing value for option $option_name" >&2
                exit 1
            fi
            case "$2" in
            true | false)
                echo "$option_name=$2"
                AUTO_TAIL_LOG=$2
                shift 2
                ;;
            *)
                echo "error value in option $option_name, must be true|false"
                exit 1
                ;;
            esac
            ;;
        --auto_tail_log=*)
            option_name=${1%%=*}
            option_value=${1#*=}
            case "$option_value" in
            true | false)
                echo "$option_name=$option_value"
                AUTO_TAIL_LOG=$option_value
                shift
                ;;
            *)
                echo "error value in option $option_name, must be true|false"
                exit 1
                ;;
            esac
            ;;
        -n | --run_as_nohup)
            option_name=$1
            if [ "$#" -lt 2 ]; then
                echo "missing value for option $option_name" >&2
                exit 1
            fi
            case "$2" in
            true | false)
                echo "$option_name=$2"
                RUN_AS_NOHUP=$2
                shift 2
                ;;
            *)
                echo "error value in option $option_name, must be true|false"
                exit 1
                ;;
            esac
            ;;
        --run_as_nohup=*)
            option_name=${1%%=*}
            option_value=${1#*=}
            case "$option_value" in
            true | false)
                echo "$option_name=$option_value"
                RUN_AS_NOHUP=$option_value
                shift
                ;;
            *)
                echo "error value in option $option_name, must be true|false"
                exit 1
                ;;
            esac
            ;;
        -d | --debug_port)
            option_name=$1
            if [ "$#" -lt 2 ]; then
                echo "missing value for option $option_name" >&2
                exit 1
            fi
            if is_valid_port "$2"; then
                echo "$option_name=$2"
                DEBUG_PORT=$2
                shift 2
            else
                echo "$option_name=$2"
                echo "error value in option $option_name, must be port range[0-65535]"
                exit 1
            fi
            ;;
        --debug_port=*)
            option_name=${1%%=*}
            option_value=${1#*=}
            if is_valid_port "$option_value"; then
                echo "$option_name=$option_value"
                DEBUG_PORT=$option_value
                shift
            else
                echo "$option_name=$option_value"
                echo "error value in option $option_name, must be port range[0-65535]"
                exit 1
            fi
            ;;
        --jmx_hostname)
            option_name=$1
            if [ "$#" -lt 2 ]; then
                echo "missing value for option $option_name" >&2
                exit 1
            fi
            echo "$option_name=$2"
            JMX_HOSTNAME=$2
            shift 2
            ;;
        --jmx_hostname=*)
            option_name=${1%%=*}
            option_value=${1#*=}
            echo "$option_name=$option_value"
            JMX_HOSTNAME=$option_value
            shift
            ;;
        --jmx_port)
            option_name=$1
            if [ "$#" -lt 2 ]; then
                echo "missing value for option $option_name" >&2
                exit 1
            fi
            if is_valid_port "$2"; then
                echo "$option_name=$2"
                JMX_PORT=$2
                shift 2
            else
                echo "$option_name=$2"
                echo "error value in option $option_name, must be port range[0-65535]"
                exit 1
            fi
            ;;
        --jmx_port=*)
            option_name=${1%%=*}
            option_value=${1#*=}
            if is_valid_port "$option_value"; then
                echo "$option_name=$option_value"
                JMX_PORT=$option_value
                shift
            else
                echo "$option_name=$option_value"
                echo "error value in option $option_name, must be port range[0-65535]"
                exit 1
            fi
            ;;
        --)
            shift
            while [ "$#" -gt 0 ]; do
                set_run_command "$1"
                shift
            done
            break
            ;;
        -*)
            echo "unknown option: $1" >&2
            exit 1
            ;;
        *)
            set_run_command "$1"
            shift
            ;;
        esac
    done
    echo ""
}

run() {
    parse_options "$@"

    cd "$CURRENT_SCRIPT_DIR" || exit 1
    if [ -z "$command" ]; then
        start
    else
        case "$command" in
        start) start ;;
        stop) stop ;;
        status) status ;;
        log) print_log ;;
        health) health_check ;;
        h | help) print_usage ;;
        *)
            echo "illegal command: $command"
            echo ""
            print_usage
            return 1
            ;;
        esac
    fi
}

run "$@"
