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
        "settings_desc" to "Manage PIN lock, backups, and transaction history.",
        "tab_security" to "Security",
        "tab_backup" to "Backup",
        "tab_restore" to "Restore",
        
        // Security sub-items
        "sec_pin_active" to "PIN Lock Active",
        "sec_pin_active_desc" to "Data protected locally. PIN is required to open app.",
        "sec_disable_lock" to "Disable App PIN Lock",
        "sec_unregistered_title" to "Create New PIN:",
        "sec_label_pin" to "Enter PIN",
        "sec_label_confirm" to "Confirm PIN",
        "sec_btn_create" to "Create PIN",
        "sec_pin_empty" to "PIN cannot be empty!",
        "sec_pin_mismatch" to "PINs do not match!",
        "sec_pin_success" to "App PIN enabled successfully!",
        "sec_pin_disabled" to "PIN lock disabled!",
        "sec_pin_numeric" to "PIN must be numeric only!",
        "sec_pin_length_invalid" to "PIN must be exactly 6 digits!",
        "sec_biometric_title" to "Biometric Authentication",
        "sec_biometric_desc" to "Use fingerprint or face recognition to unlock.",
        "sec_biometric_enable" to "Enable Biometric Unlock",
        "sec_biometric_disable" to "Disable Biometric Unlock",
        "sec_biometric_prompt" to "Unlock DuitKu",
        "sec_biometric_error_setup" to "Biometrics not enrolled or supported on this device.",
        "sec_biometric_success" to "Biometric unlock enabled!",
        "sec_biometric_disabled" to "Biometric unlock disabled!",
        
        // Settings language card
        "lang_card_title" to "App Language",
        "lang_card_subtitle" to "Choose your interface language",
        "lang_en" to "English (US)",
        "lang_id" to "Bahasa Indonesia",
        
        "theme_card_title" to "App Theme",
        "theme_card_subtitle" to "Choose your color theme",
        "theme_classic" to "Classic Purple",
        "theme_dynamic" to "Material You (Dynamic)",
        "theme_mint" to "Fresh Mint",
        "theme_ocean" to "Royal Ocean",
        "theme_sunset" to "Sunset Glow",
        "theme_sakura" to "Sakura Dream",
        
        "style_card_title" to "Visual Interface Style",
        "style_card_subtitle" to "Choose between modern wallet or classic design",
        "style_fresh" to "Fresh Modern (Playful)",
        "style_classic" to "Material You (Clean)",
        
        // Login Screen
        "login_welcome" to "Welcome,",
        "login_desc" to "Enter your 6-digit PIN to access DuitKu.",
        "login_label_pin" to "Application PIN",
        "login_btn_submit" to "Unlock Application",
        "login_incorrect_pin" to "Incorrect PIN!",
        "login_access_granted" to "Access granted!",
        "login_footer_secured" to "Data is securely stored locally on this device",
        "close" to "Close"
    )

    private val IndonesianStrings = mapOf(
        // General options
        "app_lock_title" to "DuitKu",
        "settings_title" to "Pengaturan & Keamanan",
        "settings_desc" to "Kelola kunci PIN, cadangan data, dan riwayat keuangan.",
        "tab_security" to "Keamanan",
        "tab_backup" to "Backup",
        "tab_restore" to "Restore",
        
        // Security sub-items
        "sec_pin_active" to "PIN Keamanan Aktif",
        "sec_pin_active_desc" to "Data terlindungi. PIN diminta saat membuka aplikasi.",
        "sec_disable_lock" to "Nonaktifkan Kunci PIN",
        "sec_unregistered_title" to "Buat PIN Baru:",
        "sec_label_pin" to "Masukkan PIN",
        "sec_label_confirm" to "Ulangi PIN",
        "sec_btn_create" to "Simpan Kunci PIN",
        "sec_pin_empty" to "PIN tidak boleh kosong!",
        "sec_pin_mismatch" to "Konfirmasi PIN tidak cocok!",
        "sec_pin_success" to "PIN keamanan berhasil diaktifkan!",
        "sec_pin_disabled" to "Kunci PIN dinonaktifkan!",
        "sec_pin_numeric" to "PIN hanya boleh berupa angka!",
        "sec_pin_length_invalid" to "PIN harus tepat 6 digit!",
        "sec_biometric_title" to "Autentikasi Biometrik",
        "sec_biometric_desc" to "Gunakan sidik jari atau wajah untuk membuka aplikasi.",
        "sec_biometric_enable" to "Aktifkan Sidik Jari / Biometrik",
        "sec_biometric_disable" to "Nonaktifkan Biometrik",
        "sec_biometric_prompt" to "Buka Kunci DuitKu",
        "sec_biometric_error_setup" to "Biometrik belum terdaftar atau tidak didukung.",
        "sec_biometric_success" to "Kunci biometrik berhasil aktif!",
        "sec_biometric_disabled" to "Kunci biometrik dinonaktifkan!",
        
        // Settings language card
        "lang_card_title" to "Bahasa Tampilan",
        "lang_card_subtitle" to "Pilih bahasa tampilan aplikasi",
        "lang_en" to "English (US)",
        "lang_id" to "Bahasa Indonesia",
        
        "theme_card_title" to "Tema Aplikasi",
        "theme_card_subtitle" to "Pilih skema warna tampilan DuitKu",
        "theme_classic" to "Ungu Klasik",
        "theme_dynamic" to "Material You (Dinamis)",
        "theme_mint" to "Fresh Mint",
        "theme_ocean" to "Royal Ocean",
        "theme_sunset" to "Sunset Glow",
        "theme_sakura" to "Sakura Pink",
        
        "style_card_title" to "Gaya Tampilan Visual",
        "style_card_subtitle" to "Pilih gaya dompet digital atau tampilan klasik",
        "style_fresh" to "Modern Digital (Fresh & Berwarna)",
        "style_classic" to "Material You (Minimalis & Bersih)",
        
        // Login Screen
        "login_welcome" to "Selamat Datang,",
        "login_desc" to "Masukkan 6 digit PIN untuk masuk ke aplikasi.",
        "login_label_pin" to "PIN Aplikasi",
        "login_btn_submit" to "Buka Kunci",
        "login_incorrect_pin" to "PIN salah!",
        "login_access_granted" to "Akses diberikan!",
        "login_footer_secured" to "Data tersimpan aman di perangkat Anda",
        "close" to "Tutup"
    )
}
