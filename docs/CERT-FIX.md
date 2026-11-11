# Kali APT and certificate repair — `nx cert-fix`

NxKali includes a built-in repair command for the Kali NetHunter Rootless
APT environment. It repairs package-index, interrupted DPKG configuration,
CA certificate and download-tool problems without reinstalling the rootfs.

## One-line fixer command

Run this inside the NxKali Kali terminal:

```bash
nx cert-fix
```

That is the complete command. It does not require downloading or piping a
remote shell script.

## When to use it

Run the fixer when you see errors such as:

- `Noxs package manager is busy` after an earlier package operation has
  finished.
- `Unable to locate package ...` after package indexes failed to refresh.
- TLS or certificate verification errors during APT downloads.
- An interrupted package configuration or a missing CA bundle.

If APT or DPKG is actively running, allow that operation to finish first.
The fixer checks the Noxs transaction marker and active package-manager
processes. If another transaction is still active, it exits with a busy
message; retry `nx cert-fix` after the transaction has completed.

## Repair sequence

The bundled `linux-runtime/nx/cert-fix.sh` performs a bounded repair:

1. Checks that APT is available and that the Kali sources file exists.
   If the NxKali sources file is missing, it recreates the official
   `kali-rolling` entry using `http.kali.org/kali` and the
   `kali-archive-keyring`.
2. Checks `/run/noxs/pkg-tx` and active `apt`, `apt-get` and `dpkg`
   processes before starting its own package transaction.
3. Refreshes the existing certificate bundle when the updater is present.
4. Runs `apt-get update` with strict error reporting and checks the
   captured output and package-list files instead of trusting exit code 0
   alone.
5. If HTTPS fails, temporarily tries the Kali repository over HTTP while
   retaining APT's GPG signature verification. It attempts to restore
   HTTPS and verifies it again after the repair.
6. Attempts `dpkg --configure -a` to recover interrupted configuration.
7. Installs or repairs `ca-certificates`, `kali-archive-keyring`,
   `curl` and `wget` without recommended extra packages.
8. Regenerates the certificate bundle with
   `update-ca-certificates --fresh` and checks the final package state.

The script uses bounded retries and stops when the same failure repeats.
It does not kill active APT/DPKG processes or delete their lock files.

## Logs and troubleshooting

A full transcript is saved under:

```text
~/.noxs/logs/cert-fix-<timestamp>.txt
```

If the command exits with a busy message, do not remove lock files. Wait
for the existing APT/DPKG operation to finish, then run the one-line
command again. If it fails for another reason, inspect the latest log and
share the final error section when requesting support.

## Source

- Repair script: `linux-runtime/nx/cert-fix.sh`
- CLI dispatch: `linux-runtime/launcher/nx`
- Kotlin-embedded copy: `app/src/main/java/com/crossberry/noxs/runtime/NoxsNxCertFix.kt`
- Kali setup: [KALI-SETUP.md](KALI-SETUP.md)
