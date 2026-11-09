# NxKali migration test report

## Implemented

- Imported the complete tracked Noxs source tree into this repository.
- Switched fresh environment metadata and bootstrap manifests to official Kali NetHunter Rootless minimal images.
- Configured the Kali rolling APT endpoint and archive keyring.
- Added a local KeX lifecycle helper and an in-app VNC desktop viewport with VNC password authentication.

## Verification status

The source changes have been pushed. Android build, JVM tests, rootfs installation on a physical Android device, and a live KeX desktop session must still pass before this migration can be called fully verified. Do not treat the presence of the UI or a successful source import as proof that the desktop has booted successfully.
