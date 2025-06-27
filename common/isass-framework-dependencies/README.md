非 isass 体系的项目，可以使用 import 的方式引入版本依赖管理，再具体依赖某个模块

```xml

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>vip.isass</groupId>
            <artifactId>isass-framework-parent</artifactId>
            <version>${version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

```xml

<dependencies>
    <dependency>
        <groupId>vip.isass</groupId>
        <artifactId>isass-common</artifactId>
        <version>${version}</version>
    </dependency>
</dependencies>
```