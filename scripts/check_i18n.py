#!/usr/bin/env python3
"""Checks Android string resource key parity for supported languages."""

from __future__ import annotations

from pathlib import Path
from xml.etree import ElementTree


ROOT_DIR = Path(__file__).resolve().parents[1]
FILES = {
    "ru": ROOT_DIR / "app/src/main/res/values/strings.xml",
    "en": ROOT_DIR / "app/src/main/res/values-en/strings.xml",
    "zh": ROOT_DIR / "app/src/main/res/values-zh/strings.xml",
}


def string_keys(path: Path) -> list[str]:
    """Returns sorted Android string resource names."""

    root = ElementTree.parse(path).getroot()
    return sorted(
        element.attrib["name"]
        for element in root.findall("string")
        if element.attrib.get("translatable", "true").lower() != "false"
    )


def main() -> int:
    """Checks that all localized string files expose the same keys."""

    keys_by_language = {language: string_keys(path) for language, path in FILES.items()}
    base_keys = keys_by_language["ru"]
    base_set = set(base_keys)
    for language, keys in keys_by_language.items():
        if keys == base_keys:
            continue
        current_set = set(keys)
        missing = sorted(base_set - current_set)
        extra = sorted(current_set - base_set)
        raise SystemExit(f"{language}: missing={missing} extra={extra}")
    print(f"android i18n keys ok: {len(base_keys)} keys across {len(keys_by_language)} languages")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
