# 源码 License Header 规范化

工作区统一使用 `scripts/normalize_license_headers.py` 对六个关联项目的源码头进行保守规范化。脚本仅使用 Python 标准库，默认是只读 dry-run；只有显式传入 `--apply` 才会写文件。

## 项目许可证

| 项目 | SPDX | 默认源码范围 |
| --- | --- | --- |
| `bsp-web-vue` | `Apache-2.0` | `src/`、`e2e/` |
| `isass-framework-v4` | `LGPL-3.0-only` | 各模块 `src/` |
| `isass-service-apidoc` | `LGPL-3.0-only` | 各模块 `src/` |
| `isass-service-bsp` | `LGPL-3.0-only` | 各模块 `src/` |
| `iimage-asset-h5` | `LGPL-3.0-only` | `src/`、`tests/` |
| `iimage-service-asset` | `LGPL-3.0-only` | 各模块及 OpenClaw 插件 `src/` |

仓库没有可安全推断的统一项目 Copyright holder，因此目标短头只包含 SPDX，不创建新的版权主体。`bsp-web-vue` 的 Geeker-Admin 来源、作者和 Apache-2.0 元数据必须保留。

## 使用方式

从 `isass-framework-v4` 根目录执行：

```bash
# 默认 dry-run 当前框架仓库
python3 scripts/normalize_license_headers.py

# dry-run 全部六个项目
python3 scripts/normalize_license_headers.py --all-projects --dry-run

# 显式写入全部项目
python3 scripts/normalize_license_headers.py --all-projects --apply

# CI 检查；存在可安全规范化的文件时返回非 0
python3 scripts/normalize_license_headers.py --all-projects --check

# 检查指定项目、目录或文件
python3 scripts/normalize_license_headers.py --project isass-service-bsp bsp-api/src/main/java

# 审阅 diff 或生成 JSON 报告
python3 scripts/normalize_license_headers.py --all-projects --show-diff
python3 scripts/normalize_license_headers.py --all-projects --report license-report.json
```

`--include` 用于覆盖项目默认源码入口，`--exclude` 用于追加排除 glob。POM、LICENSE、NOTICE、README 和 lockfile 默认不参与源码规则；确需检查 POM 时必须把文件作为显式参数传入。

## 安全边界

脚本会保持 UTF-8 BOM 和现有换行格式，并默认排除依赖、构建输出、vendor、third-party 和 generated 目录。空文件、非 UTF-8、minified/bundle、生成标记文件、不同 SPDX 表达式、多版权主体及无法识别的法律信息都不会自动改写。

框架中的以下内容按第三方或生成产物处理：

- `isass-apidoc-openapi3/frontend/`、打包后的 `META-INF/resources/` 及 `LICENSE-KNIFE4J`；
- `isass-net-socketio/src/main/resources/static/socket.io.js` 的 MIT 与 Guillermo Rauch 版权声明；
- Protobuf 生成的 `NetworkFrame.java`。

NoCode 生成器的 Java/XML SPDX 输出分别由 `segment/copyright.ftl` 和 `segment/copyright.xml.ftl` 持有。修改生成规则时先更新模板，再处理或重新生成消费者。

## 测试

```bash
python3 -m unittest discover -s tests/license_headers -v
```

测试覆盖缺失 Header、已规范 SPDX、完整 LGPL、位置异常、第三方和多版权、其他或重复 SPDX、生成文件、普通注释、BOM、CRLF、shebang、Python encoding、XML declaration、Vue/FreeMarker 注释格式及幂等性。
