# 🪙 PennyWise: Gamified Personal Finance Tracker

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Badge" />
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin Badge" />
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase Badge" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose Badge" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Android">
  <img src="https://img.shields.io/badge/Language-Kotlin-purple.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/Database-SQLite-orange.svg" alt="SQLite">
</p>

---

## 📋 Project Documentation

### 1. Purpose of the Application
PennyWise is a comprehensive personal finance management tool designed to empower users to take control of their financial lives. Its primary purpose is to transform the often-tedious task of expense tracking into an engaging, gamified experience that encourages long-term saving habits and financial literacy.

### 2. Problem Addressed
Many individuals struggle with "financial leakage"—small, unaccounted-for expenses that add up over time. Traditional budgeting tools are often perceived as boring or overly complex, leading to low user retention. PennyWise addresses:
* **Lack of Visibility:** Providing a clear, real-time overview of "In My Pocket" funds.
* **Manual Tracking Fatigue:** Using gamification (XP and levels) to motivate consistent logging.
* **Physical Receipt Loss:** Allowing users to digitize receipts via photo uploads.
* **Budget Overruns:** Providing visual warnings (Green/Yellow/Red status) before spending limits are reached.

### 3. All the Main Features
* **Financial Dashboard:** A high-level overview of total balance, monthly income, and current expenses.
* **Gamified Progress System:** A multi-tier progression system (from "Financial Novice" to "Wealth Builder") where users earn XP for logging expenses and meeting budget goals.
* **Receipt Management:** The ability to upload and store photos of physical receipts for digital record-keeping.
* **Smart Budgeting:** Set minimum savings goals and maximum spending limits with automated progress tracking.
* **Comprehensive Reporting:** Visual category breakdowns and the ability to download text-based financial statements for specific periods.
* **Daily Financial Tips:** An advice engine that provides daily insights to improve financial decision-making.
* **ATM Locator:** Integration to help users find the nearest banking points.
* **Multi-Language Support:** Fully localized for South African users, supporting English, Afrikaans, Sepedi, Setswana, isiXhosa, and isiZulu.

### 4. Design Considerations
* **Material Design:** The app follows modern Material 3 guidelines, using cards, floating action buttons, and a navigation drawer for an intuitive experience.
* **Visual Cues:** Uses color-coded progress bars (Blue for healthy spending, Red for critical limit breaches) to communicate financial status instantly.
* **Persistence:** Data is stored locally to ensure the app remains functional without an internet connection, prioritizing user privacy.
* **Gamification:** The XP system encourages repeated engagement rather than treating budgeting as a one-time activity. The S-curve progression map provides a clear long-term goal while keeping early milestones achievable.

### 5. Technical Architecture
The application follows a modular architecture:
* **Local Persistence:** SQLite database via `DatabaseHelper` for storing transactions and user profiles.
* **Background Processing:** `NotificationListenerService` for handling automated bank notification parsing.
* **Messaging Architecture:** Firebase Cloud Messaging (FCM) for delivering financial alerts.
* **Component-Based UI:** Modular Activity-based structure combined with Jetpack Compose for complex UI elements like the progress map.

### 6. Important Source Files
| File | Responsibility |
|---|---|
| `DatabaseHelper.kt` | Core data engine managing SQL operations. |
| `ThirdActivity.kt` | Main Dashboard controller for real-time balance calculations. |
| `BudgetActivity.kt` | Logic for setting and validating financial goals. |
| `ReportActivity.kt` | Data filtering and financial statement generation. |
| `NotificationService.kt` | Automated bank-notification processing. |
| `ProgressActivity.kt` | XP progression and S-curve milestone map. |

### 7. Technology Stack
* **Language:** Kotlin 2.0.21
* **UI Framework:** XML Layouts & Jetpack Compose
* **Local Database:** SQLite
* **Networking:** OkHttp
* **Charts:** MPAndroidChart
* **Build System:** Gradle (Kotlin DSL)

### 8. External APIs and Services Used
* **Firebase Firestore:** Cloud synchronization for user profiles and progress.
* **Advice Slip API:** Public daily advice service for financial tips.
* **Google Maps API:** Used for the ATM Finder functionality.
* **Firebase Auth:** Secure user authentication and Google Sign-In.

### 9. Installation and Setup
1. **Clone the Project**: `git clone https://github.com/Amogelang-Dev/PennyWise2.git`
2. **Environment**: Requires Android Studio (Ladybug+) and JDK 17.
3. **Firebase**: Place your `google-services.json` in the `app/` directory.
4. **Build**: Run `./gradlew assembleDebug` to generate the APK.
5. **Permissions**: Enable "Notification Access" in Android Settings for automated tracking.

### 10. Testing and Quality Gates
* **Unit Testing**: Validating `DatabaseHelper` logic and currency parsing.
* **Linting**: Automated code style and static analysis checks.
* **CI/CD**: GitHub Actions configured to verify builds on every push.
* **Input Validation**: Strict regex-based validation for all user authentication fields.

### 11. Current Limitations and Future Improvements
* **Current Limitations**: Manual receipt entry (no OCR yet) and local-first storage limitations.
* **Future Improvements**: 
    * **OCR Integration**: Automate receipt reading using Google ML Kit.
    * **AI Categorization**: Machine learning to predict expense categories.
    * **Direct Bank Integration**: Secure API links to major financial institutions.

---
## 🚀 Latest Updates & Improvements

### 🛠 UI & UX Enhancements
*   **Modern Dashboard Design**: Revamped the main interface with a high-contrast gradient balance card for better visibility.
*   **Visual Budget Tracking**: Integrated dynamic progress bars that change color (Green/Blue to Red) based on spending thresholds.
*   **Interactive Advice Hub**: Added a dedicated card for "Financial Tip of the Day" powered by the AdviceSlip API.
*   **Quick ATM Finder**: Direct integration with mapping services to locate the nearest banking points with a single tap.
*   **Custom Theming**: Improved Dark Mode support and added custom progress drawables for a more professional feel.

### 🌍 Multi-Language Expansion
Expanded accessibility to all major South African languages, including localized strings for:
*   **Afrikaans**
*   **Northern Sotho (Sepedi)**
*   **Tswana**
*   **Xhosa**
*   **Zulu**

### 🔐 Security & Reliability
*   **Secure Credential Handling**: Implemented `SecurityUtils` for robust password validation and secure string processing.
*   **Environment Safety**: Configured advanced `.gitignore` rules to protect sensitive files like `.jks` signing keys and `google-services.json`.
*   **Base Activity Architecture**: Introduced `BaseActivity` to ensure consistent theme application and locale switching across all screens.

### ⚙️ Technical Additions
*   **Background Synchronization**: Integrated `WorkManager` with `SyncWorker.kt` for efficient, reliable data syncing.
*   **Cloud Messaging**: Added `MyFirebaseMessagingService.kt` to handle real-time push notifications for budget alerts and tips.
*   **Automated Parsing**: Enhanced `NotificationParser.kt` with support for a wider range of South African banking message formats.

---
## 🎥 Demonstration Video
Official demonstration video: [Watch Here](https://youtu.be/Q26cPzqtEZ8?si=llGYXxEvvPXlTBs_)

---
<p align="center">
  Built with Kotlin, Android, and Firebase. © 2026 Pennywise Team.
</p>
