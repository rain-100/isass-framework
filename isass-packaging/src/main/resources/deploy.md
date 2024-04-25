# 部署文档

- 本程序是 `@service-name-cn@`，基于 java、SpringCloud、mybatis-plus 开发。

## 前置条件

- 已安装 jdk8 或以上
- 已有 mysql5.7 或以上

## 创建数据库

- 启动服务时，程序会自动读取配置文件的 `spring.datasource.dynamic.datasource.master.url`, 判断数据库是否存在，不存在则自动创建数据库。

## 拷贝文件

- 在服务器创建文件夹：/opt/@service-name@
- 若服务器已存在部署包，建议备份原部署包
- 将压缩包 @project.artifactId@-bin.tar.gz 拷贝到服务器 /opt/@service-name@
- 进入服务器文件夹 cd /opt/@service-name@
- 解压文件。解压文件前，请认真查看服务器已有配置文件与压缩包内文件，不要在解压时，覆盖掉服务器上已经 `正确配置` 过的配置文件，避免重新更改配置
- tar -zxvf @project.artifactId@-bin.tar.gz

## 修改参数

- 首次部署时，根据现场实际情况修改 application.properties 文件的参数

> vi application.properties

## 启动服务

### linux系统

- `./run.sh`

### windows系统

#### 命令行窗口运行

-双击 `winstart.bat`

#### windows系统服务

1. 双击`win_service_install.bat`，安装windows服务
2. 双击`win_service_start.bat`，运行服务。或者打开 `windows服务`，找到 `@project.artifactId@`进行操作

## 查看日志

- linux 环境启动服务后，会自动使用 tail 命令打开日志
- 输出以下日志，视为启动成功

```text
Tomcat started on port(s): xxx (http) with context path ''
Started xxxApp in 20.82 seconds (JVM running for 21.431)
```

## 停止服务

### linux 系统

- `./stop.sh`

### windows系统

#### 命令行窗口运行

- 关闭命令行窗口即可

#### windows 系统服务

- 双击 `win_service_stop.bat`。或者打开 `windows服务`，找到 `@project.artifactId@`进行操作
- 卸载 windows 服务：双击`win_service_uninstall.bat`

## 环境变量

> 在jar包直接运行时，改变 `run.sh` 的变量值, 或在 docker 环境运行时设置环境变量，可实现不同功能

### 变量列表


| 变量                | 默认值                                                                            | 说明                                                                                                   |
| --------------------- | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| JVM_VARS            | -server -XX:SurvivorRatio=8 -XX:InitialSurvivorRatio=8 -XX:+PrintCommandLineFlags | JVM非内存参数                                                                                          |
| JVM_MEMORY_VARS     | -Xms6G -Xmx6G -Xmn3G -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=256M             | JVM内存参数                                                                                            |
| JVM_PRINT_GC        | false                                                                             | 是否打印gc信息                                                                                         |
| DEBUG_PORT          |                                                                                   | java 远程调试端口。当设置此值时，本java应用会开启此端口，提供给开发人员进行连接，从而实现代码级别调试  |
| AUTO_TAIL_LOG       | true                                                                              | 启动后是否自动使用 tail 命令打印日志                                                                   |
| RUN_AS_NOHUP        | true                                                                              | 启动 java 的命令是否结合 nohup 进行不挂断运行                                                          |
| KEEP_DOCKER_RUNNING | false                                                                             | 当在 docker 环境中，启动 java 报错后，会导致容器退出，可配置此参数，阻止容器退出，便于进入容器调试问题 |
| WRITE_LOG_TO_FILE   | true                                                                              | 是否使用slf4j把程序日志写到日志文件                                                                    |

### 容器环境变量默认值差异列表

> 只列出与jar包直接运行时默认值 `不同` 的变量


| 变量              | 默认值 | 差异说明                                              |
| ------------------- | -------- | ------------------------------------------------------- |
| JVM_MEMORY_VARS   |        | 容器环境应该使用 cgroup 限制内存，不通过 jvm 参数限制 |
| AUTO_TAIL_LOG     | false  | 容器环境应该使用 docker logs 查看日志                 |
| RUN_AS_NOHUP      | false  | 容器环境的 java 程序无需后台运行                      |
| WRITE_LOG_TO_FILE | false  | 容器通过控制台收集日志，无需写日志文件                |
