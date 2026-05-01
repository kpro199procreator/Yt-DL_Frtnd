#!/usr/bin/env python3
"""Pequeño CLI para ejecutar yt-dlp desde este repositorio.

Uso rápido:
  python3 tools/ytdlp_cli.py "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
  python3 tools/ytdlp_cli.py -- -F "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="ytdlp-cli",
        description="Wrapper mínimo para ejecutar yt-dlp desde el repo",
    )
    parser.add_argument(
        "args",
        nargs=argparse.REMAINDER,
        help="Argumentos para yt-dlp (usa -- antes si empiezan con guion)",
    )
    return parser


def resolve_repo_root() -> Path:
    return Path(__file__).resolve().parent.parent


def main() -> int:
    parser = build_parser()
    ns = parser.parse_args()

    if not ns.args:
        parser.print_help()
        return 2

    ytdlp_bin = shutil.which("yt-dlp")
    if ytdlp_bin is None:
        print(
            "Error: no se encontró 'yt-dlp' en PATH. "
            "Instálalo y vuelve a intentarlo.",
            file=sys.stderr,
        )
        return 127

    forwarded_args = ns.args
    if forwarded_args and forwarded_args[0] == "--":
        forwarded_args = forwarded_args[1:]

    cmd = [ytdlp_bin, *forwarded_args]
    result = subprocess.run(cmd, cwd=resolve_repo_root(), check=False)
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
