# isass-nocode-core

标准 CRUD 与统一执行。提供 createBatch/update/delete/superCud、page/cursorPage/count/exists，以及可选树查询。

- 写入经 CrudWriteExecutor 事务与生命周期；查询经 CrudQueryExecutor。关联只按声明处理，MERGE 保留旧项、REPLACE 移除未提交项。
- 关联可递归保存；外键目标在依赖方生命周期前保存并回填真实 ID。嵌套增改删分别经过功能授权与生命周期，ID-only 引用不重复修改。REPLACE/级联先解析实际子记录 ID，再按 ID 删除；根关联更新加事务记录锁，失败整批回滚。
- EntityAssociation 只描述关联结构，不提供可写/只读开关；关联对象的新增、修改、删除分别校验目标操作权限，不继承主对象权限。where(field,value) 为关联读取、写入归属和删除增加不可覆盖的固定条件（按目标 Java 属性类型转换）。共享目标不声明 cascadeDelete，不随绑定移除而删除。
- ICrudService/ILocalCrudService 的 E 仅要求 IEntity，Criteria 不强制 IIdCriteria。无主键实体支持新增、普通查询/分页/统计，以及带有效条件或 matchFields 的修改、带有效条件的删除；不允许无条件写入。
- ID 是可选能力：getById/existsById、按 ID 修改/删除、updateAllColumns、deleteIds、ID 游标/树查询在运行时要求 IIdEntity。不支持的操作在数据库写入前明确拒绝；无主键条件删除直接执行 Criteria。loadRelated 目标去重/分页、嵌套关联写入及级联根删除仍依赖 ID，不以猜测主键代替能力校验。
- SuperCudReq<E, C, PK> 不限制 E 必须有主键；deleteIds 为 List<PK>，入口解析泛型后完成反序列化，不由业务生命周期转换 ID。无主键服务可用 Serializable 占位，不提交 deleteIds。JSON 结构不变，Long 主键可用字符串精确传输。
- HTTP：/{serviceName}/nocode/{contextName}/{resourceName}/{operationName}。参数用 Java camelCase；loadRelated 展开关联，returnFields 控制读取投影。标量集合 Query 使用英文逗号分隔，按属性定义解析，不受 lambda/varargs 便捷重载影响。
- 生命周期承接业务约束；数据库一致性写入放在事务内，缓存和通知放在 afterCommit。直接 Repository/自定义 SQL 需自行接入授权。
- beforeQuery 使用 context.addReadRestriction 追加服务器构造的 AND 约束，不修改请求 Criteria；统一执行器按查询对象打开、恢复 TrustedReadContext，覆盖分页、游标、统计与树查询。不能把客户端条件搬入可信列表；固定租户边界及输出字段保护仍独立执行。
- 入口与嵌套写入的权限执行器在调用时解析；注册入口时不提前加载依赖入口目录的授权组件，避免启动循环依赖，授权异常仍中止调用。
- security 包的 DataReadProjection 处理关联读取的内部补键，配合 core-common 隐藏字段；读取和租户策略契约来自 isass-security-core。
- TenantReadLifecycleListener 为读取建立固定隔离：ITenantEntity 限制当前 tenantId，无租户时拒绝租户数据；TenantReadPolicyProvider 可补关联边界及可信平台豁免。仅内部主体的后台调用保留内部访问语义，带业务主体的 HMAC 调用仍受隔离；此机制不代替写入校验。

## 静态数据初始化

启动默认扫描 `classpath*:init/**/*` 中的 `.json/.xlsx/.csv`，忽略其他文件及 Excel 临时文件 `~$*`，按资源路径顺序处理。保留 `isass.nocode.initialization.enabled/location` 配置；统一汇总失败实体并继续后续实体，文件解析失败也记录错误后继续其他文件。显式配置 `*.json` 只加载 JSON，使用表格时需改为覆盖相应格式的路径。

- JSON 为 `{ "实体资源名": [记录...] }`；Excel 每个 sheet 使用实体资源名（如 `dict`、`directory`），不是 Java 类名或表名。首行是 Java camelCase 字段名，第二行固定为字段备注，第三行起每行一条记录；直接跳过前两行，不校验备注内容，空模板不产生记录。按目标服务分组，同一服务内按 sheet 顺序处理，依赖表放在前面。整个文件先完成格式解析，再进入原有导入流程。
- CSV 一文件一实体，文件名为 `业务标识--实体资源名.csv`，取最后一个 `--` 后的名称；也接受 `实体资源名.csv`。UTF-8，兼容 BOM；Apache Commons CSV 按 RFC4180 处理逗号、双引号及字段内换行。前两条逻辑记录分别为字段名、备注，第三条起为数据，不能简单按物理行拆分。模板和数据导出均包含两条表头、使用 UTF-8 BOM。CSV 没有文本列格式，不应使用 Excel 打开再保存长 ID 数据，避免精度损失。
- 单元格支持文本、数字、布尔值。ID/外键等长整数应先设为“文本”再输入；超过 15 位有效数字的数值单元格拒绝导入，避免静默损失精度。日期使用文本，时间戳使用毫秒整数。
- 表格按本地或已注册远程 API 实体字段类型解析：集合、数组、Map、JSON 节点及自定义结构字段直接填写 JSON 值，无需 `json:`；兼容结构字段原有前缀写法。普通 String 保留原文，不猜测 `null`、`[]`、`{}` 的含义；枚举填写实际编码。JSON 文件及 `importData` 继续使用原生 JSON，无需前缀。
- 空行/记录忽略，空单元格不提交字段；`json:""` 为显式空字符串，`json:null` 为显式空值。保留标记本身可用 JSON 字符串转义，例如 `json:"json:null"` 表示文本 `json:null`。CSV 数据导出会转义空字符串、保留前缀及可能被电子表格当作公式执行的文本，导入后还原原文。
- 不允许重复/空字段名、没有表头的数据列；Excel 还禁止合并单元格、公式或错误值。错误包含 sheet/单元格，或 CSV 文件名/记录及列位置；CSV 语法错误包含文件名及解析位置。切换格式时移除旧的同源文件，并清理构建输出，避免同时加载。
- 启动初始化、JSON 请求、文件上传共用 `NocodeInitializationImporter` 解析实体归属、按服务分组和汇总结果，再通过 `INocodeInitializationService.importData(serviceName, document)` 执行。本地实现直接入库，远程实现调用目标服务同名 HTTP 接口；执行实现不再跨服务分发。请求路径的 serviceName 表示接收服务，不限制文档内实体归属；已到达实体所属服务但缺少本地实现时，记录失败，不再转发回自身。未知/歧义实体、远程错误均不阻断其他实体或服务，不自动重试。
- 仍按 ID 只插入不存在的记录，同批重复 ID 跳过，已有数据不更新；不重放交互式 CRUD 生命周期，不提供整个文件的事务回滚。初始化不是数据更新或覆盖工具。结果 `total` 为提交记录数，`inserted/skipped` 为实际新增/跳过数，`failures` 按实体记录失败原因；失败实体可能已部分写入，不能仅凭 HTTP 200 或 `success=true` 判断全部成功。
- `POST /{serviceName}/nocode/system/initialization/importData` 接收原 JSON 对象；`POST /{serviceName}/nocode/system/initialization/importFile` 接收 `multipart/form-data` 的 `file` 字段，支持 `.json/.xlsx/.csv`，与启动初始化共用 `importer.readDocument(InputStream, filename)` 解析本地及远程 API 字段类型。空文件、格式不支持或无法解析时返回 HTTP 400 且不入库；缺少文件同样返回 400。DataService 的同名读取方法使用本地类型，`readDocument(InputStream)` 保留 JSON 兼容。接口仍受既有安全规则和上传大小限制约束，未新增匿名访问或自动授权。
- `GET /{serviceName}/nocode/system/initialization/downloadTemplate?entities=dict,directory` 下载空白 `.xlsx`，也支持重复的 `entities` 参数。按入参顺序生成 sheet，同名去重；缺少实体、未知/归属不唯一、无法解析类型及 Excel 不支持的 sheet 名返回 HTTP 400。直接读取本地或已注册远程 API 实体类型，不查询业务数据或调用远程服务。字段含继承字段，排除 static/transient/JsonIgnore、associations 关联属性及 createUserId/createUserName/createTime/modifyUserId/modifyUserName/modifyTime 六个审计字段，其他字段不因此排除。第二行取字段 ApiDoc.description，无注解时回退字段名，并附布尔、枚举编码和 JSON 填写提示；使用浅紫底色 #EEE9F6、文字 #393448、Arial 11、左对齐、垂直居中及自动换行，行高 72。保留 Excel 网格线，前两行添加浅灰细边框，冻结前两行。数据行使用 Excel 默认样式，不单独设置单元格样式或行高；Long/long/BigInteger 等列仅保留列级文本格式，不再追加长整数填写提示；已有初始化权限策略不变。
- `GET /{serviceName}/nocode/system/initialization/entities` 一次返回本地与已注册远程 NoCode API 模型，包含 serviceName、contextName、entity、comment，按服务/上下文/实体排序、同一路径去重。不查询业务表、不调用远程服务；不自动发现未注册的其他微服务。调用方可本地搜索和分类，无需逐次请求。
- `downloadTemplate?entities=dict&format=csv` 下载 `初始化模版--dict.csv`；`exportData?entities=dict&format=csv` 导出实体已有数据，文件名 `初始化数据--dict.csv`。CSV 两个下载操作每次只接受一个去重后的实体，多实体返回 HTTP 400，由调用方分别下载；字段、备注与 Excel 模板共用元数据，数据导出也排除六个审计字段。原多实体 XLSX 模板和不带 format 的 JSON 数据导出保留；非法格式返回 HTTP 400。
- 数据导出由 Importer 按实体归属分组，本地直接读取，远程经 `INocodeInitializationService.exportData(serviceName, entities)` 调用目标服务同名 GET 接口；远程请求保留通用认证/HMAC 头，目标服务本地执行，不转发回自身。CSV 通过已注册 API 类型还原远程 JSON 行，保留长 ID；未知/歧义实体拒绝导出，远程失败或返回不完整时不返回部分数据。接口沿用既有授权约束，元数据可见不代表获得数据导出权限。
- 字段标注 `JsonProperty.Access.WRITE_ONLY` 时允许导入，CSV 导出该列为空，JSON 由序列化保护；模版保留可填写列，不导出密钥值。

[返回模块目录](../README.md)

- 树查询与写入祖先遍历仅以 NULL 为根；过滤结果缺少父节点时作为展示根并保留真实 parentId。
