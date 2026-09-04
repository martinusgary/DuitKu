# DuitKu

A personal finance management application based on Material You (Material 3) designed to simplify tracking your wallets, transactions, debts, and bills. DuitKu features data export/import capabilities as well as smart artificial intelligence integration via the Gemini API.

## Key Features
* **Transaction & Wallet Management:** Record income, expenses, and track balances across multiple wallets.
* **Debt & Bill Tracking:** Manage your payables and receivables, and get reminders for routine bills.
* **Material You (Material 3):** A modern, responsive, and dynamic user interface that adapts to your device's visual preferences.
* **Smart Gemini AI Integration:** Leverages server-side Gemini API capabilities for intelligent financial processing and insights.
* **Data Export & Import:** Securely back up and restore your financial history with ease.

---

## 🚀 Download & Installation (For Users)

DuitKu **Version 1.0** is officially released and ready to use on your Android device without compiling any code.

1. Go to the **[Releases](../../releases)** tab in this repository (or adjust the link accordingly).
2. Under the **v1.0.0** release, download the `app-release.apk` file from the *Assets* section.
3. Open the downloaded APK file on your Android device.
4. If prompted with a security warning, allow installation from unknown sources in your device settings.
5. Follow the on-screen instructions, and DuitKu is ready to use!

---

## 💻 Local Development (For Developers)

If you want to view the source code, contribute, or build the application yourself, please follow the guide below.

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open **Android Studio**.
2. Select **Open** and choose the directory containing this project.
3. Allow Android Studio to fix any incompatibilities as it imports the project and wait for the Gradle sync to finish.
4. Create a file named `.env` in the root project directory and set your Gemini API key in that file (see `.env.example` for an example):
   ```env
   GEMINI_API_KEY=insert_your_gemini_api_key_here
