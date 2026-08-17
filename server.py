#!/usr/bin/env python3
"""
Protocol Signature Lab V1 - 本地实验服务器
只使用 Python 标准库，无需 pip install。

启动：
    python server.py

默认监听：
    0.0.0.0:8000

签名：
    HMAC-SHA256(
        key="LAB_SECRET_V1_2026",
        message="POST\n/api/follow/add\n<Timestamp>\n<raw_body>"
    )
"""

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import hashlib
import hmac
import json
import time

HOST = "0.0.0.0"
PORT = 8000

LAB_SECRET = b"LAB_SECRET_V1_2026"
API_PATH = "/api/follow/add"


def make_signature(timestamp: str, raw_body: bytes) -> str:
    canonical = (
        b"POST\n"
        + API_PATH.encode("utf-8")
        + b"\n"
        + timestamp.encode("utf-8")
        + b"\n"
        + raw_body
    )

    return hmac.new(
        LAB_SECRET,
        canonical,
        hashlib.sha256
    ).hexdigest()


class Handler(BaseHTTPRequestHandler):

    server_version = "ProtocolSignatureLabV1/1.0"

    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))

    def send_json(self, status: int, payload: dict):
        data = json.dumps(
            payload,
            ensure_ascii=False,
            separators=(",", ":")
        ).encode("utf-8")

        self.send_response(status)
        self.send_header("Content-Type", "application/json;charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        if self.path != API_PATH:
            self.send_json(404, {
                "code": 1404,
                "message": "not found"
            })
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        raw_body = self.rfile.read(content_length)

        timestamp = self.headers.get("Timestamp", "")
        authorization = self.headers.get("Authorization", "")

        expected = make_signature(timestamp, raw_body)
        expected_auth = "LAB " + expected

        print("\n===== REQUEST =====")
        print("Path:", self.path)
        print("Timestamp:", timestamp)
        print("Authorization:", authorization)
        print("Expected:", expected_auth)
        print("Body:", raw_body.decode("utf-8", errors="replace"))
        print("===================\n")

        # 使用 compare_digest 避免普通字符串比较造成的时序差异。
        if not hmac.compare_digest(authorization, expected_auth):
            self.send_json(200, {
                "code": 1002,
                "message": "signature invalid",
                "trace_id": f"lab-{int(time.time() * 1000)}"
            })
            return

        try:
            body = json.loads(raw_body.decode("utf-8"))
        except Exception:
            self.send_json(200, {
                "code": 1003,
                "message": "invalid json"
            })
            return

        self.send_json(200, {
            "code": 1000,
            "message": "follow success",
            "follow_user_id": str(body.get("follow_user_id", "")),
            "from_page": str(body.get("from_page", "")),
            "trace_id": f"lab-{int(time.time() * 1000)}"
        })


if __name__ == "__main__":
    print(f"Protocol Signature Lab V1")
    print(f"Listening on http://{HOST}:{PORT}")
    print(f"Endpoint: POST {API_PATH}")
    print("Press Ctrl+C to stop.\n")

    server = ThreadingHTTPServer((HOST, PORT), Handler)

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
