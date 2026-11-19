# RecallFlow

An offline-first spaced-repetition study tracker for Android. Add what you want to
remember, review it when RecallFlow says it's due, and let the scheduler decide when
to show it again. No account, no cloud, no ads, no INTERNET permission.

**Status: v2.3 — published.** Data layer, all screens, review flow, calendar,
stats, notifications, settings and JSON backup are implemented. The APK is compiled
and signature-verified; it has not yet been device-tested.

## Download

Latest APK: <https://github.com/nahidsarker771/recall-flow/releases/latest>

<p align="center">
  <a href="https://github.com/nahidsarker771/recall-flow/releases/latest">
    <img src="recallflow-release-qr.png" alt="Scan to download RecallFlow" width="220">
  </a>
</p>

Requires Android 8.0 (API 26) or newer. The app is fully offline and asks for no
network access.

## Requirements

- JDK 17 (`/usr/lib/jvm/java-17-openjdk`)
- Android SDK platform 34 + build-tools 34.0.0 (`~/Android/Sdk`)
- Linux/macOS shell; Gradle wrapper is included (Gradle 8.7)

## Build

```bash
cd "project files/studyretention"
./build-apk.sh            # builds current VERSION, bumps it afterwards
./build-apk.sh 2.0        # explicit version, no bump past it
```

The signed APK is written to `project files/apk/RecallFlow-v<version>.apk`.

Or use Gradle directly:

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew assembleRelease              # signed release APK (needs keystore.properties)
./gradlew testReleaseUnitTest          # scheduler unit tests (11)
./gradlew assembleDebug                # debug APK for quick installs
```

The package name is `com.studyretention.tracker` (kept from the app RecallFlow
replaces), so this installs as an update over the old build and keeps the signing key.

## Features

### Study loop
- Add topics with optional notes and a category; first review ≈ 26 hours later.
- Today shows the next topic in a hero card, a completion ring, the current streak,
  and every due topic with swipe-right to review / swipe-left to snooze one hour.
- Review screen: live stopwatch, notes/prompt card, 0–5 recall rating with plain-language
  hints, a preview of the next interval, and save.
- Topic detail: mastery bar, interval/ease/repetitions/time studied, full history,
  manual interval override, edit and delete.

### Scheduling
- **Guided (default):** 1 → 7 → 16 → 35 days for the first four successes, then
  `interval × ease factor`.
- **Classic SM-2:** 1 day, 6 days, then `interval × previous ease factor`.
- Ratings below 3 reset the topic to 1 day; ease factor starts at 2.5, floors at 1.3,
  and follows `EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))`.
- Mastery is `interval × 100 / 90 + repetitions × 5`, capped 0–100.
- Optional daily review limit caps how many due topics Today shows (0 = no limit).

### Calendar
- Swipeable month pager built like Apple/Google Calendar, with today marked, dots for
  scheduled reviews, and a Today shortcut.
- Agenda for the selected day, add-review dialog (topic + time picker), and a tap-to-move
  sheet: in 1 hour, tomorrow, 3 days, next week, or remove.
- Auto-schedule places the next review on the calendar whenever a review is saved or a
  topic is created; moving a pending event also moves the topic's next review.

### Stats
- Total reviews, average retention, time studied, reviews today, current/longest streak.
- 30-day retention line, this-week review bars and a 0–5 rating distribution, drawn
  with Compose `Canvas` (no chart dependency, fully offline).

### Reminders
- One notification per topic per day, delivered at the user's chosen time of day.
- Exact alarms when permitted (Android 12+ "Alarms & reminders"), inexact fallback
  otherwise; 15-minute WorkManager safety net; rescheduled after boot.
- Tapping a notification deep-links straight into that topic's review.

### Settings
- Scheduling algorithm, daily limit, notification toggle, reminder time, exact-alarm
  shortcut, system/light/dark theme, dynamic color (Android 12+).
- JSON backup export/import through the system file picker (replaces local data).
- How it works (reopens the tutorial) and version info.

## First launch

A four-page walkthrough explains spaced repetition, adding topics, rating recall and
the progress views, then never appears again. Skipping also counts as seen. Reopen it
any time from **Settings → How it works**.

## Design system

- Material 3 (Material You) with dynamic color on Android 12+, fixed indigo
  `#4F46E5` brand scheme elsewhere.
- Light and dark themes; 24dp hero corners, soft tonal cards, one accent color,
  4/8/16/24dp spacing tokens.
- Material Symbols icons (outlined for inactive tabs, filled for active).
- System font only — no downloads, fully offline.

## Architecture

```
app/src/main/java/com/studyretention/tracker/
├── MainActivity.kt                     theme, edge-to-edge, notification permission, deep link
├── RecallFlowApplication.kt            container, channel, worker, alarm sync
├── data/                               Room entities/DAOs/db, preferences, repository
│   ├── Entities.kt                     Topic, ReviewSession, ScheduledEvent
│   ├── StudyRepository.kt              transactions, reviews, events, JSON backup
│   └── AppPreferences.kt               settings StateFlow
├── logic/
│   ├── SpacedRepetitionScheduler.kt    pure Kotlin, tested
│   └── StatsCalculator.kt              streaks, retention series, totals
├── notifications/                      channel, alarms, boot receiver, due worker
├── viewmodel/RecallFlowViewModel.kt    one shared VM (MVVM)
└── ui/
    ├── RecallFlowApp.kt                NavHost + bottom bar
    ├── navigation/Destinations.kt
    ├── components/                     headers, ring, bars, swipe row, dialogs
    ├── screens/{onboarding,today,topics,schedule,stats,review,settings}/
    └── theme/, util/
```

Room database `recallflow.db` is exposed as Flows through the repository and a single
shared ViewModel. The scheduler is dependency-free, so FSRS (or another strategy) can
replace it without touching UI or database code.

## Testing

```bash
cd "project files/studyretention"
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
./gradlew testReleaseUnitTest
```

Covered: 26-hour first review, ease-factor math and floor, guided ladder, ladder growth,
classic SM-2, failed-review reset, mastery clamping, interval override, preview parity.

## Privacy

No INTERNET permission, no analytics, no accounts, no tracking. Topics, reviews,
timers and calendar rows live in a local Room database; backups are written only to a
file you choose.

## Roadmap

- [x] Theme, shell, navigation, adaptive icon, onboarding
- [x] Room data layer + GUIDED/SM-2 scheduler + unit tests
- [x] Today, Topics/detail, Schedule calendar, Stats charts
- [x] Review flow, stopwatch, rating UI, history
- [x] WorkManager + alarms, notifications, auto-scheduling
- [x] Settings, JSON export/import
- [ ] Device testing and polish (next)
- [ ] Optional: R8 shrinking + branded splash before a public release
