# Time Clicker

A small Android app that tracks *when you last did something*.
Each tile ("Watered the plants") shows how long ago it was last done, as its two largest units
("37 seconds", "2 minutes 45 seconds", "3 days 5 hours", "1 year 12 days"), ticking live.
Tap a tile when you do it again and it resets to now: the time disappears, then fades back in after a second.
The number in the tile's bottom-right corner counts how many times it was pressed.

- **Tap** a tile: mark it as done now. For the next 10 seconds the tile offers to undo it
  ("Tap again to undo", with a ring counting down in its corner): tapping it again undoes the press.
  Widgets do the same (before Android 12 their ring steps once a second), and a press on one can be undone on the other.
- **Long-press** a tile: edit it in a sheet: name, group, colour or photo background, icon (or none; your icon palette, plus a searchable list of every icon with the recently used ones first),
  size (Small / Wide / Tall), undo its last press or reset its press counter, a reminder, add it to the home screen, or delete it.
  All sizes show the same content.
- **Reminders**: a tile can notify you when it hasn't been done for a while (N hours, days or weeks since
  its last tap). It notifies once; tapping the tile (or "Mark as done" on the notification) starts the wait over.
  Tiles with a reminder show a bell in their top-right corner.
- **+** button (bottom centre): add a new tile, pre-filed in the group currently shown.
- **Group chips**, or **swiping left/right**, move between groups; "All" shows the tiles without a group,
  then one section per group.
  The sliders button opens **Groups**: add, rename, delete, drag to reorder.
- **Widgets**: any tile can live on the home screen (from the widget picker, or "Add to home screen"
  in its edit sheet). Tapping the widget marks it as done (with the click sound); its icon opens the app.
- **Settings** (gear button, next to the title): theme (system / light / dark), system colours, time shown as
  "3 days ago" or as a date and time, vibration, click sound, press counter, **icon palette**, groups, language,
  **export / import** of everything (tiles, groups, history, photos, settings) as one `.zip` file,
  and a **Credits** page for the open-source projects the app uses, with their licences.
- **Icon palette** (Settings): every [Lucide](https://lucide.dev) icon (about 1,900), by category, with search
  (in English, and in French when the app is in French: "poubelle", "anniversaire", "lave-linge").
  Tap icons to add them to, or remove them from, the icons offered when editing a tile.

The look is a neumorphic "bento" grid: soft pastel tiles raised off the page that sink in when pressed,
and glide to their new place when the grid changes. Screens slide in and out, and switching the theme
fades the colours instead of flashing. Light and dark themes follow the system unless
picked in the settings. On Android 12+ the app can follow the system's **Material You** colours:
surfaces and accents, with the pastel tile colours nudged toward their hue to match.

On **tablets**, in landscape and in split screen, the grid gets as many columns as fit (two on a phone,
four on a tablet held upright, seven across a landscape tablet), so tiles keep their phone size.
Settings, Groups, Credits and the icon palette stay a readable width, centred.

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
| Icons    | [Lucide](https://lucide.dev) (ISC), bundled as path data in `assets/lucide/icons.tsv` |
| Build    | Gradle (Kotlin DSL, version catalog), AGP 9 |
| Min / target SDK | 26 / 37 |

## Project layout

```
app/src/main/java/dev/cfaz/timeclicker/
├── TimeClickerApplication.kt  # creates the AppContainer
├── AppContainer.kt            # manual dependency injection
├── MainActivity.kt            # Home <-> Groups screen switch
├── data/
│   ├── Tracker.kt             # domain model: tiles, groups, colour/size enums, icon (a Lucide name)
│   ├── Reminder.kt            # a tile's reminder: after how long without being done
│   ├── IconCatalog.kt         # the bundled Lucide icons: path data, categories, search tags
│   ├── TrackerRepository.kt   # the single entry point the UI uses
│   ├── PhotoStore.kt          # copies picked photos into private storage
│   └── local/                 # Room: entities, DAO, database + migrations
└── ui/
    ├── components/            # neumorphic buttons, text field, segmented control, tablet gutters
    ├── home/                  # home screen (group pager), bento grid, tile, edit sheet, ViewModel
    ├── groups/                # manage groups screen
    ├── icons/                 # icon palette screen, full icon chooser, icon and category names
    ├── settings/              # settings screen, credits
    ├── theme/                 # palette (+ Material You), neumorphic shadow modifiers, icons
    └── time/                  # "2 minutes 45 seconds" formatting + ticking clock
├── reminder/                  # reminder notifications: one alarm for the next due tile, "Mark as done"
└── widget/                    # home-screen widgets (Glance): widget, tile picker, refresh alarm
app/src/main/assets/lucide/    # icons.tsv (generated) and the Lucide licence
app/src/main/assets/licenses/  # licence texts shown in the credits
app/schemas/                   # exported Room schemas (commit these, they back migrations)
tools/lucide/generate_icons.py # regenerates icons.tsv from the latest Lucide release
tools/store/                   # captures and frames the Play Store screenshots (see its README)
fastlane/metadata/android/     # the Play Store listing: text and graphics, in every language
```

### Icons

Tile icons are [Lucide](https://lucide.dev) icons, stored by name (`circle-check`). Earlier versions stored their
own names (`check`, `paw`…); database migration 3 → 4 and the import of format-1 backups rename them. `python tools/lucide/generate_icons.py`
downloads the latest Lucide release and its categories and rewrites `assets/lucide/icons.tsv`, each icon
flattened to one path for both Compose and the widgets. Before committing a regenerated file, check its diff
for removed icons: a tile using one would lose its icon (the unit tests check the default ones).

Lucide's search tags are only in English. `assets/lucide/tags-<language>.tsv` adds search words in another language
(one icon per line: name, tab, comma-separated words), used when the app is in that language; `tags-fr.tsv` covers
every icon. They are written by hand, not generated: the script lists the icons a file is missing after an update,
and the unit tests fail on lines for icons Lucide no longer has. Another language only needs its own file.

### Data model

- `tracker_groups`: user-defined groups (name, display position).
- `trackers`: one row per tile (name, group, colour, icon, size, photo file, display position,
  `count_since`: the press counter counts events after this time, and the reminder: `reminder_every` +
  `reminder_unit`, null for none).
  Deleting a group sets its tiles' group to null (they then only show under "All").
- `tracker_events`: one row per time a tile was done.

A card's "last time" is its most recent event. Resetting a card inserts an event, and tapping it again to undo deletes it.
Because the full history is kept, future features (history view, stats, average interval)
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

The debug build installs as a separate app (`dev.cfaz.timeclicker.debug`), so it can live next to the release build.

## CI: building and releasing on GitHub

`.github/workflows/build-apk.yml` runs on every push to `main`, on every pull request, and on manual dispatch:

- runs the unit tests and builds a minified release APK and app bundle
- uploads them as workflow artifacts (`TimeClicker-<version>.apk`, and `TimeClicker-<version>.aab` with its R8 mapping)
- on a tag push `vX.Y.Z`, also creates a GitHub Release with the APK attached, and uploads the app bundle and the
  store listing to Google Play (see below):

  ```bash
  git tag v1.0.0 && git push origin v1.0.0
  ```

`versionCode` is the workflow run number, so each new APK installs as an update over the previous one,
and each bundle is newer than the last one on Play.

### Publishing to Google Play

The `google-play` job uploads the bundle with [fastlane supply](https://docs.fastlane.tools/actions/supply/), along with
the store listing as committed in `fastlane/metadata/android` (text, screenshots, feature graphic, release notes from
`changelogs/default.txt`); it doesn't regenerate it. Images already on Play are only sent again when they change.
It runs on version tags, or from **Actions → Build and release → Run workflow** with a track picked.

Setup, once:

1. In the Play Console, create the app (`dev.cfaz.timeclicker`) and upload the **first** bundle by hand
   (a `TimeClicker-<version>-bundle` workflow artifact): Google's API can't make an app's first upload.
2. In Google Cloud, create a service account with a JSON key; in the Play Console (**Users and permissions**), invite its
   email with release permissions for this app.
3. Add the key's JSON as the `PLAY_SERVICE_ACCOUNT_JSON` repository secret. The release signing secrets below must be
   set too: Play only accepts bundles signed with your upload key.
4. Optional repository variables: `PLAY_TRACK` (`internal` by default; or `alpha`, `beta`, `production`) and
   `PLAY_RELEASE_STATUS` (`draft` by default, which you then roll out in the Console; Play refuses anything else until
   the app's first release is published, then `completed` releases straight away).

The job uses the `google-play` environment: add required reviewers to it (**Settings → Environments**) to approve each
upload. With Play App Signing, installs from Play are signed by Google's key, so they don't update from (or to) the
GitHub APK: uninstall to switch.

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
