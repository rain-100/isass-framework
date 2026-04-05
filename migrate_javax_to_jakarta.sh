#!/bin/bash
# 脚本功能：将项目中的 javax.* 包名迁移为 jakarta.*
# 适用于 Spring Boot 3+/4+ 的 Jakarta EE 迁移

echo "开始迁移 javax -> jakarta ..."

# 查找所有 Java 和配置文件
find src -type f \( -name "*.java" -o -name "*.xml" -o -name "*.properties" -o -name "*.yml" -o -name "*.yaml" \) -print0 | while IFS= read -r -d '' file; do
    # 替换常见的 javax 包
    sed -i 's/javax\.servlet/jakarta.servlet/g' "$file"
    sed -i 's/javax\.persistence/jakarta.persistence/g' "$file"
    sed -i 's/javax\.validation/jakarta.validation/g' "$file"
    sed -i 's/javax\.annotation/jakarta.annotation/g' "$file"
    sed -i 's/javax\.inject/jakarta.inject/g' "$file"
    sed -i 's/javax\.ws\.rs/jakarta.ws.rs/g' "$file"
    sed -i 's/javax\.xml\.bind/jakarta.xml.bind/g' "$file"
    sed -i 's/javax\.el/jakarta.el/g' "$file"
    sed -i 's/javax\.transaction/jakarta.transaction/g' "$file"
done

echo "迁移完成！请检查编译错误并手动处理特殊依赖。"
