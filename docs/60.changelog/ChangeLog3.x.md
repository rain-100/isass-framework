# super core

## 更新日志

### super.3.4.0

#### feat

- 重构 net 网络通信模块，新增支持分布式集群情况下的会话信息（标签等）增删改查
- FileUtil 添加 getFileIgnoreCase 方法，可以不分大小写查找目录下的文件
- FileUtil 添加 getDesktopPath 方法，可以获取桌面路径
---

## 更新日志

### super.3.0.3

#### feat

- 新增跨域配置，支持跨域请求

---

### super.3.0.5

#### optimize

- 支持跨域，微服务自动集成跨域功能
- dynamic-datasource 从 3.0.0 升级为 3.2.1

---

### super.3.0.6

#### feat

- 各微服务合成一个单体包后，各微服务的flyway支持独立管理

---