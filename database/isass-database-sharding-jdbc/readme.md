 ## 数据库中间件 sharding-jdbc
 ### 不支持集合类型的 insert update 操作 
 - MasterSlaveConnection 继承了 AbstractUnsupportedOperationConnection, 不支持 Collection<String> 的 insert update 操作。
 持续留意 sharding-jdbc, 有无对其支持 