# CashFlow by Solvix Studio (Android)

CashFlow adalah aplikasi pencatatan keuangan harian dengan fokus pengalaman mobile yang rapi, aman, dan cepat. Cocok untuk mencatat pemasukan, pengeluaran, tabungan/target, dan laporan, dengan tampilan yang bisa disesuaikan tema.

## Highlight
- Mobile-first UI yang nyaman di layar kecil
- Multi theme (light/dark/space) dengan icon & warna yang konsisten
- Ringkasan cepat: income, expense, balance
- Laporan visual bulanan/tahunan + export PDF/CSV
- Proteksi privasi pada onboarding/login
- Opsi login fingerprint (jika perangkat mendukung)
- Data user tersimpan online (Supabase)

## Fitur Utama
- Pemasukan & Pengeluaran (CRUD)
- History transaksi + filter rentang tanggal
- Target / Dreams tracker
- Kalkulator
- Profil & Pengaturan
- Multi-bahasa (Indonesia / English)

## Tech Stack
- Kotlin + Jetpack Compose
- Material 3
- Supabase (Postgres)

## Cara Menjalankan
1) Buka project Android di Android Studio.
2) Sync Gradle, lalu pilih device/emulator.
3) Jalankan konfigurasi `app` (Debug).

## Build APK (Release)
1) Android Studio → Build → Generate Signed App Bundle / APK.
2) Pilih `APK`, lalu `release`.
3) Simpan file `.apk` hasil build dan install ke perangkat.

## Catatan
- Pastikan perangkat memiliki akses internet untuk sign in / sign up.
- Jika fingerprint diaktifkan, perangkat harus sudah punya lockscreen + fingerprint terdaftar.
