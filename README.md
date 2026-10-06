# Time Clicker

A small Android app that tracks *when you last did something*.
Each tile ("Watered the plants") shows how long ago it was last done, as its two largest units
("37 seconds", "2 minutes 45 seconds", "3 days 5 hours", "1 year 12 days"), ticking live.
Tap a tile when you do it again and it resets to now: the time disappears, then fades back in after a second.
The number in the tile's bottom-right corner counts how many times it was pressed.

- **Tap** a tile: mark it as done now. A snackbar offers **Undo**.
- **Long-press** a tile: edit it in a sheet: name, group, colour or photo background, icon (or none),
  size (Small / Wide / Tall), reset its press counter, add it to the home screen, or delete it.
  All sizes show the same content.
- **+** button (bottom centre): add a new tile, pre-filed in the group currently shown.
- **Group chips**, or **swiping left/right**, move between groups; "All" shows one section per group.
  The sliders button opens **Groups**: add, rename, delete, drag to reorder.
- **Widgets**: any tile can live on the home screen (from the widget picker, or "Add to home screen"
  in its edit sheet). Tapping the widget marks it as done; its icon opens the app.
- **Settings** (gear button, next to the title): theme (system / light / dark), wallpaper colours, time shown as
  "3 days ago" or as a date and time, vibration, click sound, press counter, the Undo message, groups, language,
  and **export / import** of everything (tiles, groups, history, photos, settings) as one `.zip` file.

The look is a neumorphic "bento" grid: soft pastel tiles raised off the page that sink in when pressed,
and glide to their new place when the grid changes. Light and dark themes follow the system unless
picked in the settings. On Android 12+ the app can follow the wallpaper's **Material You** colours:
surfaces and accents, with the pastel tile colours nudged toward the wallpaper's hue to match.

Translated into English, French, Spanish, German, Italian, Portuguese, Dutch, Polish, Russian, Turkish,
Indonesian, Arabic, Hindi, Japanese, Korean and Simplified Chinese. On Android 13+ the language can be
picked per app in the system settings.

Fully offline. All data lives in a local SQLite database in the app's private storage;
tile photos are copied into the app's private storage too.

## Tech stack

| Concern  | Choice |
|----------|--------|
| Language | Kotlin |
| UI       | Jetpack Compose + Material 3, custom neumorphic palette (light & dark, Material You on Android 12+) |
| Widgets  | Jetpack Glance |
| Storage  | Room (SQLite) |
| Images   | Coil (local files only), Android photo picker (no permission needed) |
| Build    | Gradle (Kotlin DSL, version catalog), AGP 9 |
| Min / target SDK | 26 / 37 |

## Project layout

```
app/src/main/java/com/keeptrack/timeclicker/
├── TimeClickerApplication.kt  # creates the AppContainer
├── AppContainer.kt            # manual dependency injection
├── MainActivity.kt            # Home <-> Groups screen switch
├── data/
│   ├── Tracker.kt             # domain model: tiles, groups, colour/size/icon enums
│   ├── TrackerRepository.kt   # the single entry point the UI uses
│   ├── PhotoStore.kt          # copies picked photos into private storage
│   └── local/                 # Room: entities, DAO, database + migrations
└── ui/
    ├── components/            # neumorphic buttons, text field, segmented control
    ├── home/                  # home screen (group pager), bento grid, tile, edit sheet, ViewModel
    ├── groups/                # manage groups screen
    ├── theme/                 # palette (+ Material You), neumorphic shadow modifiers, icons
    └── time/                  # "2 minutes 45 seconds" formatting + ticking clock
└── widget/                    # home-screen widgets (Glance): widget, tile picker, refresh alarm
app/schemas/                   # exported Room schemas (commit these, they back migrations)
```

### Data model

- `tracker_groups`: user-defined groups (name, display position).
- `trackers`: one row per tile (name, group, colour, icon, size, photo file, display position,
  and `count_since`: the press counter counts events after this time).
  Deleting a group sets its tiles' group to null ("Other").
- `tracker_events`: one row per time a tile was done.

A card's "last time" is its most recent event. Resetting a card inserts an event, and Undo deletes it.
Because the full history is kept, future features (history view, stats, average interval, reminders)
can use data that is already there.

### Extending the database

1. Change the entities and bump `version` in `AppDatabase`.
2. Build. Room exports the new schema to `app/schemas/`.
3. Add a migration (see the `AutoMigration`s in `AppDatabase`) so existing user data is kept.
4. Extend `MigrationTest` and run it on a device or emulator: `./gradlew connectedDebugAndroidTest`.

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
- uploads it as a workflow artifact (`TimeClicker-<version>.apk`)
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
keytool -genkeypair -v -keystore release.jks -alias timeclicker \
  -keyalg RSA -keysize 4096 -validity 10000
```

Then add these repository secrets (Settings → Secrets and variables → Actions, or with the `gh` CLI):

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` (on macOS: `base64 -i release.jks`) |
| `KEYSTORE_PASSWORD` | the keystore password |
| `KEY_ALIAS` | `timeclicker` (whatever alias you used) |
| `KEY_PASSWORD` | the key password (optional: defaults to the keystore password, which is what `keytool` uses by default) |

Back up `release.jks` and its passwords outside the repo. If you lose them, you can't ship updates to an existing install.
`*.jks` is git-ignored.
