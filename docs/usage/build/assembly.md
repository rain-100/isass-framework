# 标准部署包 Assembly 使用说明

本文面向服务项目维护者，说明 boot 模块如何使用 `isass-core-build` 生成统一的服务部署包。

## `isass-core-build` 做了什么

框架父 POM 已将 `maven-assembly-plugin` 绑定到 `package` 阶段，并使用名为
`assembly` 的标准部署清单。boot 模块同时引入 `isass-core-build` 作为项目依赖和 Assembly 插件依赖后，
框架会生成一个 `tar.gz` 部署包，统一收集：

- Spring Boot 重打包生成的 `*-exec.jar`；
- boot 模块编译输出中的 `config/`；
- `target/lib` 中的额外运行库；
- `git.properties` 版本信息；
- Linux 启动、停止和日志脚本；
- Windows 前台启动、服务安装/启动/停止/卸载脚本及 `JavaService.exe`；
- 随包分发的 `deploy.md` 运维说明。

服务项目只复用该标准清单，**不创建也不维护** `src/assembly/deployment.xml`，也不需要重复配置
Assembly 的 execution、`descriptorRef` 或 descriptor 路径。

## 项目接入

在可执行服务的 boot 模块中配置 `spring-boot-maven-plugin` 生成带 `exec` classifier 的可执行 JAR，
并将 `isass-core-build` 同时声明为项目依赖和 `maven-assembly-plugin` 插件依赖。项目依赖供
Assembly 的 `dependencySet` 收集脚本资源，插件依赖供 Maven 加载 `assembly` 描述符。`bsp-boot`
使用的配置如下：

```xml
<dependencies>
    <dependency>
        <groupId>vip.isass</groupId>
        <artifactId>isass-core-build</artifactId>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>repackage</id>
                    <configuration>
                        <classifier>exec</classifier>
                    </configuration>
                    <goals>
                        <goal>repackage</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <dependencies>
                <dependency>
                    <groupId>vip.isass</groupId>
                    <artifactId>isass-core-build</artifactId>
                    <version>${isass.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

`maven-assembly-plugin` 的版本、`package` 阶段执行和 `assembly` descriptorRef 均由框架父 POM
提供。业务项目无需新增任何 Assembly execution，但两个 `isass-core-build` 声明都不可省略。

## 打包与产物

在服务工程根目录执行，其中 `<boot-module>` 为 boot 模块的 artifactId：

```bash
mvn -pl <boot-module> -am package
```

例如 BSP：

```bash
mvn -pl bsp-boot -am package
```

部署包位于 boot 模块的 `target/` 目录，文件名为：

```text
<boot-artifactId>-bin.tar.gz
```

解压后的标准结构如下：

```text
<boot-artifactId>/
├── <boot-artifactId>-exec.jar
├── config/
├── lib/                         # 可选：仅在 target/lib 存在文件时生成
├── git.properties
├── deploy.md
├── run.sh
├── stop.sh
├── printlog.sh
├── win_start.bat
├── win_service_install.bat
├── win_service_start.bat
├── win_service_stop.bat
├── win_service_uninstall.bat
└── JavaService.exe
```

- `config/` 来自 boot 模块的编译资源输出，通常包含 `application.yml`。
- `lib/` 来自 boot 模块的 `target/lib`，用于放置需要随包分发的额外运行库；目录为空或不存在时不会进入部署包。
- `git.properties` 用于定位构建版本与提交信息。
- Windows 文件在 Linux 部署时可忽略。

## 运行方式

部署前先解压部署包，并按环境调整 `config/application.yml`。生产配置应受控管理；升级时不要直接覆盖已确认的生产配置。

### Linux

```bash
tar -zxvf <boot-artifactId>-bin.tar.gz
cd <boot-artifactId>
chmod +x run.sh stop.sh printlog.sh
./run.sh start
./run.sh status
./run.sh health
```

常用命令：

```bash
./run.sh log
./run.sh stop
./printlog.sh
```

### Windows

- 临时前台运行：执行 `win_start.bat`。
- 安装为 Windows 服务：依次执行 `win_service_install.bat`、`win_service_start.bat`。
- 停止或卸载服务：执行 `win_service_stop.bat`、`win_service_uninstall.bat`。

完整的 JVM 参数、日志路径、健康检查、容器运行建议和生产环境守护策略，以部署包内的
`deploy.md` 为准。
