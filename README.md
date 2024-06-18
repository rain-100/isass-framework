# super core

## 更新日志

 更新日志请查看 docs/changelog

## Git commit 规范

### commit message格式

``` text
<type>(<scope>): <subject>
```

#### type(必须)

用于说明git commit的类别，只允许使用下面的标识。
          
- feat：新功能（feature）。

- fix/to：修复bug，可以是QA发现的BUG，也可以是研发自己发现的BUG。
- fix：产生diff并自动修复此问题。适合于一次提交直接修复问题
- to：只产生diff不自动修复此问题。适合于多次提交。最终修复问题提交时使用fix

- docs：文档（documentation）。
 
- style：格式（不影响代码运行的变动）。
 
- refactor：重构（即不是新增功能，也不是修改bug的代码变动）。
 
- perf：优化相关，比如提升性能、体验。
 
- test：增加测试。
 
- chore：构建过程或辅助工具的变动。
 
- revert：回滚到上一个版本。
 
- merge：代码合并。
 
- sync：同步主线或分支的Bug。

#### scope(可选)

scope用于说明 commit 影响的范围，比如数据层、控制层、视图层等等，视项目不同而不同。

例如在Angular，可以是location，browser，compile，compile，rootScope， ngHref，ngClick，ngView等。如果你的修改影响了不止一个scope，你可以使用*代替。

#### subject(必须)

subject是commit目的的简短描述，不超过50个字符。

结尾不加句号或其他标点符号。

---

根据以上规范git commit message将是如下的格式：
- fix(DAO):用户查询缺少username属性 
- feat(Controller):用户查询接口开发

---

以上就是我们梳理的git commit规范，那么我们这样规范git commit到底有哪些好处呢？

- 便于程序员对提交历史进行追溯，了解发生了什么情况。
- 一旦约束了commit message，意味着我们将慎重的进行每一次提交，不能再一股脑的把各种各样的改动都放在一个git commit里面，这样一来整个代码改动的历史也将更加清晰。
- 格式化的commit message才可以用于自动化输出Change log。

## 服务注册与发现
- spring.cloud.discovery.enabled（不用配置）
- spring.cloud.nacos.discovery.enabled （是否启用 nacos 服务注册与发现，默认true）

## 配置中心
- spring.cloud.nacos.config.enabled （是否启用 nacos 配置中心，默认true）

## Spring Boot之两种引入spring boot maven依赖的方式

1、方式一：spring-boot-starter-parent

``` xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.2.1.RELEASE</version>
</parent>
```

- 进入spring-boot-starter-parent里，能够发现它其实通过 parent 的方式依赖了咱们下面要讲的spring-boot-dependencies模块
- 可以通过property覆盖内部的依赖
2、方式二：使用spring-boot-dependenciesspa

``` xml
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

## 版本定义参数
https://maven.apache.org/maven-ci-friendly.html