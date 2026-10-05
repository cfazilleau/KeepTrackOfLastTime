# Last Time

A small Android app that tracks *when you last did something*.
Each card ("Watered the plants") shows how long ago it was last done ("3 days and 5 hours ago").
Tap a card when you do it again and it resets to now.

- **Tap** a card: mark it as done now. A snackbar offers **Undo**.
- **Long-press** a card: rename or delete it.
- **+** button: add a new card.

Fully offline. All data lives in a local SQLite database in the app's private storage.

## Tech stack

| Concern  | Choice |
|----------|--------|
| Language | Kotlin |
| UI       | Jetpack Compose + Material 3 (dynamic colors on Android 12+) |
| Storage  | Room (SQLite) |
| Build    | Gradle (Kotlin DSL, version catalog), AGP 9 |
| Min / target SDK | 26 / 37 |

## Project layout

```
app/src/main/java/com/keeptrack/lasttime/
├── LastTimeApplication.kt     # creates the AppContainer
├── AppContainer.kt            # manual dependency injection
├── MainActivity.kt
├── data/
│   ├── Tracker.kt             # domain model
│   ├── TrackerRepository.kt   # the single entry point the UI uses
│   └── local/                 # Room: entities, DAO, database
└── ui/
    ├── home/                  # home screen, cards, dialogs, ViewModel
    ├── theme/
    └── time/                  # "3 days and 5 hours ago" formatting + ticking clock
app/schemas/                   # exported Room schemas (commit these, they back migrations)
```

### Data model

- `trackers`: one row per card (name, creation time, display position).
- `tracker_events`: one row per time a card was done.

A card's "last time" is its most recent event. Resetting a card inserts an event, and Undo deletes it.
Because the full history is kept, future features (history view, stats, average interval, reminders)
can use data that is already there.

### Extending the database

1. Change the entities and bump `version` in `AppDatabase`.
2. Build. Room exports the new schema to `app/schemas/`.
3. Add a migration, for example `autoMigrations = [AutoMigration(from = 1, to = 2)]`, so existing user data is kept.

## Building locally

Requires JDK 17+ and the Android SDK (Android Studio sets this up).

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
```

The debug build installs as a separate app (`com.keeptrack.lasttime.debug`), so it can live next to the release build.

## CI: building the APK on GitHub

`.github/workflows/build-apk.yml` runs on every push to `main`, on every pull request, and on manual dispatch:

- runs the unit tests and builds a minified release APK
- uploads it as a workflow artifact (`LastTime-<version>.apk`)
- on a tag push `vX.Y.Z`, also creates a GitHub Release with the APK attached:

  ```bash
  git tag v1.0.0 && git push origin v1.0.0
  ```

`versionCode` is the workflow run number, so each new APK installs as an update over the previous one.

### Release signing (do this once)

Android only installs an update over an existing app if both are signed with the **same key**.
Without these secrets, CI signs with a throwaway key. Each build then needs an uninstall first,
which **erases your data**. Set up a permanent key:

```bash
keytool -genkeypair -v -keystore release.jks -alias lasttime \
  -keyalg RSA -keysize 4096 -validity 10000
```

Then add these repository secrets (Settings → Secrets and variables → Actions, or with the `gh` CLI):

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` (on macOS: `base64 -i release.jks`) |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `lasttime` |
| `KEY_PASSWORD` | the key password |

Back up `release.jks` and its passwords outside the repo. If you lose them, you can't ship updates to an existing install.
`*.jks` is git-ignored.
