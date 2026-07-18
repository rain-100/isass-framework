# Assembly 使用文档 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为服务项目维护者提供可直接接入标准 Assembly 部署包的框架使用文档。

**Architecture:** 在 `docs/usage/build/` 新增独立文档，说明父 POM 已绑定的 Assembly 执行、boot 模块所需的 `isass-core-build` 插件依赖、标准产物结构和运行入口。文档以 `bsp-boot` 的真实配置和 `isass-core-build` 的 `assembly` 描述符为唯一依据，不要求项目维护自定义部署清单。

**Tech Stack:** Maven Assembly Plugin 3.7.1、Spring Boot Maven Plugin、Markdown。

## Global Constraints

- 文档必须位于 `docs/usage/build/assembly.md`。
- 项目只通过 `isass-core-build` 提供的 `assembly` descriptorRef 打包，不创建 `src/assembly/deployment.xml`。
- POM 示例必须与修正后的 `bsp-boot/pom.xml` 的项目依赖和插件依赖一致。
- 部署包结构、文件名和脚本必须与 `isass-core-build/src/main/resources/assemblies/assembly.xml` 一致。
- Linux 与 Windows 运行说明以部署包内 `deploy.md` 为运维细节的权威来源。

---

### Task 1: 编写 Assembly 接入与运行文档

**Files:**
- Create: `docs/usage/build/assembly.md`
- Reference: `isass-core-dependencies/pom.xml:1110-1127`
- Reference: `isass-core-build/src/main/resources/assemblies/assembly.xml:171-267`
- Reference: `isass-core-build/src/main/resources/deploy.md`
- Reference: `/Users/rain/a/code/company/isass/isass-service-bsp/bsp-boot/pom.xml:55-65`

**Interfaces:**
- Consumes: 父 POM 绑定的 `assembly` descriptorRef 和 boot 模块的 `isass-core-build` 插件依赖。
- Produces: 服务项目维护者可复制的 Maven 配置、打包命令、部署包目录结构和最小运行命令。

- [ ] **Step 1: 创建使用文档并写明框架职责与接入配置**

在文档开头说明 `isass-core-build` 通过 Assembly 描述符提供标准 `tar.gz` 部署包：复制 `*-exec.jar`、编译后的 `config/`、`lib/`、`git.properties`、Linux/Windows 脚本及 `deploy.md`。说明父 POM 已在 `package` 阶段执行 `maven-assembly-plugin` 的 `assembly` 描述符，并在 boot 模块中同时添加项目依赖和插件依赖：

```xml
<dependencies>
    <dependency>
        <groupId>vip.isass</groupId>
        <artifactId>isass-core-build</artifactId>
    </dependency>
</dependencies>

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
```

紧接着明确：项目不创建或维护 `src/assembly/deployment.xml`，也不需要重复配置 execution、descriptorRef 和 descriptor；框架父 POM 与 `isass-core-build` 负责该约定。项目依赖供 Assembly 收集资源，插件依赖供 Maven 加载描述符，二者都不可省略。

- [ ] **Step 2: 写明打包命令与产物结构**

新增 `mvn -pl <boot-module> -am package` 示例，并说明生成文件为 `<boot-artifactId>-bin.tar.gz`。列出与标准 Assembly 描述符一致的目录：

```text
<boot-artifactId>/
├── <boot-artifactId>-exec.jar
├── config/
├── lib/
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

说明 `config/` 来自 boot 模块编译输出、`lib/` 来自 `target/lib`，Windows 文件可在 Linux 环境中忽略。

- [ ] **Step 3: 写明最小运行方式与注意事项**

写入 Linux 解压、配置、授权与启动步骤：

```bash
tar -zxvf <boot-artifactId>-bin.tar.gz
cd <boot-artifactId>
chmod +x run.sh stop.sh printlog.sh
./run.sh start
./run.sh status
./run.sh health
```

写入 Windows 入口：`win_start.bat` 用于前台启动；按 `win_service_install.bat`、`win_service_start.bat` 的顺序安装并启动服务；停止和卸载使用相应脚本。提示完整 JVM 参数、日志、健康检查和生产守护策略以压缩包中的 `deploy.md` 为准，并提示升级时受控合并生产 `config/application.yml`。

- [ ] **Step 4: 校验文档与实际 Assembly 清单一致**

运行：

```bash
rg -n "isass-core-build|maven-assembly-plugin|deployment.xml|bin.tar.gz|run.sh|win_service_install.bat" docs/usage/build/assembly.md
git diff --check
```

预期：文档包含标准插件依赖、禁止自定义部署清单、包名、Linux/Windows 入口；`git diff --check` 无输出并返回 0。

- [ ] **Step 5: 提交文档**

```bash
git add docs/usage/build/assembly.md
git commit -m "docs: add assembly usage guide"
```

## Self-Review

- Spec coverage: Task 1 覆盖框架职责、BSP 一致的接入配置、打包命令、产物结构、Linux/Windows 运行方式和不自定义部署清单的约束。
- Placeholder scan: 本计划不含 `TBD`、`TODO` 或未定义实现步骤。
- Type consistency: 文档名称、descriptorRef、artifactId 占位符和命令在所有步骤中一致。
