# IseuSchedule V2

An unofficial Android app for fast university schedule access, cabinet-related student flows, offline cache, OCR-assisted captcha input, and local lesson reminders.

![Android](https://img.shields.io/badge/Android-App-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-Offline%20Cache-6D4C41)
![ML Kit](https://img.shields.io/badge/ML%20Kit-Text%20Recognition-0F9D58)

> This is an unofficial project and is not affiliated with the university.

## Problem

Students and teachers need quick access to schedule data and, for authenticated student flows, cabinet-related academic information. In practice, browser-based schedule and cabinet pages can be slow, repetitive, and inconvenient on mobile devices. Users also need their latest data to remain useful when connectivity is unstable, and they benefit from timely reminders before lessons start.

## Solution

IseuSchedule V2 packages these flows into a native Android app built with Jetpack Compose. The app supports student and teacher schedule browsing, keeps selected data locally for offline-friendly usage, assists with captcha entry through OCR, and schedules local lesson notifications from cached timetable data. The UI also handles loading, error, and offline states instead of failing silently when the network is unavailable.

## Screenshots

| Home | Schedule |
|---|---|
| ![Home](docs/screenshots/01-home.png) | ![Schedule](docs/screenshots/02-schedule.png) |

| Offline State | Settings |
|---|---|
| ![Offline state](docs/screenshots/03-offline-state.png) | ![Settings](docs/screenshots/04-settings.png) |

| OCR Captcha | Notification |
|---|---|
| ![OCR captcha](docs/screenshots/05-ocr-captcha.png) | ![Notification](docs/screenshots/06-notification.png) |

Optional:

| Auth | Performance |
|---|---|
| ![Auth](docs/screenshots/07-auth.png) | ![Performance](docs/screenshots/08-performance.png) |

## Features

### Schedule Browsing

- Browse university timetable data from a dedicated Android UI instead of raw web pages.
- Supports both student and teacher schedule flows.
- Includes week selection, day selection, and teacher search/selection flows.
- Supports a student schedule-only path without full registration.

### Account, Session, and Academic Data

- Student login flow with prepared session state and captcha handling.
- Student registration flow for faculty, department, course, group, and subgroup selection.
- Teacher registration/search flow backed by saved teacher profile data.
- Student performance screen with semester switching and cached academic results.
- Cached profile data and cached student photo support are present in the data layer.

### Offline and Reliability

- Room-backed cache for schedule weeks and performance data.
- Offline-friendly behavior when previously cached data exists.
- Loading, retry, error, and offline states are explicitly modeled in the UI state/view models.
- DataStore-backed app preferences for role, registration state, cache settings, notification settings, and saved student credentials.

### OCR and Captcha Assistance

- Google ML Kit text recognition is used to prefill captcha input during student login.
- Captcha preprocessing is implemented to improve OCR readability before recognition.
- OCR is assistive, not guaranteed; users can still correct the captcha value manually in the login flow.

### Local Lesson Notifications

- Local lesson reminders are scheduled from cached timetable data.
- Notification planning handles first-lesson reminders, next-lesson reminders, and end-of-day events.
- Notification scheduling is restored after device reboot through a `BroadcastReceiver`.
- Android notification permission and exact alarm permission are declared in the manifest.

### App Experience

- Jetpack Compose UI with Material 3 and Navigation Compose.
- Dedicated screens/features for auth, home, performance, settings, about, menu, and navigation.
- Custom design system/theme packages and animation helpers are included in the codebase.

## Tech Stack

| Area | Technologies |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3, Navigation Compose |
| State & async | ViewModel, Lifecycle, Coroutines, Flow |
| Local data | Room, DataStore Preferences, file-based photo cache |
| Network & parsing | OkHttp, Jsoup |
| OCR | Google ML Kit Text Recognition |
| Notifications | AlarmManager, BroadcastReceiver, notification channels |
| Build | Gradle Kotlin DSL, KSP |

## Architecture

The project follows a layered Android structure: feature/UI packages on top, stateful `ViewModel` logic in feature hosts, domain models/contracts/use cases in the middle, and data/repository implementations underneath. Local persistence, network parsing, session state, OCR, and notification scheduling are split into dedicated packages instead of being mixed directly into screens.

The notification pipeline is also separated cleanly: cached schedule data feeds a planner, the planner produces the next event, the scheduler places an alarm, and receivers restore or display notifications when needed.

```text
app/src/main/java/com/example/scheduleiseu/
├── app/
├── core/
│   ├── designsystem/
│   └── ui/
├── data/
│   ├── local/
│   │   ├── cache/
│   │   ├── db/
│   │   └── preferences/
│   ├── mapper/
│   ├── network/
│   ├── ocr/
│   ├── remote/
│   │   ├── cookie/
│   │   ├── datasource/
│   │   ├── model/
│   │   └── parser/
│   ├── repository/
│   └── session/
├── domain/
│   ├── core/
│   └── model/
├── feature/
│   ├── about/
│   ├── auth/
│   ├── common/
│   ├── home/
│   ├── menu/
│   ├── navigation/
│   ├── performance/
│   ├── settings/
│   └── whatsnew/
├── notification/
├── MainActivity.kt
```

## Offline Mode

The app caches schedule weeks in Room and also stores performance data locally. Student and teacher schedule view models observe cached weeks and can surface previously loaded timetable data when the network is unavailable. For student flows, cache policy is configurable through settings, while teacher schedule caching is also supported in the repository layer. DataStore is used for preferences, registration data, role/session flags, and notification/cache settings, which lets the app restore user context across launches.

Offline mode matters here because schedule lookup is a repeated daily task. When a user loses connectivity, the app can still render cached timetable content and communicate that the UI is currently working from saved data instead of pretending everything is live.

## OCR / Captcha Flow

The student login flow prepares a login session, downloads the captcha image, runs ML Kit text recognition, and prefills the recognized value when possible. The implementation also performs a simple bitmap preprocessing pass to improve contrast before retrying OCR.

This should be treated as input assistance, not as guaranteed automation. The recognized result can be wrong, so the user may still need to correct the captcha manually before sign-in.

## Notifications

The app schedules local lesson notifications from cached timetable data using `AlarmManager`. The planner currently supports reminders before the first lesson of the day, reminders for the next lesson when a previous lesson ends, and a day-finished event. Notification channels are created in app code, and the notification receiver reschedules the next reminder after showing the current one.

The manifest declares `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, and `SCHEDULE_EXACT_ALARM`. A boot receiver restores scheduling after device reboot, and the scheduler falls back to non-exact windows if exact alarms are unavailable on the device.

## What I Built Personally

- Designed the app package structure around features, domain contracts, repositories, local storage, remote parsing, and notification scheduling.
- Built the Android UI with Jetpack Compose, Navigation Compose, custom theme components, and screen-specific state handling.
- Implemented student and teacher schedule flows, including registration/context selection and week-based browsing.
- Added local persistence for schedule and performance data, plus DataStore-backed app preferences.
- Implemented OCR-assisted captcha recognition with Google ML Kit for the student login flow.
- Added local lesson notifications with alarm scheduling and reboot recovery.
- Packaged the repository so it can be evaluated more clearly as a proof-of-work portfolio project.

## Roadmap

- Introduce Hilt or Koin for dependency injection.
- Split large screens and view models into smaller focused units.
- Add unit tests for parsers, repositories, and notification planning.
- Add UI tests or screenshot tests for key screens.
- Improve visual polish and accessibility.
- Add CI build checks with GitHub Actions after local build verification is stable.
- Add more screenshots or a short GIF demo.
- Prepare a demo APK/release if sharing a build is safe.

## How to Run

```bash
git clone https://github.com/YARIGAVGAN/IseuSchedule_V2.git
cd IseuSchedule_V2
```

Then:

- Open the project in Android Studio.
- Sync Gradle.
- Run on an emulator or a physical Android device.
- Current Gradle config: `minSdk 26`, `targetSdk 35`, `compileSdk 35`.

Build commands:

Windows:

```bash
gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

## Disclaimer

- This is an unofficial educational and portfolio project.
- It is not affiliated with the educational institution.
- Do not commit real credentials, tokens, passwords, or private user data.
- Screenshots and demo materials should never expose personal data.
