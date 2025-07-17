# isass Framework 模块结构

## 项目概述

isass 全称为 Intelligent System Architecture Service Solution 中文名为 智能系统架构服务解决方案，是一个专注于 技术研发 团队的全生命周期整合解决方案，包括基础设施与中间件、开发框架与流程、运维部署与实施、团队管理与提升。其中后端开发框架是基于 java 以及主流开源框架整合的一套微服务框架，运维部署使用git、tekton、argocd、rancher，实现了全自动CI/CD。

## 模块层次结构

### 第一层：基础设施层（Foundation Layer）

这是整个框架的最底层，提供基础的工具类和通用功能。

```
📁 common/
├── 📦 isass-framework-build          # 构建工具模块
├── 📦 isass-framework-common         # 核心通用模块 ⭐
├── 📦 isass-framework-dependencies   # 依赖管理模块
└── 📦 isass-framework-parent         # 父POM模块
```

**说明：**
- `isass-framework-common` 是整个框架的基础，所有其他模块都依赖它
- 提供日志、工具类、异常处理等基础功能
- 不依赖任何其他ISASS模块

### 第二层：基础服务层（Basic Services Layer）

基于基础设施层，提供序列化、安全、数据库等基础服务。

```
📁 serialization/                     # 序列化模块
├── 📦 isass-framework-serialization-jackson
└── 📦 isass-framework-serialization-protobuf

📁 security/                          # 安全模块
├── 📦 isass-framework-security-core
└── 📦 isass-framework-security-springsecurity

📁 database/                          # 数据库模块
├── 📦 isass-framework-database-core
├── 📦 isass-framework-database-mysql
├── 📦 isass-framework-database-postgresql
├── 📦 isass-framework-database-dameng
├── 📦 isass-framework-database-elasticsearch
├── 📦 isass-framework-database-redis
└── 📦 isass-framework-database-mybatisplus

📁 ioc/                               # 依赖注入模块
└── 📦 isass-framework-springboot-starter
```

**说明：**
- 这些模块依赖 `isass-framework-common`
- 提供企业应用的基础服务能力
- 可以独立使用，也可以组合使用

### 第三层：专业服务层（Professional Services Layer）

基于基础服务层，提供网络通信、消息队列、RPC等专业服务。

```
📁 net/                               # 网络通信模块
├── 📦 isass-framework-net-core
├── 📦 isass-framework-net-netty
├── 📦 isass-framework-net-websocket
├── 📦 isass-framework-net-socketio
├── 📦 isass-framework-net-proxy-server
└── 📦 isass-framework-net-proxy-upstream

📁 mq/                                # 消息队列模块
├── 📦 isass-framework-mq-core
├── 📦 isass-framework-mq-kafka011
├── 📦 isass-framework-mq-ons
└── 📦 isass-framework-mq-spring-event

📁 rpc/                               # 远程调用模块
└── 📦 isass-framework-rpc-okhttp
```

**说明：**
- 这些模块依赖基础服务层和 `isass-framework-common`
- 提供特定领域的专业服务
- 可以根据业务需求选择性使用

### 第四层：Web应用层（Web Application Layer）

基于专业服务层，提供Web应用开发支持。

```
📁 web/                               # Web应用模块
├── 📦 isass-framework-web-springmvc
└── 📦 isass-framework-web-springmvc-starter

📁 nocode/                            # 低代码模块
├── 📦 isass-framework-nocode-core
├── 📦 isass-framework-nocode-generator
└── 📦 isass-framework-nocode-springboot-starter
```

**说明：**
- 提供Web应用开发的基础设施
- 依赖下层所有模块
- 为最终的业务应用提供完整的开发框架 

## 完整模块树形图

```
isass-framework/
├── 📁 common/                        # 基础设施层
│   ├── 📦 isass-framework-build
│   ├── 📦 isass-framework-common     # ⭐ 核心基础模块
│   ├── 📦 isass-framework-dependencies
│   └── 📦 isass-framework-parent
│
├── 📁 serialization/                 # 基础服务层
│   ├── 📦 isass-framework-serialization-jackson
│   └── 📦 isass-framework-serialization-protobuf
│
├── 📁 security/                      # 基础服务层
│   ├── 📦 isass-framework-security-core
│   └── 📦 isass-framework-security-springsecurity
│
├── 📁 database/                      # 基础服务层
│   ├── 📦 isass-framework-database-core
│   ├── 📦 isass-framework-database-mysql
│   ├── 📦 isass-framework-database-postgresql
│   ├── 📦 isass-framework-database-dameng
│   ├── 📦 isass-framework-database-elasticsearch
│   ├── 📦 isass-framework-database-redis
│   └── 📦 isass-framework-database-mybatisplus
│
├── 📁 ioc/                           # 基础服务层
│   └── 📦 isass-framework-springboot-starter
│
├── 📁 net/                           # 专业服务层
│   ├── 📦 isass-framework-net-core
│   ├── 📦 isass-framework-net-netty
│   ├── 📦 isass-framework-net-websocket
│   ├── 📦 isass-framework-net-socketio
│   ├── 📦 isass-framework-net-proxy-server
│   └── 📦 isass-framework-net-proxy-upstream
│
├── 📁 mq/                            # 专业服务层
│   ├── 📦 isass-framework-mq-core
│   ├── 📦 isass-framework-mq-kafka011
│   ├── 📦 isass-framework-mq-ons
│   └── 📦 isass-framework-mq-spring-event
│
├── 📁 rpc/                           # 专业服务层
│   └── 📦 isass-framework-rpc-okhttp
│
├── 📁 web/                           # Web应用层
│   ├── 📦 isass-framework-web-springmvc
│   └── 📦 isass-framework-web-springmvc-starter
│
└── 📁 nocode/                        # Web应用层
    ├── 📦 isass-framework-nocode-core
    ├── 📦 isass-framework-nocode-generator
    └── 📦 isass-framework-nocode-springboot-starter

```

## 模块依赖关系

### 依赖层次

1. **基础设施层** → 无依赖
2. **基础服务层** → 依赖 `isass-framework-common`
3. **专业服务层** → 依赖基础服务层 + `isass-framework-common`
4. **Web应用层** → 依赖所有下层模块

### 关键依赖说明

- `isass-framework-common` 是所有模块的基础依赖
- `isass-framework-serialization-jackson` 被多个模块依赖，提供JSON序列化能力
- `isass-framework-security-core` 提供安全相关的基础功能
- `isass-framework-net-core` 依赖序列化和安全模块，提供网络通信基础

## 使用建议

### Spring Boot之两种引入spring boot maven依赖的方式

方式一：spring-boot-starter-parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.2.1.RELEASE</version>
</parent>
```

- 进入spring-boot-starter-parent里，能够发现它其实通过 parent 的方式依赖了咱们下面要讲的spring-boot-dependencies模块
- 可以通过property覆盖内部的依赖

方式二：使用spring-boot-dependenciesspa

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.2.1.RELEASE</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- 使用这种方式就不用继承父模块，能够解决单继承的问题。这样就能够继承其余父模块，好比本身建立的父模块。
- scope=import，type=pom表示在此pom中引入spring-boot-dependencies的pom的全部内容，注意只能在dependencyManagement中使用。
- 不过这种方式不能使用property的形式覆盖原始的依赖项。如需要改写定义好的版本号，要在dependencyManagement里面的spring-boot-dependencies之前添加依赖的东西
- 大多数咱们可能用到的包依赖和插件依赖都已经在spring-boot-dependencies中定义好了

> 综上所述，继承 spring-boot-starter-parent 适合于单模块项目或者已经采用 Spring Boot 的项目，而使用 dependencyManagement 元素适合于多模块项目或者需要更灵活依赖管理的场景。

### 版本定义参数

https://maven.apache.org/maven-ci-friendly.html


### 最小化使用
如果只需要基础功能，可以只引入：
- `isass-framework-common`

### 数据库应用
对于数据库应用，建议引入：
- 具体的数据库模块（如 `isass-framework-database-mysql`）

### 完整Web应用
对于完整的Web应用，建议引入：
- 所有基础服务层模块
- 根据需求选择专业服务层模块
- Web应用层模块

### 微服务应用
对于微服务应用，建议引入：
- 基础服务层模块
- 网络通信模块
- 消息队列模块
- RPC模块

## 版本信息

- **当前版本**: 4.0.0-SNAPSHOT
- **Java版本**: 21
- **Spring Boot版本**: 3.5
- **许可证**: GNU Lesser General Public License Version 3 
