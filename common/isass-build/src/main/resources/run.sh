#!/bin/sh

#
#                    GNU LESSER GENERAL PUBLIC LICENSE
#                        Version 3, 29 June 2007
#
#  Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
#  Everyone is permitted to copy and distribute verbatim copies
#  of this license document, but changing it is not allowed.
#
#
#   This version of the GNU Lesser General Public License incorporates
# the terms and conditions of version 3 of the GNU General Public
# License, supplemented by the additional permissions listed below.
#
#   0. Additional Definitions.
#
#   As used herein, "this License" refers to version 3 of the GNU Lesser
# General Public License, and the "GNU GPL" refers to version 3 of the GNU
# General Public License.
#
#   "The Library" refers to a covered work governed by this License,
# other than an Application or a Combined Work as defined below.
#
#   An "Application" is any work that makes use of an interface provided
# by the Library, but which is not otherwise based on the Library.
# Defining a subclass of a class defined by the Library is deemed a mode
# of using an interface provided by the Library.
#
#   A "Combined Work" is a work produced by combining or linking an
# Application with the Library.  The particular version of the Library
# with which the Combined Work was made is also called the "Linked
# Version".
#
#   The "Minimal Corresponding Source" for a Combined Work means the
# Corresponding Source for the Combined Work, excluding any source code
# for portions of the Combined Work that, considered in isolation, are
# based on the Application, and not on the Linked Version.
#
#   The "Corresponding Application Code" for a Combined Work means the
# object code and/or source code for the Application, including any data
# and utility programs needed for reproducing the Combined Work from the
# Application, but excluding the System Libraries of the Combined Work.
#
#   1. Exception to Section 3 of the GNU GPL.
#
#   You may convey a covered work under sections 3 and 4 of this License
# without being bound by section 3 of the GNU GPL.
#
#   2. Conveying Modified Versions.
#
#   If you modify a copy of the Library, and, in your modifications, a
# facility refers to a function or data to be supplied by an Application
# that uses the facility (other than as an argument passed when the
# facility is invoked), then you may convey a copy of the modified
# version:
#
#    a) under this License, provided that you make a good faith effort to
#    ensure that, in the event an Application does not supply the
#    function or data, the facility still operates, and performs
#    whatever part of its purpose remains meaningful, or
#
#    b) under the GNU GPL, with none of the additional permissions of
#    this License applicable to that copy.
#
#   3. Object Code Incorporating Material from Library Header Files.
#
#   The object code form of an Application may incorporate material from
# a header file that is part of the Library.  You may convey such object
# code under terms of your choice, provided that, if the incorporated
# material is not limited to numerical parameters, data structure
# layouts and accessors, or small macros, inline functions and templates
# (ten or fewer lines in length), you do both of the following:
#
#    a) Give prominent notice with each copy of the object code that the
#    Library is used in it and that the Library and its use are
#    covered by this License.
#
#    b) Accompany the object code with a copy of the GNU GPL and this license
#    document.
#
#   4. Combined Works.
#
#   You may convey a Combined Work under terms of your choice that,
# taken together, effectively do not restrict modification of the
# portions of the Library contained in the Combined Work and reverse
# engineering for debugging such modifications, if you also do each of
# the following:
#
#    a) Give prominent notice with each copy of the Combined Work that
#    the Library is used in it and that the Library and its use are
#    covered by this License.
#
#    b) Accompany the Combined Work with a copy of the GNU GPL and this license
#    document.
#
#    c) For a Combined Work that displays copyright notices during
#    execution, include the copyright notice for the Library among
#    these notices, as well as a reference directing the user to the
#    copies of the GNU GPL and this license document.
#
#    d) Do one of the following:
#
#        0) Convey the Minimal Corresponding Source under the terms of this
#        License, and the Corresponding Application Code in a form
#        suitable for, and under terms that permit, the user to
#        recombine or relink the Application with a modified version of
#        the Linked Version to produce a modified Combined Work, in the
#        manner specified by section 6 of the GNU GPL for conveying
#        Corresponding Source.
#
#        1) Use a suitable shared library mechanism for linking with the
#        Library.  A suitable mechanism is one that (a) uses at run time
#        a copy of the Library already present on the user's computer
#        system, and (b) will operate properly with a modified version
#        of the Library that is interface-compatible with the Linked
#        Version.
#
#    e) Provide Installation Information, but only if you would otherwise
#    be required to provide such information under section 6 of the
#    GNU GPL, and only to the extent that such information is
#    necessary to install and execute a modified version of the
#    Combined Work produced by recombining or relinking the
#    Application with a modified version of the Linked Version. (If
#    you use option 4d0, the Installation Information must accompany
#    the Minimal Corresponding Source and Corresponding Application
#    Code. If you use option 4d1, you must provide the Installation
#    Information in the manner specified by section 6 of the GNU GPL
#    for conveying Corresponding Source.)
#
#   5. Combined Libraries.
#
#   You may place library facilities that are a work based on the
# Library side by side in a single library together with other library
# facilities that are not Applications and are not covered by this
# License, and convey such a combined library under terms of your
# choice, if you do both of the following:
#
#    a) Accompany the combined library with a copy of the same work based
#    on the Library, uncombined with any other library facilities,
#    conveyed under the terms of this License.
#
#    b) Give prominent notice with the combined library that part of it
#    is a work based on the Library, and explaining where to find the
#    accompanying uncombined form of the same work.
#
#   6. Revised Versions of the GNU Lesser General Public License.
#
#   The Free Software Foundation may publish revised and/or new versions
# of the GNU Lesser General Public License from time to time. Such new
# versions will be similar in spirit to the present version, but may
# differ in detail to address new problems or concerns.
#
#   Each version is given a distinguishing version number. If the
# Library as you received it specifies that a certain numbered version
# of the GNU Lesser General Public License "or any later version"
# applies to it, you have the option of following the terms and
# conditions either of that published version or of any later version
# published by the Free Software Foundation. If the Library as you
# received it does not specify a version number of the GNU Lesser
# General Public License, you may choose any version of the GNU Lesser
# General Public License ever published by the Free Software Foundation.
#
#   If the Library as you received it specifies that a proxy can decide
# whether future versions of the GNU Lesser General Public License shall
# apply, that proxy's public statement of acceptance of any version is
# permanent authorization for you to choose that version for the
# Library.
#
#

#-------------------------------------------------------------------
# 定义变量，可修改以下变量
# 变量值的优先级：运行脚本的 options 参数 > 环境变量 > 变量默认值
#-------------------------------------------------------------------

# 模块名
project_name="@project.artifactId@"

# 运行包名
project_jar="@project.artifactId@-exec.jar"

# 默认JVM内存参数，如果设置了环境变量 JVM_MEMORY_VARS ，则会被环境变量覆盖
jvm_memory_vars=${JVM_MEMORY_VARS="-Xms3G -Xmx6G -XX:NewRatio=1 -XX:SurvivorRatio=8 -XX:InitialSurvivorRatio=8 -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=512M"}

# 默认JVM非内存参数，如果设置了环境变量 JVM_VARS ，则会被环境变量覆盖
jvm_vars=${JVM_VARS="-server -XX:+PrintCommandLineFlags"}

# 是否打印gc信息
jvm_print_gc=${JVM_PRINT_GC="false"}

# java 远程调试端口。当设置此值时，本 java 应用会监听此端口，提供给开发人员进行连接，从而实现代码级别调试
debug_port=${DEBUG_PORT=""}

# jmx hostname。当设置此值时，本 java 应用会使用本参数设置 jmx 的 hostname。一般设置为本机 ip
jmx_hostname=${JMX_HOSTNAME=""}

# jmx 端口。当设置此值时，本 java 应用会监听此端口，从而对 java 程序进行性能监控
jmx_port=${JMX_PORT=""}

# java 程序生成日志文件的目录，不能随便改
log_path="./logs/"

# 启动后是否自动打印日志，如果设置了环境变量 AUTO_TAIL_LOG，则会被环境变量覆盖
auto_tail_log=${AUTO_TAIL_LOG="true"}

# 启动前是否先删除所有日志文件，如果设置了环境变量 AUTO_TAIL_LOG，则会被环境变量覆盖
rm_log=${RM_LOG="false"}

# 启动 java 的命令是否结合 nohup 进行不挂断运行，如果设置了环境变量 RUN_AS_NOHUP，则会被环境变量覆盖
run_as_nohup=${RUN_AS_NOHUP="true"}

# 当在 docker 环境中，启动 java 报错后，会导致容器退出，可配置此参数，阻止容器退出，便于进入容器调试问题
keep_docker_running=${KEEP_DOCKER_RUNNING="false"}

#-------------------------------------------------------------------
# 以下内容请不要修改
#-------------------------------------------------------------------

pid=''
command=''
CURRENT_SCRIPT_DIR=$(
        cd "$(dirname "$0")"
        pwd
)

print_usage() {
        echo "usage:"
        echo "  run.sh [command] [options]"
        echo ""
        echo "command:"
        echo "  start                     [default command] start the server"
        echo "  stop                      stop the server"
        echo "  status                    status the server"
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
                echo "file 'application.pid' not found, server maybe stopped."
                pid=''
        else
                pid=$(head -n 1 application.pid)
                echo "found pid[${pid}] in file './application.pid'"
                pid="${pid}"
        fi
}

start() {
        check_jdk

        get_pid

        if [ "$pid" != "" ]; then
                if [ -d "/proc/${pid}" ]; then
                        echo "${project_name} is running, pid is ${pid}, can not start repeatedly!"
                        exit 1
                fi
        fi

        echo "try to start ${project_name} ..."
        echo ""

        jvm_params="${jvm_vars} ${jvm_memory_vars}"
        if [ $jvm_print_gc = "true" ]; then
                jvm_params="${jvm_params} -XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:${log_path}/gc.log"
        fi
        if [ -n "$debug_port" ]; then
                jvm_params="${jvm_params} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$debug_port"
        fi
        if [ -n "$jmx_port" ]; then
                jvm_params="${jvm_params} -Djava.rmi.server.hostname=${jmx_hostname} -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=$jmx_port -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
        fi

        cmd="java ${jvm_params} -jar ${project_jar}"
        if [ $run_as_nohup = "true" ]; then
                cmd="nohup $cmd 1>/dev/null 2>&1 &"
        fi

        if [ $rm_log = "true" ]; then
                echo "deleting all log files..."
                eval "rm -rf ${log_path}/*"
        fi

        echo "executing cmd:"
        echo "$cmd"
        eval $cmd
        echo ""

        if [[ $auto_tail_log = "true" ]] && [[ $run_as_nohup = "true" ]]; then
                echo 'log will printing after 5 second using command "tail -f -n 500" automatic.'
                echo 'you can use "ctrl+c" to exit log printing, and will not close the application.'
                echo ''

                sleep 5
                print_log
        else
                if [ $run_as_nohup != "true" ]; then
                        echo "app started, use './run.sh status' to check status"
                fi
        fi

        if [ $keep_docker_running = "true" ]; then
                tail -f /dev/null
        fi
}

stop() {
        echo "try to stop ${project_name} ..."

        get_pid

        if [ "$pid" = "" ]; then
                echo "${project_name} is not running!"
                return 0
        fi

        if [ -d "/proc/${pid}" ]; then
                echo "${project_name} is running, pid is ${pid}"
                kill ${pid}
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
        if [ "$pid" = "" ]; then
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
        if [ ! -d ${log_path} ]; then
                echo "print log error, can not found log folder [${log_path}], please try print log later."
        else
                cd ${log_path}
                filename=$(ls -t | grep ^log | head -n1 | awk '{print $0}')
                tail -f -n 500 $filename
        fi
}

check_jdk() {
        if command -v java >/dev/null; then
                java_version=$(java -version 2>&1 | sed '1!d' | sed -e 's/"//g' -e 's/version//')
                echo "java_version: ${java_version}"
        else
                echo "jdk is not install, please install first!"
                exit 1
        fi
}

health_check() {
        get_pid
        if [ "$pid" = "" ]; then
                echo "${project_name} is not running."
                echo "${project_name} unhealthy"
                exit 1
        else
                if [ -d "/proc/${pid}" ]; then
                        ip_port=$(cat config/application.properties | grep server.port)
                        port=${ip_port##*=}

                        portLastLetter=${port:0-1}
                            case $portLastLetter in
                            [a-z]|[A-Z])
                            ;;
                            [0-9])
                            ;;
                            *)
                                port=${port:0:-1}
                            ;;
                        esac

                        microService=${project_name##*-service-}
                        url="http://localhost:${port}/${microService}/actuator/health"

                        echo $url
                        # http_code=$(curl -I -m 5 -o /dev/null -s -w %{http_code} ${url})
                        resp=$(curl -s --connect-timeout 5 -m 5 $url)
                        echo $resp
                        if [[ "$resp" =~ '"UP"' ]]; then
                                echo "${project_name} health"
                                exit
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

parse_options() {
        TEMP=$(getopt -n "$0" -o hl:d:n:r -l help,auto_tail_log:,debug_port:,print_gc,run_as_nohup:,jmx_hostname:,jmx_port:,rm_log -- "$@")
        if [ $? != 0 ]; then
                echo "command parse error..." >&2
                exit 1
        fi

        eval set -- "$TEMP"
        while true; do
                case "$1" in
                -h | --help)
                        print_usage
                        exit 0
                        ;;
                --print_gc)
                        echo "$1"
                        jvm_print_gc="true"
                        shift
                        ;;
                -r | --rm_log)
                        echo "$1"
                        rm_log="true"
                        shift
                        ;;
                -l | --auto_tail_log)
                        case "$2" in
                        true | false)
                                echo "$1=$2"
                                auto_tail_log=$2
                                shift 2
                                ;;
                        *)
                                echo "error value in option $1, must be true|false"
                                exit 1
                                ;;
                        esac
                        ;;
                -n | --run_as_nohup)
                        case "$2" in
                        true | false)
                                echo "$1=$2"
                                run_as_nohup=$2
                                shift 2
                                ;;
                        *)
                                echo "error value in option $1, must be true|false"
                                exit 1
                                ;;
                        esac
                        ;;
                -d | --debug_port)
                        if [[ $2 -ge 0 ]] && [[ $2 -le 65535 ]] 2>/dev/null; then
                                echo "$1=$2"
                                debug_port=$2
                                shift 2
                        else
                                echo "$1=$2"
                                echo "error value in option $1, must be port range[0-65535]"
                                exit 1
                        fi
                        ;;
                --jmx_hostname)
                        echo "$1=$2"
                        jmx_hostname=$2
                        shift 2
                        ;;
                --jmx_port)
                        if [[ $2 -ge 0 ]] && [[ $2 -le 65535 ]] 2>/dev/null; then
                                echo "$1=$2"
                                jmx_port=$2
                                shift 2
                        else
                                echo "$1=$2"
                                echo "error value in option $1, must be port range[0-65535]"
                                exit 1
                        fi
                        ;;
                --)
                        shift
                        break
                        ;;
                *)
                        echo "Internal error!"
                        exit 1
                        ;;
                esac
        done
        echo ""

        if [ $# -gt 0 ]; then
                command=$1
        fi
}

run() {
        parse_options $*

        cd $CURRENT_SCRIPT_DIR
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
                        echo "illegal command: $1"
                        echo ""
                        print_usage
                        ;;
                esac
        fi
}

run $*
