# NxKali

**Kali NetHunter Rootless for Android — a private Kali Linux userspace with terminal and local KeX desktop.**

NxKali is based on the Noxs Android application source. It uses PRoot inside app-private storage; it does not replace Android and does not require device root.

## Features

- Official Kali NetHunter Rootless minimal rootfs for ARM64, ARMHF, and x86_64.
- SHA-256 verification before guarded rootfs extraction.
- Kali rolling APT sources and archive keyring.
- Android terminal and app-private Linux filesystem.
- Local Kali KeX desktop viewport through loopback VNC (`127.0.0.1:5901`).
- VNC password authentication; KeX credentials are not saved by NxKali.

## Kali desktop

1. Complete the first-run Kali setup.
2. Open the terminal and run `kex passwd` once.
3. Choose **Kali Desktop** on the welcome screen and enter that password.
4. Use **Stop** to end the KeX session.

## Source

- Repository: https://github.com/crossberry-in/Nxkali
- Official Kali NetHunter Rootless documentation: https://www.kali.org/docs/nethunter/nethunter-rootless/
- Rootfs manifests: `app/src/main/assets/bootstrap/` and `linux-runtime/bootstrap/`

## Build

The Android project uses Gradle. Run:

```bash
./gradlew assembleDebug testDebugUnitTest :terminal-emulator:test
```

Build and device verification status is tracked in [Kali test report](docs/KALI-TEST-REPORT.md).

## License

See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md). The Noxs-derived source retains its upstream license notices.