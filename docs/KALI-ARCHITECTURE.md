# NxKali architecture

NxKali is based on the Noxs Android application source and replaces its Ubuntu userspace bootstrap with Kali NetHunter Rootless.

## Runtime layers

1. **Android app** — Kotlin activities, storage, lifecycle, and terminal UI.
2. **App-private runtime** — bundled PRoot, rootfs archive cache, runtime state, and logs.
3. **Kali userspace** — official Kali NetHunter Rootless minimal rootfs, extracted with the guarded tar reader.
4. **APT** — the signed `kali-rolling` repository using the Kali archive keyring.
5. **KeX** — a local desktop server started inside the Kali userspace.
6. **Desktop viewport** — an RFB/VNC client connects only to `127.0.0.1:5901`; VNC password authentication is supported.

## Security boundaries

- No Android root permission is requested.
- The Kali rootfs is isolated in the app's private directory and launched through PRoot.
- The KeX VNC server is accessed through loopback only.
- The KeX password is supplied for the handshake and cleared from the transient character buffers; it is not stored by NxKali.
- Rootfs archives are SHA-256 verified before extraction.
- Tar extraction rejects path traversal, escaping links, device nodes, and setuid/setgid bits.
