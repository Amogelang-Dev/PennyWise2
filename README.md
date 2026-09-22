# PennyWise

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="140" alt="PennyWise application icon">
</p>

<h2 align="center">Gamified Personal Finance for Smarter Spending</h2>

<p align="center">
  <strong>Track expenses, manage budgets, build financial habits, and make progress visible.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Android">
  <img src="https://img.shields.io/badge/Language-Kotlin-purple.svg" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-XML%20%2B%20Jetpack%20Compose-blue.svg" alt="UI">
  <img src="https://img.shields.io/badge/Database-SQLite-orange.svg" alt="SQLite">
  <img src="https://img.shields.io/badge/Cloud-Firebase%20Firestore-yellow.svg" alt="Firebase">
</p>

## Purpose of the App

PennyWise is an Android personal-finance application designed to help users understand and improve their spending habits.

Many people know that they should budget, but traditional finance applications can feel complicated, passive, or difficult to use consistently. PennyWise addresses this problem by combining practical [...]

The app allows users to:

- Record expenses manually.
- Detect supported bank transaction notifications automatically.
- Set monthly income and spending targets.
- Attach receipts to transactions.
- Review spending by category.
- Generate downloadable financial statements.
- Earn experience points for positive financial actions.
- Progress through financial milestones.
- Synchronise selected user-progress data with Firebase Firestore.
- Find nearby ATMs using map applications.
- View daily financial advice.

The intended users are individuals who want a simple, visual, and motivating way to monitor everyday spending.

## Main Features

### Automated Expense Tracking

`NotificationService.kt` listens for notifications from supported South African banking applications, including:

- FNB
- Capitec
- Standard Bank
- Absa
- Nedbank
- TymeBank
- Discovery Bank

When a supported bank notification contains a South African Rand amount, the service extracts the amount and records it as an automated expense.

The service also awards XP for successfully detected transactions.

> Users must explicitly enable notification access in Android Settings for this feature to work.

### Manual Expense Recording

Users can also create expense records manually. Each expense can include:

- Amount
- Category
- Description
- Date
- Start and end time
- Receipt image URI

Manual recording provides a fallback when a transaction notification is unavailable or when the user wants to record cash spending.

### Budget Management

The budget screen allows users to define:

- Monthly income
- Minimum spending or savings goal
- Maximum spending limit

The app validates budget values before saving them. For example, the minimum value must be lower than the maximum value.

Budget settings are saved locally using Android `SharedPreferences`.

### Reports and Statements

`ReportActivity.kt` reads expenses from the local SQLite database and provides:

- Date-range filtering.
- Category totals.
- Percentage breakdowns.
- Bar-chart visualisation.
- Budget status feedback.
- Transaction history.
- Receipt viewing.
- Downloadable text statements.

The report screen helps users transform individual expense records into useful financial information.

### Gamified Financial Progress

`ProgressActivity.kt` uses XP and milestones to create a visual financial journey.

The current milestone levels are:

| Milestone | XP Requirement |
|---|---:|
| Financial Novice | 10 XP |
| Saver | 100 XP |
| Strategist | 200 XP |
| Wealth Builder | 500 XP |
| Generational Wealth Builder | 1,000 XP |

The screen renders an interactive S-curve using Jetpack Compose Canvas. Completed, current, and locked milestones use different visual states to make progress easy to understand.

### Receipt Management

Receipts can be attached to expense records and accessed later from the reports screen. Users can view photo receipts, view transaction details, or download a text receipt.

### Dark Mode

`PennyWiseApp.kt` applies the user's dark-mode preference globally when the application starts.

## Design Considerations

### Simplicity

Financial applications should not require users to understand complex accounting concepts. PennyWise uses straightforward labels, currency formatting, navigation menus, and visual feedback to reduce c[...]

### Local-First Expense Storage

Expenses are stored locally using SQLite through `DatabaseHelper.kt`. This provides:

- Fast access to transaction data.
- Offline availability.
- Reduced network dependency.
- Better responsiveness when viewing reports.

### Progressive Disclosure

The app separates functionality into focused screens:

- Registration and login.
- Dashboard.
- Budget configuration.
- Manual expense entry.
- Reports.
- Progress.
- Profile.
- Settings.
- About and help information.

This prevents every feature from being displayed on a single screen.

### Visual Feedback

The application uses multiple forms of visual feedback:

- Budget status colours.
- Spending charts.
- Progress indicators.
- XP values.
- Locked and completed milestone icons.
- Toasts and Snackbars for user actions.

The aim is to make financial information understandable at a glance.

### Gamification

The XP system encourages repeated engagement rather than treating budgeting as a one-time activity. Users earn XP for actions such as setting goals and recording transactions.

The S-curve progression map provides a clear long-term goal while keeping early milestones achievable.

### Accessibility and Device Compatibility

The project targets Android API 24 and above and uses:

- Edge-to-edge layouts.
- Window inset handling.
- Android resource files for strings and colours.
- Material components.
- Standard Android navigation patterns.

The application should continue to improve accessibility by adding content descriptions, testing larger font sizes, and verifying colour contrast in both light and dark modes.

### Privacy and Security Considerations

The app works with sensitive financial information. Important privacy considerations include:

- Notification access should be clearly explained to users.
- Only supported banking package names should be processed.
- Expense data should not be exposed through debug logs in production builds.
- Firebase security rules should restrict users to their own records.
- Sensitive data should be encrypted where appropriate.
- Passwords must never be stored directly in Firestore or plain-text local preferences.

The current implementation should be improved by replacing direct password storage in `FirstActivity.kt` with Firebase Authentication. Passwords should be handled by Firebase Authentication and never [...]

## Technical Architecture

```text
Android UI
├── XML layouts and Material components
├── Jetpack Compose progress map
└── Activity-based navigation

Application Services
├── NotificationService
├── Firebase Firestore synchronisation
└── External map and advice integrations

Data Storage
├── SQLite through DatabaseHelper
├── SharedPreferences for local settings and XP
└── Firebase Firestore for selected cloud data

Reporting
├── MPAndroidChart
├── Date-range filtering
├── Budget status calculation
└── Downloadable statements
```

### Important Source Files

| File | Responsibility |
|---|---|
| `PennyWiseApp.kt` | Applies application-wide settings such as dark mode |
| `FirstActivity.kt` | Registration and initial user setup |
| `SecondActivity.kt` | Login flow |
| `ThirdActivity.kt` | Main dashboard |
| `ForthActivity.kt` | Expense entry |
| `BudgetActivity.kt` | Income and budget configuration |
| `ReportActivity.kt` | Reports, charts, receipts, and statements |
| `ProgressActivity.kt` | XP progression and milestone map |
| `DatabaseHelper.kt` | SQLite expense storage |
| `NotificationService.kt` | Automated bank-notification processing |
| `AndroidManifest.xml` | Activities, permissions, and notification service registration |

## Technology Stack

- **Language:** Kotlin 2.0.21
- **Build system:** Gradle Kotlin DSL
- **Android Gradle Plugin:** 8.7.2
- **Compile SDK:** Android API 35
- **Minimum SDK:** Android API 24
- **UI:** XML layouts, Material Components, and Jetpack Compose
- **Local database:** SQLite using `SQLiteOpenHelper`
- **Cloud services:** Firebase Firestore and Firebase Analytics
- **Charts:** MPAndroidChart
- **Networking:** OkHttp
- **Testing:** JUnit, AndroidX Test, and Espresso

## External APIs Used

The app currently integrates with three external API services:

- **Firebase Firestore API** — used for user registration, profile data, and syncing XP/progress data to the cloud.
- **Advice Slip API** — the public daily advice service used to show motivational financial tips on the dashboard (`https://api.adviceslip.com/advice`).
- **Google Maps / OpenStreetMap location services** — used to open nearby ATM locations through the device map app or an OpenStreetMap browser fallback.

These three services are the main external integrations used by PennyWise for cloud storage, daily guidance, and ATM discovery.

## Installation and Setup

### Requirements

- Android Studio
- JDK 11
- Android SDK 35
- An Android emulator or physical Android device
- A Firebase project for cloud functionality

### Clone the Project

```bash
git clone https://github.com/Amogelang-Dev/PennyWise2.git
cd PennyWise2
```

Open the project in Android Studio and allow Gradle to synchronise.

### Firebase Configuration

1. Create a Firebase project.
2. Register the Android application using the package name:

```text
com.example.navigation
```

3. Enable the required Firebase services.
4. Download `google-services.json`.
5. Place the file in the appropriate Android app module location.
6. Configure Firestore security rules before using the application with real financial data.

### Build the Application

Linux and macOS:

```bash
./gradlew assembleDebug
```

Windows:

```bash
gradlew.bat assembleDebug
```

### Run Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## GitHub Utilisation

GitHub provides the project with:

- Version control for Kotlin and Android source code.
- A central location for documentation.
- Change history and traceability.
- Collaboration through branches and pull requests.
- Issue tracking for bugs and feature requests.
- Code review before changes are merged.
- Project visibility for contributors and assessors.

A recommended workflow is:

1. Create an issue describing a feature or bug.
2. Create a dedicated branch.
3. Implement the change.
4. Run the Gradle build and tests locally.
5. Open a pull request.
6. Review the changes.
7. Merge the pull request after successful checks.

Example branch naming:

```text
feature/budget-alerts
feature/receipt-gallery
bugfix/notification-parser
docs/update-readme
```

## GitHub Actions

The repository currently does not contain a `.github/workflows` directory, so GitHub Actions are not yet configured.

GitHub Actions should be used to automatically verify every pull request and push. A suitable Android workflow would:

1. Check out the repository.
2. Install the required JDK.
3. Configure the Android SDK.
4. Run unit tests.
5. Build the debug APK.
6. Upload the APK as a build artifact.

A future workflow could also include:

- Kotlin formatting checks.
- Android lint.
- Instrumentation tests.
- Dependency vulnerability scanning.
- Release APK generation.
- Automated documentation checks.

GitHub Actions would improve the project by ensuring that changes are tested consistently instead of relying only on manual testing.

## Recommended Quality Gates

Before merging a pull request, the project should pass:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

For connected Android testing:

```bash
./gradlew connectedAndroidTest
```

Sensitive files such as Firebase configuration files and signing keys should be managed carefully and should not expose production credentials.

## Current Limitations and Future Improvements

Potential improvements include:

- Replace plain-text password storage with Firebase Authentication.
- Add stronger Firebase Firestore security rules.
- Add unit tests for `DatabaseHelper`.
- Add tests for currency and notification amount parsing.
- Add automated UI tests for registration, budgeting, and reports.
- Add a GitHub Actions workflow.
- Introduce a repository layer and ViewModels as the application grows.
- Improve database migration handling instead of dropping the table in `onUpgrade`.
- Add encrypted local storage for sensitive preferences.
- Improve support for additional banks and notification formats.
- Add recurring expenses and income tracking.
- Add export formats such as CSV and PDF.
- Add a dedicated privacy policy and data-retention explanation.

---
##Demonstration Video

Here is the link to the official demonstration video: https://youtu.be/Q26cPzqtEZ8?si=llGYXxEvvPXlTBs_


## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

---

<p align="center">
  Built with Kotlin, Android, Firebase, and a commitment to better financial habits.
</p>
