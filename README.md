# DuitKu — Smart Personal Finance Management

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Room Database](https://img.shields.io/badge/Storage-Room%20(Offline--First)-F4B400?style=for-the-badge&logo=sqlite&logoColor=white)
![Gemini AI](https://img.shields.io/badge/AI-Gemini%202.5%20Flash-8E75C2?style=for-the-badge&logo=google&logoColor=white)
![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)

**An elegant, privacy-first personal finance application crafted with modern Android architecture, Material Design 3 (Material You), and intelligent receipt scanning powered by Google Gemini AI.**

[Features](#key-features) • [Download](#download--installation) • [Architecture](#architecture--tech-stack) • [Developer Setup](#developer-guide) • [Security & Privacy](#security--privacy)

</div>

---

## Overview

**DuitKu** is designed to bring clarity, discipline, and simplicity to everyday financial management. Built with an **offline-first philosophy**, DuitKu ensures your financial records remain completely private on your device while offering smart AI-assisted automation, intuitive multi-account balance tracking, debt management, and actionable financial analytics.

Whether you are tracking routine daily expenses, managing multiple digital wallets and bank accounts, settling group bills (*talangan*), or analyzing monthly spending trends, DuitKu provides a cohesive, fluid experience adapted to your device's visual style.

---

## Key Features

### Multi-Wallet & Account Management
- **Connected Accounts:** Organize cash on hand, bank accounts, digital e-wallets, and savings pockets in one unified view.
- **Inter-Wallet Transfers:** Transfer funds between wallets seamlessly with automated admin fee deduction and real-time balance updates.
- **Privacy Mode:** Conceal sensitive balances and pocket values on demand with an intuitive one-tap toggle.

### Smart Transaction Tracking
- **Complete Categorization:** Track **Income**, **Expense**, and **Transfers** with custom-colored icons and user-definable category tags.
- **Comprehensive Details:** Record merchant names, administrative fees, notes, and exact dates/timestamps with an interactive header date picker.
- **Multi-Filter & Instant Search:** Quickly filter transaction histories by wallet, category, date range, or keywords.

### Gemini AI Receipt Scanner
- **Zero-Manual Entry:** Take a photo or upload single/batch receipt images directly from your camera or gallery.
- **Smart Data Extraction:** Leverages Google's **Gemini 2.5 Flash** model to parse total amounts, merchant/store names, transaction types, and automatically match them to relevant categories.
- **Batch Processing:** Scan multiple shopping receipts simultaneously and review itemized totals before saving.

### Debt & Bill Tracking (Hutang & Tagihan)
- **Receivables & Payables:** Monitor loans given to friends or debts owed to creditors with designated due dates.
- **Talangan & Split Expenses:** Record group expenses where you fronted the cost, complete with partial repayment history.
- **Payment Lifecycle:** Track full settlement status, partial installments, and remaining outstanding amounts.

### Visual Analytics & Budgeting
- **Interactive Cash Flow:** Monitor monthly income vs. expense balance charts with intuitive percentage breakdowns.
- **Category Spending Distribution:** Visual representations identify top expenditure avenues at a glance.
- **Budget Alerts:** Define monthly spending limits per category with progress bars and threshold warnings.

### Biometrics & Data Protection
- **Biometric Authentication:** Lock application access using Fingerprint or Face Recognition via AndroidX Biometric.
- **PIN / Password Fallback:** Secure local authentication for devices without hardware biometrics.
- **Encrypted Backups:** Export full database backups in standard or AES-encrypted JSON formats for safe restoration.

### Material Design 3 & Personalization
- **Dynamic Color (Material You):** Cohesive aesthetics adapting harmoniously between Light Mode and Dark Mode.
- **Custom Accent Themes:** Choose between distinct color profiles including *Classic*, *Mint*, *Ocean*, *Sunset*, and *Sakura*.
- **Bilingual Interface:** Instant localized switching between **Bahasa Indonesia** and **English**.

---

## Architecture & Tech Stack

DuitKu adheres strictly to modern Android development best practices and the **MVVM (Model-View-ViewModel)** architectural pattern.

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) | 100% Kotlin with Coroutines & StateFlow |
| **UI Toolkit** | [Jetpack Compose](https://developer.android.com/jetpack/compose) | Declarative UI with Material 3 components |
| **Local Database** | [Room Persistence](https://developer.android.com/training/data-storage/room) | SQLite abstraction layer with KSP code generation |
| **AI Integration** | [Google Gemini API](https://ai.google.dev/) | Multimodal receipt OCR via Gemini 2.5 Flash |
| **Networking** | [OkHttp3](https://square.github.io/okhttp/) & [Retrofit](https://square.github.io/retrofit/) | Resilient HTTP communications & serialization |
| **Security** | [AndroidX Biometric](https://developer.android.com/jetpack/androidx/releases/biometric) | Hardware-backed biometric authentication prompts |
| **Build System** | [Gradle (Kotlin DSL)](https://gradle.org/) | Type-safe configuration with Version Catalogs |

```
app/src/main/java/com/example/
├── data/
│   ├── dao/          # Room Database DAOs (Transaction, Wallet, Category, Debt)
│   ├── database/     # AppDatabase definition and type converters
│   ├── model/        # Entities & data transfer objects
│   └── repository/   # Repository pattern implementation
├── ui/
│   ├── components/   # Reusable Compose components & dialogs
│   ├── screens/      # Feature screens (Dashboard, Analytics, Wallets, Settings)
│   ├── theme/        # Material 3 Color Schemes, Typography, & Theme engines
│   ├── util/         # Gemini Client, Biometric helpers, Formatters
│   └── viewmodel/    # Centralized FinanceViewModel & UI state management
└── MainActivity.kt   # Single Activity entry point with Edge-to-Edge display
```

---

## Download & Installation

### Option 1: Direct APK Download (For End Users)

1. Navigate to the **[Releases](../../releases)** page of this repository.
2. Download the latest `app-release.apk` under the **Assets** section.
3. Open the APK file on your Android device running **Android 8.0 (Oreo / API 26) or higher**.
4. If prompted, grant permission to *Install unknown apps* for your browser/file manager.
5. Launch **DuitKu** and begin taking control of your financial journey.

---

## Developer Guide

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** OpenJDK 17 or higher
- **Android SDK:** Compile SDK 34 / Target SDK 34 / Minimum SDK 26
- **Gemini API Key:** Obtain an API key from [Google AI Studio](https://aistudio.google.com/)

### Setting Up the Project

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/DuitKu.git
   cd DuitKu
   ```

2. **Configure Environment Secrets:**
   Create a `.env` file in the root directory of the project (refer to `.env.example`):
   ```env
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
   > **Note:** The Secrets Gradle Plugin automatically reads `.env` and exposes `BuildConfig.GEMINI_API_KEY` securely without hardcoding credentials into version control.

3. **Build and Run:**
   - Open the project in **Android Studio**.
   - Sync the project with Gradle files (`Sync Project with Gradle Files`).
   - Select your target device or emulator and click **Run** (`Shift + F10`).

4. **Command-Line Compilation:**
   - Assemble Debug APK:
     ```bash
     ./gradlew assembleDebug
     ```
   - Assemble Production Release APK:
     ```bash
     ./gradlew assembleRelease
     ```
   - Run Unit & Robolectric Tests:
     ```bash
     ./gradlew testDebugUnitTest
     ```

---

## Security & Privacy

- **Offline-First Storage:** All transaction entries, wallet balances, and sensitive notes are stored locally in an encrypted Room SQLite database on your device.
- **Zero Third-Party Tracking:** DuitKu does not embed advertising SDKs, invasive telemetry trackers, or third-party analytics platforms.
- **Ephemeral AI Queries:** When using the Gemini receipt scanner, images are sent directly via encrypted HTTPS to Google's Gemini API strictly for optical extraction and are not retained or utilized for external profiling.
- **Hardware-Protected Access:** Biometric authentication uses Android's native Keystore and hardware-backed biometric cryptographic prompts.

---

## Contributing

Contributions make the open-source community an incredible place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

Distributed under the **Apache License 2.0**. See `LICENSE` for more information.

---

<div align="center">

Built for smart, mindful personal finance management.

</div>
