from __future__ import annotations

from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[3]
SCENE_FILE = ROOT / "admin" / "src" / "main" / "java" / "com" / "interviewpilot" / "agent" / "application" / "BusinessAgentScene.java"
APP_YAML = ROOT / "admin" / "src" / "main" / "resources" / "application.yaml"
OUT = Path(__file__).resolve().parents[1] / "references" / "generated-agent-scene-map.md"


def load_bindings() -> dict[str, str]:
    with APP_YAML.open("r", encoding="utf-8") as fp:
        data = yaml.safe_load(fp) or {}
    return (((data.get("interview-pilot") or {}).get("agent-binding")) or {})


def parse_aliases(raw_tail: str) -> list[str]:
    return [part.strip().strip('"') for part in raw_tail.split(",") if part.strip()]


def parse_scene_constants(source: str) -> list[tuple[str, str, str, list[str]]]:
    constants: list[tuple[str, str, str, list[str]]] = []
    enum_start = source.find("public enum BusinessAgentScene")
    if enum_start == -1:
        return constants
    enum_source = source[enum_start:]
    enum_body = enum_source.split(";", 1)[0]
    cursor = enum_body.find("{")
    if cursor == -1:
        return constants
    cursor += 1
    length = len(enum_body)
    while cursor < length:
        while cursor < length and (enum_body[cursor].isspace() or enum_body[cursor] == ","):
            cursor += 1
        start = cursor
        while cursor < length and (enum_body[cursor].isupper() or enum_body[cursor] == "_"):
            cursor += 1
        enum_name = enum_body[start:cursor]
        if not enum_name:
            break
        while cursor < length and enum_body[cursor].isspace():
            cursor += 1
        if cursor >= length or enum_body[cursor] != "(":
            break
        cursor += 1
        depth = 1
        value_start = cursor
        while cursor < length and depth > 0:
            if enum_body[cursor] == "(":
                depth += 1
            elif enum_body[cursor] == ")":
                depth -= 1
            cursor += 1
        values = parse_aliases(enum_body[value_start: cursor - 1].replace("\n", " "))
        if len(values) >= 2:
            constants.append((enum_name, values[0], values[1], values[2:]))
    return constants


def main() -> None:
    bindings = load_bindings()
    lines = [
        "# Agent 场景索引（自动生成）",
        "",
        "该文档从 `BusinessAgentScene.java` 和 `application.yaml` 的 `interview-pilot.agent-binding` 自动提取。",
        "",
        "| 枚举名 | 场景 code | 默认名 | 别名 | 当前配置绑定 |",
        "| --- | --- | --- | --- | --- |",
    ]
    for enum_name, scene_code, default_name, aliases in parse_scene_constants(SCENE_FILE.read_text(encoding="utf-8")):
        configured = bindings.get(scene_code, "")
        alias_text = "、".join(aliases) if aliases else "-"
        lines.append(f"| `{enum_name}` | `{scene_code}` | `{default_name}` | `{alias_text}` | `{configured}` |")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
