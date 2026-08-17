"""
这个文件是学习完 JADX 后再看的“答案版” Python 示例。

运行前先启动：
    python server.py
"""

import hashlib
import hmac
import json
import time
import urllib.request

BASE_URL = "http://127.0.0.1:8000"
PATH = "/api/follow/add"
SECRET = b"LAB_SECRET_V1_2026"


def sign(raw_body: bytes, timestamp: str) -> str:
    canonical = (
        b"POST\n"
        + PATH.encode()
        + b"\n"
        + timestamp.encode()
        + b"\n"
        + raw_body
    )

    digest = hmac.new(
        SECRET,
        canonical,
        hashlib.sha256
    ).hexdigest()

    return "LAB " + digest


def follow(user_id: int, follow_user_id: str, from_page="Search"):
    timestamp = str(int(time.time()))

    # separators 保持紧凑 JSON，方便观察原始 body。
    raw_body = json.dumps(
        {
            "user_id": user_id,
            "follow_user_id": follow_user_id,
            "from_page": from_page,
            "timestamp": int(timestamp),
        },
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")

    req = urllib.request.Request(
        BASE_URL + PATH,
        method="POST",
        data=raw_body,
        headers={
            "Content-Type": "application/json;charset=utf-8",
            "Timestamp": timestamp,
            "Authorization": sign(raw_body, timestamp),
        },
    )

    with urllib.request.urlopen(req, timeout=10) as resp:
        print(resp.status)
        print(resp.read().decode("utf-8"))


if __name__ == "__main__":
    follow(519835849, "363330929")
