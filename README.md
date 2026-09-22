# PennyWise - Gamified Personal Finance

PennyWise is a modern Android application designed to help users take control of their financial lives through automated expense tracking, goal setting, and an engaging gamified experience.

## 🚀 Features

*   **Automated Expense Tracking**: Captures transaction notifications from major South African banks (FNB, Capitec, Standard Bank, Absa, Nedbank, TymeBank, Discovery Bank) to auto-log expenses.
*   **Gamified Journey**: Visualizes financial progress on an interactive S-curve map. Users earn XP for logging expenses and staying consistent, leveling up from "Financial Novice" to "Generational Wealth Builder."
*   **Budget Management**: Set monthly income, minimum savings goals, and maximum spending limits with real-time visual feedback on the dashboard.
*   **Digital Receipt Management**: Attach photos of receipts to transactions for better record-keeping.
*   **Financial Tips**: Daily financial advice fetched from the AdviceSlip API.
*   **ATM Finder**: Quickly locate the nearest ATM using geo-intents (Google Maps/OpenStreetMap).
*   **Comprehensive Reports**: Detailed category breakdowns and downloadable spending statements.
*   **Cloud Sync**: Firebase Firestore integration to sync XP and user profile data across devices.

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Modern UI) & XML (Legacy UI support)
*   **Local Database**: SQLite (via `DatabaseHelper`)
*   **Backend**: Firebase Firestore (Auth & Cloud Sync)
*   **Networking**: OkHttp (for AdviceSlip API)
*   **Architecture**: Activity-based with specialized services (Notification Listener)

## 📦 Installation & Setup

1.  **Clone the repository**: `git clone https://github.com/your-repo/pennywise.git`
2.  **Open in Android Studio**: Ensure you have the latest version of Arctic Fox or higher.
3.  **Firebase Setup**: 
    *   Create a project in the Firebase Console.
    *   Add your Android app's package name (`com.example.navigation`).
    *   Download `google-services.json` and place it in the `app/` directory.
    *   Enable Firestore and Authentication (Email/Password).
4.  **Permissions**:
    *   The app requires `BIND_NOTIFICATION_LISTENER_SERVICE` to capture bank transactions.
    *   Grant Notification Access in Android Settings after installation.

## 📱 How It Works

1.  **Register/Login**: Securely create an account using Firebase.
2.  **Set Budgets**: Input your monthly income and limits in the Budget section.
3.  **Log Expenses**: Add manual expenses via the '+' button or let the `NotificationService` handle it automatically.
4.  **Level Up**: Watch your progress on the S-curve in the 'Progress' tab as you earn XP.
5.  **Analyze**: Check the 'Reports' section to see where your money goes.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---
*Built with ❤️ by the PennyWise Team.*
