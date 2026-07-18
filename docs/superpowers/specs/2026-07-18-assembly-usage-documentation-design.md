# Assembly 使用文档设计

## 目标

在框架 `docs/usage/` 中新增面向服务项目维护者的 Assembly 使用文档，说明
`isass-core-build` 提供的标准部署包能力，以及 boot 模块如何启用该能力。

## 范围

- 说明父 POM 已将 `maven-assembly-plugin` 的 `assembly` 描述符绑定到 `package` 阶段。
- 给出与修正后的 `bsp-boot` 一致的项目依赖和插件依赖配置。
- 说明项目不维护 `src/assembly/deployment.xml`，统一复用 `isass-core-build` 的部署清单。
- 说明 `mvn -pl <boot-module> -am package` 的打包方式和生成文件名。
- 展示标准部署包目录：可执行 JAR、`config/`、`lib/`、脚本、`git.properties` 与部署说明。
- 说明 Linux 和 Windows 的启动、停止、日志与服务化入口，并链接压缩包内 `deploy.md` 获取完整运维参数说明。

## 非目标

- 不修改 Maven 构建逻辑、Assembly 描述符或启动脚本。
- 不要求业务项目定制 Assembly 描述符。
- 不复制完整的环境变量与运维细节；这些由部署包内 `deploy.md` 维护。

## 文档位置与结构

新增 `docs/usage/build/assembly.md`，章节依次为：

1. 适用范围与 `isass-core-build` 职责。
2. 项目接入条件与 BSP boot 模块配置示例。
3. 打包命令与产物位置。
4. 部署包目录结构及各文件职责。
5. Linux、Windows 的最小运行步骤。
6. 不自定义部署清单的约束与常见注意事项。

## 验收标准

- 项目维护者可以复制插件配置并生成标准部署包。
- 文档准确说明描述符、产物名、目录及脚本来源。
- 文档明确项目无需维护自定义部署清单。
- 示例和术语与 `bsp-boot`、`isass-core-build` 的实际实现一致。
