package com.example.ui.util

object Localization {
    fun getString(key: String, isIndonesian: Boolean): String {
        val strings = if (isIndonesian) IndonesianStrings else EnglishStrings
        return strings[key] ?: EnglishStrings[key] ?: key
    }

    private val EnglishStrings = mapOf(
        // General options
        "app_lock_title" to "DuitKu",
        "settings_title" to "Settings & Security",
        "settings_desc" to "Manage lock PINs, backup database, and restore your transaction logs.",
        "tab_security" to "Security",
        "tab_backup" to "Backup",
        "tab_restore" to "Restore",
        
        // Security sub-items
        "sec_pin_active" to "Lock PIN Active",
        "sec_pin_active_desc" to "Your DuitKu data is protected locally. A PIN is required to open the application.",
        "sec_disable_lock" to "Disable Application PIN Lock",
        "sec_unregistered_title" to "Configure Security PIN:",
        "sec_label_pin" to "Enter PIN",
        "sec_label_confirm" to "Confirm PIN",
        "sec_btn_create" to "Create Application PIN",
        "sec_pin_empty" to "PIN field cannot be empty!",
        "sec_pin_mismatch" to "PIN confirmation does not match!",
        "sec_pin_success" to "App lock PIN enabled successfully!",
        "sec_pin_disabled" to "App lock disabled!",
        "sec_pin_numeric" to "PIN must be numeric only!",
        "sec_pin_length_invalid" to "PIN must be exactly 6 digits!",
        
        // Settings language card
        "lang_card_title" to "Interface Language",
        "lang_card_subtitle" to "Choose your professional interface language",
        "lang_en" to "English (US)",
        "lang_id" to "Bahasa Indonesia",
        
        // Login Screen
        "login_welcome" to "Welcome Back,",
        "login_desc" to "Please enter your numeric application PIN to access your personal dashboard.",
        "login_label_pin" to "Application PIN",
        "login_btn_submit" to "Unlock Application",
        "login_incorrect_pin" to "Incorrect PIN!",
        "login_access_granted" to "Permission granted!",
        "login_footer_secured" to "Your transactions & records are fully secured locally",
        "close" to "Close"
    )

    private val IndonesianStrings = mapOf(
        // General options
        "app_lock_title" to "DuitKu",
        "settings_title" to "Pengaturan & Keamanan",
        "settings_desc" to "Kelola kunci keamanan PIN, cadangan database, dan pemulihan riwayat keuangan Anda.",
        "tab_security" to "Keamanan",
        "tab_backup" to "Backup",
        "tab_restore" to "Restore",
        
        // Security sub-items
        "sec_pin_active" to "PIN Keamanan Aktif",
        "sec_pin_active_desc" to "Data keuangan DuitKu terproteksi. PIN diperlukan di awal membuka aplikasi.",
        "sec_disable_lock" to "Nonaktifkan Kunci PIN Aplikasi",
        "sec_unregistered_title" to "Buat PIN Baru:",
        "sec_label_pin" to "Masukkan PIN",
        "sec_label_confirm" to "Ulangi PIN Baru",
        "sec_btn_create" to "Buat Kunci PIN",
        "sec_pin_empty" to "PIN tidak boleh kosong!",
        "sec_pin_mismatch" to "Konfirmasi PIN tidak sesuai!",
        "sec_pin_success" to "PIN keamanan berhasil diaktifkan!",
        "sec_pin_disabled" to "Kunci PIN dinonaktifkan!",
        "sec_pin_numeric" to "PIN hanya boleh berisi angka!",
        "sec_pin_length_invalid" to "PIN harus tepat 6 digit!",
        
        // Settings language card
        "lang_card_title" to "Bahasa Tampilan",
        "lang_card_subtitle" to "Pilih bahasa profesional antarmuka Anda",
        "lang_en" to "English (US)",
        "lang_id" to "Bahasa Indonesia",
        
        // Login Screen
        "login_welcome" to "Selamat Datang Kembali,",
        "login_desc" to "Silakan masukkan PIN angka Anda untuk mengakses dasbor keuangan DuitKu.",
        "login_label_pin" to "PIN Aplikasi",
        "login_btn_submit" to "Buka Kunci Aplikasi",
        "login_incorrect_pin" to "PIN salah!",
        "login_access_granted" to "Akses diberikan!",
        "login_footer_secured" to "Transaksi & data Anda terproteksi aman secara lokal",
        "close" to "Tutup"
    )
}
