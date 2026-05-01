## protobuf

本模块提供 Protobuf 序列化和反序列化能力。

### 创建 Proto 文件并生成 Java 代码

#### 1. 创建 proto 文件

在 `src/main/resources/ProtoFile` 目录下创建 `.proto` 文件，例如 `User.proto`：

```protobuf
package vip.isass.framework.serialization.protobuf;
option java_package = "vip.isass.framework.serialization.impl.protobuf.user";
option java_outer_classname = "User";

message User {
    optional int32 id = 1;
    optional string name = 2;
    optional string email = 3;
}
```

**关键配置说明：**

| 配置项 | 说明 |
|--------|------|
| `package` | protobuf 消息命名空间 |
| `option java_package` | 生成的 Java 类的包名 |
| `option java_outer_classname` | 生成的 Java 外部类名 |

#### 2. 运行 Maven 生成代码

```bash
mvn protobuf:compile
```

或者在编译时自动生成：

```bash
mvn compile
```

#### 3. 生成的 Java 文件位置

生成的 Java 代码默认输出到 `src/main/java` 目录下，保持与 proto 源文件相同的包结构。

例如上述 `User.proto` 会生成：
```
src/main/java/vip/isass/framework/serialization/impl/protobuf/user/User.java
```

#### 4. 在代码中使用

```java
import vip.isass.framework.serialization.impl.protobuf.user.User;

// 创建消息
User user = User.newBuilder()
    .setId(1)
    .setName("张三")
    .setEmail("zhangsan@example.com")
    .build();

// 序列化
byte[] bytes = user.toByteArray();

// 反序列化
User parsedUser = User.parseFrom(bytes);
```

### 注意事项

- **不要手动编辑生成的 Java 文件** - 下次编译时会被覆盖
- 如需修改生成的类，请修改 `.proto` 文件后重新编译
- 本模块已配置 `protobuf-maven-plugin`，编译时会自动执行 `protoc` 命令

### 其他模块如何使用

如果其他模块也需要创建 proto 文件并生成 Java 代码，需要完成以下配置：

#### 1. 添加依赖

在模块的 `pom.xml` 中添加 isass-serialization-protobuf 依赖：

```xml
<dependency>
    <groupId>vip.isass</groupId>
    <artifactId>isass-serialization-protobuf</artifactId>
</dependency>
```

#### 2. 添加插件配置

在模块的 `pom.xml` 中添加 `protobuf-maven-plugin` 配置：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:${protobuf-java.version}:exe:${os.detected.classifier}</protocArtifact>
                <protoSourceRoot>${basedir}/src/main/resources/ProtoFile</protoSourceRoot>
                <outputDirectory>${basedir}/src/main/java</outputDirectory>
                <clearOutputDirectory>false</clearOutputDirectory>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### 3. 创建 proto 文件

在模块的 `src/main/resources/ProtoFile` 目录下创建 `.proto` 文件。

#### 4. 编译生成代码

```bash
mvn compile
```

#### 5. 使用生成的类

在代码中 import 生成的类即可使用，注意包名要与 proto 文件中的 `java_package` 配置一致。