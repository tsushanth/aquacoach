# AquaCoach

AquaCoach is a simple, focused water-tracking app for Android. Log how much you drink throughout
the day, watch your progress fill in against a daily hydration goal, and get gentle reminders to
take a sip when you're falling behind.

## Features

- **Quick logging** — one-tap buttons for common amounts (100 ml, 250 ml, 350 ml, 500 ml), plus a
  custom-amount entry for any quantity (Premium).
- **Daily progress ring** — an animated ring on the Home screen shows today's intake against your
  goal, with the exact amount and remaining total spelled out.
- **Hydration history** — a day-by-day log of totals and goal completion, with the last 7 days
  free and full history unlocked with Premium.
- **Reminders** — configurable notifications nudge you to drink water, with quiet hours support
  and a custom interval (Premium).
- **ml / oz units** — switch your preferred measurement unit at any time; all amounts convert
  automatically.
- **Premium upgrade** — a subscription/lifetime paywall (Google Play Billing) unlocks custom
  amounts, full history, custom reminder scheduling, and removes ads.
- **Material 3 theming** — dynamic color on Android 12+, with dedicated light and dark color
  schemes on older versions, full dark mode support, and edge-to-edge UI.
- **Accessibility** — content descriptions on all interactive elements, haptic feedback on key
  actions, and proper keyboard/IME handling in input fields.

## Requirements

- **Android Studio** Koala (2024.1) or newer
- **JDK 17**
- **Android SDK** compile/target SDK 34, minimum SDK 24 (Android 7.0+)
- **Gradle** 8.7 (via the included wrapper)
- Kotlin 1.9.24, Android Gradle Plugin 8.5.2

## Build instructions

Clone the repository, then build from the command line or open it in Android Studio.

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires a connected device/emulator)
./gradlew connectedAndroidTest
```

Or open the project root in Android Studio and run the `app` configuration.

> Note: the app links to Google Play Billing for Premium purchases. Billing calls only resolve on
> a device/emulator signed in to Google Play with the app's release `applicationId`
> (`com.factory.aquacoach`); billing is not required to build or run the free feature set.

## Project structure

```
app/src/main/java/com/factory/aquacoach/
├── AquaCoachApp.kt              # Application class; wires up repositories/managers
├── MainActivity.kt              # Single-activity host, navigation graph, bottom nav
├── data/
│   ├── billing/                 # Google Play Billing integration, premium entitlement state
│   ├── local/                   # Room database, DAO, entities
│   ├── preferences/             # DataStore-backed app settings (goal, unit, reminders)
│   └── repository/              # Water entry repository (single source of truth for entries)
├── notification/                # Reminder notification channel, scheduling (WorkManager), worker
└── ui/
    ├── components/               # Shared composables (progress ring, unit formatting, Pro badge)
    ├── home/                     # Home screen: progress ring, quick add, today's log
    ├── history/                  # History screen: daily totals list
    ├── settings/                 # Settings screen: goal, units, reminders, quiet hours
    ├── paywall/                  # Premium paywall / billing UI
    └── theme/                    # Material 3 color scheme, typography, dynamic theming
```

The app follows a straightforward MVVM structure: each screen has a `ViewModel` that exposes a
single `StateFlow<UiState>`, built by combining the relevant repositories/managers with
`combine(...).stateIn(...)`. Screens are stateless composables that collect that state and forward
user actions back to the view model.
