#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-$ROOT/dist}"
BASE="https://kali.download/nethunter-images/current/rootfs"
FILE="kali-nethunter-rootfs-minimal-arm64.tar.xz"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$OUT"
for tool in curl sha256sum awk stat zip; do command -v "$tool" >/dev/null 2>&1 || { echo "$tool required" >&2; exit 1; }; done
curl -fsSL --retry 5 --retry-all-errors "$BASE/SHA256SUMS" -o "$STAGE/SHA256SUMS"
curl -fL --retry 5 --retry-all-errors --output "$STAGE/rootfs.tar.xz" "$BASE/$FILE"
EXPECTED="$(awk -v f="$FILE" '$2 == f { print $1; exit }' "$STAGE/SHA256SUMS")"
ACTUAL="$(sha256sum "$STAGE/rootfs.tar.xz" | awk '{print $1}')"
SIZE="$(stat -c%s "$STAGE/rootfs.tar.xz" 2>/dev/null || stat -f%z "$STAGE/rootfs.tar.xz")"
[[ -n "$EXPECTED" && "$EXPECTED" == "$ACTUAL" ]] || { echo 'Kali SHA-256 verification failed' >&2; exit 1; }
printf '{\n  "schema": 1,\n  "arch": "arm64-v8a",\n  "filename": "rootfs.tar.xz",\n  "sizeBytes": %s,\n  "rootfsSha256": "%s"\n}\n' "$SIZE" "$ACTUAL" > "$STAGE/offline-package.json"
PACKAGE="$OUT/NxKali-Offline-ARM64.zip"
rm -f "$PACKAGE"
(cd "$STAGE" && zip -0 "$PACKAGE" rootfs.tar.xz offline-package.json)
cp "$STAGE/offline-package.json" "$OUT/offline-package.json"
(cd "$OUT" && sha256sum NxKali-Offline-ARM64.zip > NxKali-Offline-ARM64.zip.sha256)
echo "Created $PACKAGE; rootfs SHA-256: $ACTUAL"
