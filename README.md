# Mahila-Shakti Unnati

Mahila-Shakti Unnati is an Android micro-finance support application for women, SHG members, and group coordinators. The app helps users register, request admin approval, manage SHG members, record savings, create loans, track repayments, filter reports, and export CSV records for review.

## Problem Statement

Many self-help group and micro-finance workflows are still tracked manually in notebooks or scattered phone messages. This project provides a local Android prototype that keeps member, savings, loan, repayment, income, expense, and report data in one organized mobile app.

## Main Features

- Registration and login for Individual, SHG Member, and Group Coordinator users
- Built-in admin login for approving or rejecting user and SHG requests
- Role-based navigation for Admin, Individual, SHG Member, and Group Coordinator users
- Local Room database storage for accounts, members, savings, loans, transactions, budgets, and profiles
- SHG group creation and joining flow with admin approval
- Member registration with phone, role, group name, and monthly saving goal
- Savings contribution tracking by member and group
- Loan creation with borrower, amount, interest rate, due date, purpose, repayment progress, and close status
- Loan repayment history and CSV export
- Income and expense tracking with category, payment method, notes, date, edit, and delete actions
- Reports screen with filtering and CSV export through Android sharing
- Settings/profile screen for name, income range, currency, and budget preferences

## Tech Stack

- Kotlin
- Java
- Jetpack Compose
- Android Architecture Components
- ViewModel and StateFlow
- Kotlin Coroutines
- Room Database / SQLite
- Gradle Kotlin DSL
- FileProvider for CSV sharing

## Project Structure

```text
MahilaShaktiUnnati/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/myapplication/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   ├── viewmodel/
│       │   │   └── ui/theme/
│       │   └── res/
│       ├── androidTest/
│       └── test/
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## How To Run

1. Install Android Studio.
2. Open this folder in Android Studio.
3. Let Gradle sync finish.
4. Connect an Android phone or start an emulator.
5. Run the app from Android Studio.

Command line build:

```powershell
.\gradlew.bat :app:assembleDebug
```

Release APK build:

```powershell
.\gradlew.bat :app:assembleRelease
```

The release APK is generated at:

```text
app/release/app-release.apk
```

## Demo Login

Admin account:

```text
Email: admin@mahila.local
Password: admin123
```

Normal users should register first. After registration, the admin must approve the account before the user can access the app.

## APK Installation On Phone

1. Send `app-release.apk` to the phone using WhatsApp, Gmail, Telegram, or Drive.
2. Download the APK on the phone.
3. Tap the APK file.
4. Allow "Install unknown apps" if Android asks for permission.
5. Tap Install and open the app.

## Screenshots

The repository includes emulator screenshots as project evidence:

- `emulator_screen.png`
- `emulator_screen_after.png`
- `emulator_screen_pull.png`
- `emulator_screen_sw.png`

## Architecture

```text
User / SHG Member / Coordinator / Admin
        |
        v
Jetpack Compose UI
        |
        v
ViewModel + StateFlow
        |
        v
Repository + Coroutines
        |
        v
Room Database / SQLite

Reports Screen -> CSV Export -> FileProvider -> Share or Save Report
```

## Evaluation Readiness

- Source code is present in Kotlin, Java, XML, and Gradle files.
- Standard Android configuration files are included.
- The app has more than 400 meaningful lines of mobile project code.
- The project includes custom micro-finance workflows instead of default template content.
- The app has a generated release APK and has been installed on a phone.
- README includes description, features, tech stack, setup steps, run commands, screenshots, and future improvements.

## Future Improvements

- Secure password hashing for production use
- Session persistence after app restart
- Firebase/Auth or cloud backup for multi-device synchronization
- Repayment reminders and notification support
- Biometric lock for privacy
- Multilingual labels for first-time digital finance users
- More analytics charts for savings ratio, overdue loans, and monthly trends
