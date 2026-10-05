#!/usr/bin/env python3
"""按当前 Mapper 模板生成父接口迁移补丁，保留手写方法、注解和 XML；不写文件。"""
import argparse
from pathlib import Path
import re


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_root", type=Path, help="本次明确选取的 Java 源码根目录")
    args = parser.parse_args()
    template = (Path(__file__).resolve().parent.parent / "src/main/resources/templates/nocode/mapper.java.ftl").read_text()
    parent = re.search(r"extends (\w+)<", template).group(1)
    parent_import = re.search(r"import ([\w.]+\." + parent + r");", template).group(1)
    patches = []
    for path in sorted(args.source_root.resolve().rglob("*Mapper.java")):
        source = path.read_text()
        if "import com.baomidou.mybatisplus.core.mapper.BaseMapper;" not in source:
            continue
        declaration = re.search(r"^public interface \w+ extends BaseMapper<[^>]+> \{", source, re.MULTILINE)
        if declaration is None:
            raise ValueError(f"无法安全迁移 Mapper 声明: {path}")
        patches.extend([
            f"*** Update File: {path}", "@@",
            "-import com.baomidou.mybatisplus.core.mapper.BaseMapper;",
            f"+import {parent_import};", "@@",
            "-" + declaration.group(),
            "+" + declaration.group().replace("extends BaseMapper<", f"extends {parent}<"),
        ])
    if patches:
        print("\n".join(["*** Begin Patch", *patches, "*** End Patch"]))


if __name__ == "__main__":
    main()
