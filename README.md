# IdentityVaultApp

IdentityVault adalah aplikasi Kotlin Android untuk pengujian identity profile berbasis app/profile, bukan untuk mengubah identitas hardware asli.

Fitur utama:
- Identity profile generator dan editor manual.
- ON/OFF per field identity.
- Environment / Root Detector.
- Root, Magisk, dan LSPosed compatibility checks.
- Backup/restore data inti aplikasi.
- LSPosed hook marker untuk mendeteksi module aktif.

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
