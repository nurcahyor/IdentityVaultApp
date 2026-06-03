# Build IdentityVault APK

## Requirements

- GitHub account
- Android Studio optional
- Java 17
- Git

## Cara build otomatis via GitHub Actions

1. Buka repo GitHub.
2. Masuk tab Actions.
3. Pilih Build APK.
4. Klik Run workflow.
5. Tunggu sampai selesai.
6. Buka run result.
7. Download artifact IdentityVault-debug-apk.
8. Extract zip.
9. Install APK ke emulator/device.

## Cara build lokal

```bash
./gradlew clean assembleDebug
```

Hasil APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Di Windows PowerShell:

```powershell
.\gradlew.bat clean assembleDebug
```

## Cara install APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Cara test LSPosed

1. Install APK.
2. Enable module di LSPosed.
3. Pilih scope target.
4. Reboot emulator/device.
5. Buka IdentityVault.
6. Generate All.
7. Apply.
8. Force stop target app / Settings.
9. Buka ulang target app.

## Scope rekomendasi testing

- Full Device Info
- Settings / com.android.settings
- Settings Storage / com.android.providers.settings
- System Framework / android jika perlu

## Troubleshooting

### APK tidak muncul di artifact

Pastikan workflow Build APK selesai tanpa error dan langkah Upload debug APK menemukan file di:

```text
app/build/outputs/apk/debug/*.apk
```

### gradlew permission denied

Workflow sudah menjalankan:

```bash
chmod +x ./gradlew
```

Kalau build lokal Linux/macOS tetap gagal, jalankan command yang sama.

### LSPosed tidak detect module

Pastikan APK terinstall, module aktif di LSPosed, scope target sudah dipilih, lalu reboot emulator/device.

### xposed_init salah

File `app/src/main/assets/xposed_init` harus menunjuk:

```text
com.identityvault.hook.MainHook
```

### Setelah update APK

Setelah update APK, reboot LSPosed/device agar module versi baru dimuat.

### About phone/tablet belum berubah

Force stop Settings atau reboot:

```bash
adb shell am force-stop com.android.settings
```

Kalau masih belum berubah, cek Log IdentityVault. Jika `com.android.settings` belum pernah terlihat, centang `com.android.settings` di scope LSPosed lalu reboot emulator/device.

### Advertising ID kosong

Advertising ID hanya bisa di-hook jika target app menyertakan atau memanggil Google Advertising ID API:

```text
com.google.android.gms.ads.identifier.AdvertisingIdClient
```

Jika log menunjukkan `CLASS_NOT_FOUND` atau `AdvertisingIdClient class not found in this target app`, berarti target app tersebut tidak membaca Advertising ID lewat library itu. Gunakan app target yang benar-benar membaca Google Advertising ID untuk mengetes field ini.
