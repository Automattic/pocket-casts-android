#!/usr/bin/env python3
"""Serve a local What's New catalog to a debuggable build by seeding it into the app's OkHttp cache.

Usage:
  scripts/whats_new_mock_catalog.py [catalog.json] [--package PKG] [--host HOST] [--locale LOCALE] [-s SERIAL]
  scripts/whats_new_mock_catalog.py --reset [...]
"""

import argparse
import hashlib
import subprocess
import sys
import time
from email.utils import formatdate
from pathlib import Path

DEFAULT_CATALOG = Path(__file__).resolve().parent.parent / "docs" / "whats-new" / "mock-catalog.json"
CACHE_DIR = "cache/HttpCache"
JOURNAL_HEADER = "libcore.io.DiskLruCache\n1\n201105\n2\n\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("catalog", nargs="?", type=Path, default=DEFAULT_CATALOG)
    parser.add_argument("--package", default="au.com.shiftyjelly.pocketcasts.debug")
    parser.add_argument("--host", default="static.pocketcasts.net")
    parser.add_argument("--locale", default="en")
    parser.add_argument("-s", "--serial")
    parser.add_argument("--reset", action="store_true")
    args = parser.parse_args()

    device = Device(args.serial, args.package)
    url = f"https://{args.host}/whats-new/v1/android/{args.locale}.json"
    key = hashlib.md5(url.encode()).hexdigest()

    device.adb("shell", "am", "force-stop", args.package)
    device.run_as("mkdir", "-p", CACHE_DIR)

    if args.reset:
        device.run_as("rm", "-f", f"{CACHE_DIR}/{key}.0", f"{CACHE_DIR}/{key}.1")
        device.append_journal(f"REMOVE {key}\n")
        print(f"Removed the cached catalog for {url}")
        return

    body = args.catalog.read_bytes()
    metadata = cache_metadata(url, len(body)).encode()
    device.write(f"{CACHE_DIR}/{key}.0", metadata)
    device.write(f"{CACHE_DIR}/{key}.1", body)
    device.append_journal(f"CLEAN {key} {len(metadata)} {len(body)}\n")
    print(f"Seeded {args.catalog} as {url}")


def cache_metadata(url, content_length):
    now_millis = int(time.time() * 1000)
    headers = [
        "Content-Type: application/json; charset=utf-8",
        f"Content-Length: {content_length}",
        f"Date: {formatdate(usegmt=True)}",
        "Cache-Control: max-age=31536000",
        f"OkHttp-Sent-Millis: {now_millis}",
        f"OkHttp-Received-Millis: {now_millis}",
    ]
    lines = [url, "GET", "0", "HTTP/1.1 200 OK", str(len(headers)), *headers, "", "TLS_AES_128_GCM_SHA256", "0", "0", "TLSv1.3"]
    return "\n".join(lines) + "\n"


class Device:
    def __init__(self, serial, package):
        self.serial_args = ["-s", serial] if serial else []
        self.package = package

    def adb(self, *args, stdin=None):
        result = subprocess.run(["adb", *self.serial_args, *args], input=stdin, capture_output=True)
        if result.returncode != 0:
            sys.exit(result.stderr.decode() or result.stdout.decode())
        return result.stdout

    def run_as(self, *command, stdin=None):
        mode = "exec-out" if stdin is None else "exec-in"
        return self.adb(mode, "run-as", self.package, *command, stdin=stdin)

    def write(self, path, data):
        self.run_as("sh", "-c", f"cat > {path}", stdin=data)

    def append_journal(self, line):
        journal = f"{CACHE_DIR}/journal"
        existing = self.run_as("sh", "-c", f"cat {journal} 2>/dev/null || true")
        if not existing.startswith(JOURNAL_HEADER.encode()):
            existing = JOURNAL_HEADER.encode()
        self.write(journal, existing + line.encode())


if __name__ == "__main__":
    main()
