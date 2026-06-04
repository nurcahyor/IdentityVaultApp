# IdentityVaultApp

IdentityVault adalah aplikasi Kotlin Android untuk pengujian satu identity profile aktif, bukan untuk mengubah identitas hardware asli.

Fitur utama:
- Identity profile generator dan editor manual.
- ON/OFF per field identity.
- Satu current profile aktif untuk Generate, Apply, Backup, Restore, dan hook provider.
- Environment / Root Detector.
- Root, Magisk, dan LSPosed compatibility checks.
- Backup/restore profile aktif dan data inti aplikasi.
- LSPosed hook marker untuk mendeteksi module aktif.

## Perilaku Profile

IdentityVault sekarang kembali sederhana:

- Tidak ada Identity Slots.
- Tidak ada Identity Groups.
- Tidak ada assigned identity per package.
- Tidak ada active group atau backup/restore per group/app.
- Apply selalu memakai current profile yang sedang tampil di halaman IdentityVault.
- Backup Profile selalu membackup current profile tersebut.
- Restore Profile mengembalikan current profile ke halaman IdentityVault.
- Hook/provider selalu membaca current profile dari `IdentityRepository`.

Jika file backup lama masih memiliki blok `identitySlots`, bagian itu diabaikan saat restore dan tidak mempengaruhi current profile.

Catatan:
- Hanya buat edukasi dan testing.
- Tidak menulis ke EFS, persist, modem, NVRAM, baseband, atau radio partition.
- Tidak mengubah IMEI hardware asli.

## Build APK

APK debug bisa dibuat otomatis via GitHub Actions atau lokal.

- Automatic build via GitHub Actions: buka tab Actions, pilih Build APK, lalu download artifact `IdentityVault-debug-apk`.
- Tutorial lengkap: [docs/BUILD_APK.md](docs/BUILD_APK.md)
- Lokasi APK debug setelah build lokal:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Catatan: APK ini butuh root/Magisk/LSPosed untuk fungsi hook.

Build lokal:

```powershell
.\gradlew.bat :app:assembleDebug
```
