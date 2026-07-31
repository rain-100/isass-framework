# Isass V4 部署说明

`@service-name-cn@` 是基于 Java 25 与 Spring Boot 4 的 Isass V4 服务。标准部署包由 boot 模块在 `package` 阶段生成，文件名为
`@project.artifactId@-bin.tar.gz`。

## 部署包结构

```text
@project.artifactId@/
├── @project.artifactId@-exec.jar
├── config/
│   └── application.yml
├── lib/
├── deploy.md
├── run.sh
├── stop.sh
└── printlog.sh
```

Windows 部署包还包含 `win_start.bat`、Windows 服务安装/启动/停止/卸载脚本及 `JavaService.exe`。

## 前置条件

- 安装与 Isass V4 编译版本兼容的 JDK 25，并确保 `java` 位于 `PATH`。
- 按服务的 `config/application.yml` 配置并确保 MySQL、Redis 等依赖可访问。
- 数据库结构由 Liquibase 管理；部署前应由运维或 DBA 创建数据库和账户，并授予服务账户建表、变更表结构所需的权限。服务不会替代数据库账户、备份和权限管理。

## Linux 部署

1. 建议为每个服务创建独立目录，例如 `/opt/@project.artifactId@`。
2. 备份该目录中已有的 `config/application.yml` 和部署包。
3. 将 `@project.artifactId@-bin.tar.gz` 上传到该目录并解压：

   ```bash
   cd /opt/@project.artifactId@
   tar -zxvf @project.artifactId@-bin.tar.gz
   cd @project.artifactId@
   ```

4. 首次部署或配置变更时，修改 `config/application.yml`。敏感配置应通过部署系统注入或受控配置文件提供，不能提交到代码仓库。
5. 赋予脚本执行权限并启动：

   ```bash
   chmod +x run.sh stop.sh printlog.sh
   ./run.sh start
   ```

常用命令：

```bash
./run.sh status
./run.sh health
./run.sh log
./run.sh stop
```

`./run.sh` 默认以 `nohup` 后台运行并自动跟随日志；按 `Ctrl+C` 仅退出日志查看，不会停止服务。生产环境建议由
systemd、容器编排平台或其他进程守护工具管理服务生命周期。

## Windows 部署

- 临时以前台方式启动：双击 `win_start.bat`。
- 安装为 Windows 服务：依次执行 `win_service_install.bat`、`win_service_start.bat`。
- 停止或卸载：执行 `win_service_stop.bat`、`win_service_uninstall.bat`。

Windows 服务名称为 `@project.artifactId@`。

## 运行参数

Linux 启动脚本支持通过环境变量调整 JVM 与运行行为；命令行选项优先于环境变量。

| 变量                        | 默认值                                                  | 说明                                                      |
|-----------------------------|---------------------------------------------------------|-----------------------------------------------------------|
| `JVM_MEMORY_VARS`           | 主机：`-Xms3G -Xmx6G`；容器：按 `MaxRAMPercentage=88.0` | 覆盖 JVM 内存参数。应按实际容器或主机内存设置。           |
| `JVM_VARS`                  | `-server -XX:+PrintCommandLineFlags`                    | JVM 非内存参数。                                          |
| `JVM_PRINT_GC`              | `false`                                                 | 是否输出 GC 日志。                                        |
| `DEBUG_PORT`                | 空                                                      | 开启 JDWP 远程调试端口。仅限受访问控制的排障环境。        |
| `JMX_HOSTNAME` / `JMX_PORT` | 空                                                      | 开启 JMX 监控。启用时必须通过网络策略保护端口。           |
| `WRITE_LOG_STDOUT`          | `true`                                                  | 打印日志到控制台。                                        |
| `WRITE_LOG_TO_FILE`         | `false`                                                 | 打印日志到日志文件。                                      |
| `AUTO_TAIL_LOG`             | `true`                                                  | 后台启动后是否自动跟随日志。仅日打印志到控制台时有意义    |
| `RUN_AS_NOHUP`              | `true`                                                  | 是否使用 `nohup` 在后台运行。容器中通常设为 `false`。     |
| `RM_LOG`                    | `false`                                                 | 启动前删除 `logs/` 下已有日志。生产环境通常保持 `false`。 |
| `KEEP_DOCKER_RUNNING`       | `false`                                                 | 容器排障时 java 启动失败后保持容器运行。                  |

示例：

```bash
JVM_MEMORY_VARS="-Xms1G -Xmx1G" AUTO_TAIL_LOG=false ./run.sh start
DEBUG_PORT=5005 ./run.sh start
```

## 验证与排障

- 使用 `./run.sh status` 确认进程状态，使用 `./run.sh health` 调用 Actuator 健康检查。
- 使用 `./run.sh log` 查看最新应用日志；日志目录为 `logs/`。
- 发生启动失败时，保留 `config/application.yml`、应用日志和 `git.properties`，用于确认部署版本与配置。
- 不要在解压升级时直接覆盖已确认的生产配置；应先比对新包中的配置模板，再受控合并。
