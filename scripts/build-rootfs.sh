#!/usr/bin/env bash
# Pin the official Kali NetHunter Rootless minimal rootfs for an app ABI.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARCH="${NOXS_ARCH:-arm64-v8a}"
case "$ARCH" in
  arm64-v8a) IMAGE_ARCH=arm64 ;;
  armeabi-v7a) IMAGE_ARCH=armhf ;;
  x86_64) IMAGE_ARCH=amd64 ;;
  *) echo "Unsupported ABI: $ARCH" >&2; exit 2 ;;
esac
BASE="https://kali.download/nethunter-images/current/rootfs"
FILE="kali-nethunter-rootfs-minimal-${IMAGE_ARCH}.tar.xz"
STAGE="$(mktemp -d)"
trap 'rm -rf "$STAGE"' EXIT
command -v curl >/dev/null 2>&1 || { echo 'curl required' >&2; exit 1; }
command -v sha256sum >/dev/null 2>&1 || { echo 'sha256sum required' >&2; exit 1; }
curl -fsSL --retry 3 "$BASE/SHA256SUMS" -o "$STAGE/SHA256SUMS"
curl -fSL --retry 3 "$BASE/$FILE" -o "$STAGE/$FILE"
EXPECTED="$(awk -v f="$FILE" '$2 == f { print $1; exit }' "$STAGE/SHA256SUMS")"
ACTUAL="$(sha256sum "$STAGE/$FILE" | awk '{print $1}')"
SIZE="$(stat -c%s "$STAGE/$FILE" 2>/dev/null || stat -f%z "$STAGE/$FILE")"
if [[ -z "$EXPECTED" || "$EXPECTED" != "$ACTUAL" ]]; then
  echo "Kali SHA-256 verification failed for $FILE" >&2
  exit 1
fi
MANIFEST=$(cat <<EOF
{
  "schema": 1,
  "arch": "$ARCH",
  "rootfs": {
    "url": "$BASE/$FILE",
    "sha256": "$ACTUAL",
    "format": "tar.xz",
    "sizeBytes": $SIZE
  },
  "generatedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "source": "Kali NetHunter Rootless official minimal rootfs"
}
EOF
)
for DEST in "$ROOT/linux-runtime/bootstrap/$ARCH/bootstrap.manifest" "$ROOT/app/src/main/assets/bootstrap/$ARCH/bootstrap.manifest"; do
  mkdir -p "$(dirname "$DEST")"
  printf '%s\n' "$MANIFEST" > "$DEST"
done
echo "Pinned $FILE ($SIZE bytes, SHA-256 $ACTUAL)"
