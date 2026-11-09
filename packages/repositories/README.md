# repositories/

Reference APT source definition for the official Kali rolling repository.

RootfsConfigurator writes the active source to `/etc/apt/sources.list.d/noxs.sources` and pins the Kali archive keyring.

```deb822
Types: deb
URIs: https://http.kali.org/kali
Suites: kali-rolling
Components: main contrib non-free non-free-firmware
Signed-By: /usr/share/keyrings/kali-archive-keyring.gpg
```
