# isass-entrypoint-core

服务入口契约。定义 IEntrypoint、入口注解、参数来源及访问策略。

- 仅 @EntrypointOperation 标注的方法形成外部入口；有业务主体的自定义用例优先扩展对应实体服务。
- `FileStream` 单次消费，支持 `writeTo` / `openInputStream`；持有远端资源的返回值须消费或显式关闭，消费及取消会释放源资源，关闭后不能再次消费。

[返回模块目录](../README.md)
