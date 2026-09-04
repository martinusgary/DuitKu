# DuitKu

Aplikasi manajemen keuangan pribadi berbasis Material You (Material 3) yang dirancang untuk memudahkan pencatatan dompet, transaksi, hutang, dan tagihan. DuitKu dilengkapi dengan fitur ekspor/impor data serta integrasi kecerdasan buatan melalui Gemini API.

## Fitur Utama
* **Manajemen Transaksi & Dompet:** Pencatatan pemasukan, pengeluaran, dan saldo antar dompet.
* **Pantau Hutang & Tagihan:** Kelola status hutang piutang serta pengingat tagihan rutin.
* **Material You (Material 3):** Antarmuka modern, responsif, dan dinamis menyesuaikan preferensi visual perangkat Anda.
* **Integrasi Cerdas Gemini AI:** Memanfaatkan kapabilitas server-side Gemini API untuk pemrosesan keuangan yang lebih cerdas.
* **Ekspor & Impor Data:** Cadangkan dan pulihkan riwayat keuangan Anda dengan aman dan mudah.

---

## 🚀 Download & Instalasi (Untuk Pengguna)

DuitKu **Versi 1.0** sudah dirilis dan bisa langsung Anda gunakan di perangkat Android tanpa perlu melakukan kompilasi kode. 

1. Kunjungi tab **[Releases](../../releases)** pada repositori ini (atau sesuaikan dengan tautan rilis Anda).
2. Pada rilis **v1.0.0**, unduh file `app-release.apk` (atau nama file APK yang tersedia) di bagian *Assets*.
3. Buka file APK yang sudah diunduh di perangkat Android Anda.
4. Jika muncul peringatan keamanan, izinkan instalasi dari sumber tidak dikenal (*Install unknown apps*) di pengaturan perangkat Anda.
5. Ikuti petunjuk di layar, dan DuitKu siap digunakan!

---

## 💻 Pengembangan Lokal (Untuk Developer)

Jika Anda ingin melihat kode sumber, berkontribusi, atau membangun aplikasi ini sendiri, silakan ikuti panduan berikut.

**Prasyarat:** [Android Studio](https://developer.android.com/studio)

1. Buka aplikasi **Android Studio**.
2. Pilih **Open** dan arahkan ke direktori proyek DuitKu ini.
3. Tunggu proses sinkronisasi Gradle selesai dan izinkan Android Studio untuk memperbaiki inkonsistensi dependensi saat mengimpor proyek jika diperlukan.
4. Buat sebuah file baru bernama `.env` pada direktori *root* proyek. Masukkan kunci API Gemini Anda ke dalam file tersebut (Anda bisa melihat file `.env.example` sebagai referensi):
   ```env
   GEMINI_API_KEY=masukkan_api_key_gemini_anda_di_sini
