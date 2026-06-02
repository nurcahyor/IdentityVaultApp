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

Build:

```powershell
.\gradlew.bat :app:assembleDebug
```
