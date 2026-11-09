# Kali NetHunter Rootless on NxKali

NxKali provisions the official Kali NetHunter Rootless minimal userspace inside the Android app's private storage. It runs through PRoot; Android itself is not replaced and device root access is not required.

## First setup

1. Open NxKali and complete the environment setup.
2. The app downloads the architecture-matched Kali minimal rootfs from the official Kali download host.
3. NxKali verifies the archive against the pinned SHA-256 value in the bootstrap manifest before extraction.
4. The rootfs is extracted with the path/link checks in `TarGuard`, then configured with the signed `kali-rolling` APT source.

The initial rootfs download is approximately 120 MiB for ARM64. Extracted storage use is substantially larger; keep at least 1 GiB free before setup.

## Terminal

The Kali shell is launched inside PRoot under the app-private rootfs. The app's `sudo` wrapper affects only that guest environment; it does not grant Android root access.

## Kali KeX desktop

1. Open the Kali terminal and run `kex passwd` once to configure the KeX password.
2. Return to NxKali and choose **Kali Desktop**.
3. Enter the KeX password. NxKali starts the KeX server and connects its VNC viewport to `127.0.0.1:5901`.
4. Touch the desktop to send pointer input; use the text field to send keyboard text.
5. Choose **Stop** to stop the KeX session.

The VNC connection stays on the Android device's loopback interface. NxKali does not expose the desktop server to the local network.

## Source and verification

Rootfs URLs and hashes are pinned in:

- `app/src/main/assets/bootstrap/`
- `linux-runtime/bootstrap/`

The source of truth for the current official image checksums is Kali's `SHA256SUMS` file in the NetHunter rootfs directory.
