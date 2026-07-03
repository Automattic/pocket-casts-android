#!/usr/bin/env python3
"""Export a deterministic Android benchmark fixture from FunctionGemma validation data."""

import argparse
import hashlib
import json
import re
from pathlib import Path


MODEL_MARKER = "<start_of_turn>model\n"
TURN_RE = re.compile(r"<start_of_turn>(developer|user|model)\n(.*?)<end_of_turn>", re.DOTALL)
TOOL_CALL_RE = re.compile(
    r"<start_function_call>call:(\w+)\{(.*?)\}<end_function_call>",
    re.DOTALL,
)


def split_params(text: str) -> list[str]:
    parts = []
    start = 0
    in_escape = False
    index = 0
    while index < len(text):
        if text.startswith("<escape>", index):
            in_escape = True
            index += len("<escape>")
            continue
        if text.startswith("</escape>", index):
            in_escape = False
            index += len("</escape>")
            continue
        if text[index] == "," and not in_escape:
            parts.append(text[start:index])
            start = index + 1
        index += 1
    parts.append(text[start:])
    return [part.strip() for part in parts if part.strip()]


def parse_value(value: str):
    if value.startswith("<escape>") and value.endswith("</escape>"):
        return value[len("<escape>"):-len("</escape>")]
    if value == "true":
        return True
    if value == "false":
        return False
    try:
        return int(value)
    except ValueError:
        pass
    try:
        return float(value)
    except ValueError:
        return value


def parse_call(completion: str) -> tuple[str, str, dict]:
    match = TOOL_CALL_RE.search(completion)
    if match is None:
        raise ValueError("sample completion does not contain a function call")
    params = {}
    for part in split_params(match.group(2)):
        key, separator, value = part.partition(":")
        if separator:
            params[key] = parse_value(value)
    action = params.pop("action", "")
    return match.group(1), action, params


def parse_sample(text: str) -> dict:
    marker_index = text.rfind(MODEL_MARKER)
    if marker_index < 0:
        raise ValueError("sample does not contain a final model turn")
    prompt = text[:marker_index]
    completion = text[marker_index + len(MODEL_MARKER):]
    turns = [
        {"role": role, "content": content}
        for role, content in TURN_RE.findall(prompt)
        if role != "developer"
    ]
    if not turns or turns[-1]["role"] != "user":
        raise ValueError("sample prompt does not end in a user turn")
    transcript = turns.pop()["content"]
    name, action, params = parse_call(completion)
    return {
        "history": turns,
        "transcript": transcript,
        "expected_name": name,
        "expected_action": action,
        "expected_params": params,
    }


def stable_key(row: dict) -> str:
    serialized = json.dumps(row, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(serialized).hexdigest()


def select_rows(rows: list[dict], count: int) -> list[dict]:
    if count > len(rows):
        raise ValueError(f"requested {count} rows from a {len(rows)}-row source")

    selected = []
    selected_ids = set()

    def include(row: dict) -> None:
        row_id = stable_key(row)
        if row_id not in selected_ids:
            selected.append(row)
            selected_ids.add(row_id)

    by_tool = {}
    by_dialog_action = {}
    longest = max(
        rows,
        key=lambda row: len(
            json.dumps(
                [row["expected_name"], row["expected_action"], row["expected_params"]],
                sort_keys=True,
            ),
        ),
    )
    for row in rows:
        by_tool.setdefault(row["expected_name"], row)
        if row["expected_name"] == "dialog_control":
            by_dialog_action.setdefault(row["expected_action"], row)

    for tool in sorted(by_tool):
        include(by_tool[tool])
    for action in sorted(by_dialog_action):
        include(by_dialog_action[action])
    include(longest)

    if len(selected) > count:
        raise ValueError(
            f"count={count} is too small for {len(selected)} required representative rows",
        )

    for row in sorted(rows, key=stable_key):
        if len(selected) == count:
            break
        include(row)
    return selected


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--count", type=int, default=100)
    args = parser.parse_args()

    rows = [
        parse_sample(json.loads(line)["text"])
        for line in args.source.read_text().splitlines()
        if line.strip()
    ]
    selected = select_rows(rows, args.count)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(selected, indent=2, ensure_ascii=False) + "\n")
    print(f"Exported {len(selected)} rows to {args.output}")


if __name__ == "__main__":
    main()
