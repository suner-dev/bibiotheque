#!/usr/bin/env python3
"""Serveur web simple pour le frontend Angular (SPA)."""
import http.server
import os
import sys

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 4202
DIRECTORY = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "..", "bibliotheque-frontend", "dist", "bibliotheque-frontend"
)
DIRECTORY = os.path.abspath(DIRECTORY)

class SPAHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def do_GET(self):
        path = self.translate_path(self.path)
        if os.path.isdir(path):
            index_path = os.path.join(path, "index.html")
            if os.path.isfile(index_path):
                path = index_path
        if not os.path.isfile(path):
            # SPA fallback: serve index.html for any non-file route
            path = os.path.join(DIRECTORY, "index.html")
        try:
            with open(path, "rb") as f:
                content = f.read()
            self.send_response(200)
            self.send_header("Content-type", self.guess_type(path))
            self.end_headers()
            self.wfile.write(content)
        except FileNotFoundError:
            self.send_error(404, "Not found")

if __name__ == "__main__":
    print(f"Serving frontend from {DIRECTORY} on port {PORT}")
    server = http.server.HTTPServer(("0.0.0.0", PORT), SPAHandler)
    server.serve_forever()