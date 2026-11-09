# repositories/

Reference APT source list. `kali-rolling.sources` is the final Kali Linux (NetHunter Rootless)
configuration installed by RootfsConfigurator as the sole
`/etc/apt/sources.list.d/noxs.sources` file (arm64/armhf form; amd64 builds
use archive.kali.com + security.kali.com instead — same suites). During
first setup, the same signed repositories are temporarily served over HTTP
only to install and verify `ca-certificates`; initialization then switches to
HTTPS and requires a successful authenticated `apt-get update`.

Noxs ships exactly ONE environment (Kali Linux (NetHunter Rootless), Kali Rolling) — no
distribution selector, no Debian fallback (Kali Linux-first spec). The old
`bookworm.sources` reference was removed with the Debian environment.
