# TapOnce Remote

A clean, completely free universal TV remote — no ads, no purchases, no subscription.

TapOnce Remote is an independent alternative to [Universal TV Remote for All TV](https://play.google.com/store/apps/details?id=com.boost.universal.remote) (package `com.boost.universal.remote`) — not affiliated with, endorsed by, or a modification of it. See the [market research report](https://appy.fyi/report/com.boost.universal.remote) this app's feature set was built from.

**Is the incumbent open source?** No public repository was found for `com.boost.universal.remote` or its developer after a web search — the "Universal TV Remote for All TV" name is used by several unrelated small studios on Google Play (this specific package wasn't among the ones with identifiable developer pages), and apps in this ad-monetized generic-remote category are, as far as could be confirmed, closed source. This couldn't be verified with certainty from a public source listing, but no evidence of an open-source release was found.

## What it does

- **WiFi TV discovery and saved devices** — finds nearby Roku (ECP) and Google Cast devices, and remembers previously connected TVs for quick reconnection.
- **Free core remote controls** — power, volume, mute, channel, navigation, and supported playback controls work with no purchase or ad gate.
- **Touchpad and text input** — swipe-to-navigate gesture pad and on-screen keyboard for protocols that support directional or text commands.
- **Basic IR fallback** — power/volume/mute/channel over the phone's infrared emitter when WiFi control isn't available.
- **Completely free** — no ads, no in-app purchases, no subscription of any kind.

## Requirements

- `min_sdk` 23 (Android 6.0+), `target_sdk` 35.

## Building

```
./gradlew clean
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
keytool -genkeypair -v -keystore release-upload.jks -storepass changeit -keypass changeit -alias upload -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=TapOnce Remote,O=AppyFyi,C=US" || true
ANDROID_KEYSTORE_PATH=$PWD/release-upload.jks ANDROID_KEYSTORE_PASSWORD=changeit ANDROID_KEY_ALIAS=upload ANDROID_KEY_PASSWORD=changeit ./gradlew bundleRelease
```

A `Taskfile.yml` wraps all of this (and every appy.fyi API call this project uses) as short `task` commands — see the project's build report for the full list.

## Status

Still open before this app can ship to real users:

- **Trademark clearance** — the working name/branding hasn't been legally cleared.
- **Privacy claim verification** — the drafted privacy policy needs a human check against what the app actually collects.
- **Closed testing recruitment** — Play Console's closed/internal testing track needs real testers before wider release.

The keystore passwords in the build command above are placeholders (`changeit`) — replace them with real secrets before producing a release build that ships.
