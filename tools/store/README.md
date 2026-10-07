# Store listing

The Google Play listing lives in [`fastlane/metadata/android/`](../../fastlane/metadata/android), in the layout
[fastlane supply](https://docs.fastlane.tools/actions/supply/) uploads. CI sends it with each release
(main README, Publishing to Google Play); it can also be copied by hand into the Play Console. One folder per store
language, the same 16 as the app:

| File | Limit | |
|------|-------|-|
| `title.txt` | 30 | `Time Clicker` in every language, like the launcher name |
| `short_description.txt` | 80 | |
| `full_description.txt` | 4,000 | |
| `changelogs/default.txt` | 500 | release notes; replace before each release |
| `images/phoneScreenshots/1-8.jpg` | 2-8 | 1080×1920, captioned |
| `images/tenInchScreenshots/1-3.jpg` | 0-8 | 2560×1440, captioned (also fine as 7-inch screenshots) |
| `images/featureGraphic.png` | | 1024×500 |
| `en-US/images/icon.png` | | 512×512, the default for every language |

`python tools/store/check.py` checks the limits and image sizes.

## Regenerating the graphics

The screenshots show demo tiles ([`demo.py`](demo.py)) in each language, captured from the release build on an
emulator, then framed with a caption ([`captions.json`](captions.json)). Needs Python 3 with Pillow, and Edge or Chrome.

1. An emulator with a **Google APIs** image (rootable; not "Google Play"). Phone screenshots need a 1080×1920 screen
   (Play rejects screenshots longer than 2:1), e.g. the `pixel_2` device; tablet ones a Pixel Tablet in landscape.
2. Build, install, and once per emulator put four widgets on the launcher's first page for the widget screenshot:
   in the app (with the demo tiles: run a capture first), long-press "Watered the plants", "Fed the cat", "Called Grandma"
   and "Took vitamins" in turn, and tap **Add to home screen**. Then capture (the app's data on that emulator is replaced):

   ```bash
   ./gradlew assembleRelease
   adb -s emulator-5558 root
   adb -s emulator-5558 install -r app/build/outputs/apk/release/app-release.apk
   python tools/store/capture.py --serial emulator-5558                  # phone, every language
   python tools/store/capture.py --serial emulator-5558 --kind feature   # tiles for the feature graphic
   python tools/store/capture.py --serial emulator-5556 --kind tablet    # tablet, landscape
   python tools/store/compose.py
   python tools/store/check.py
   ```

   `--locales en-US fr-FR` limits a run to some languages. Raw captures go to `build/store/raw/`.

### Photos

Two demo tiles have a photo background ([`demo.py`](demo.py) `PHOTOS`):

- `plant.jpg`: "A Wet Leaf" by Zeddong, [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:A_Wet_Leaf.jpg),
  [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) (resized to 1280 px). The licence asks for this credit
  wherever the photo appears, the store screenshots included.
- `cat.jpg`: the developer's own cat.

## The rest of the Play Console

These aren't part of the uploaded metadata; they're filled in once in the console.

| Field | Answer |
|-------|--------|
| App or game, price | App, free |
| Category | Productivity |
| Contact email | *(yours: it's shown on the store page)* |
| Website | https://timeclicker.cfaz.dev |
| Privacy policy | https://timeclicker.cfaz.dev/privacy.html |
| Ads | No ads |
| App access | All functionality is available without special access (no account) |
| Content rating | Category "Utility, productivity, communication or other"; every question "No" (no user-to-user interaction, no sharing of location, no purchases) |
| Target audience | 13 and over (picking under 13 brings in the Families policy) |
| Data safety | No data collected, no data shared: the app has no internet permission. Data deletion: not applicable (nothing leaves the device; uninstalling deletes everything) |
| Health, financial, news, government | None |
| Permissions | `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` need no declaration |
