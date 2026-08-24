# InkSend

Turn any message you type into a styled image — pick a font, color, and background — and send
it to WhatsApp in one tap, right from the keyboard, with no forced ads and no subscription.

## Independent alternative — not affiliated with Fontmaker

InkSend is an independent alternative to
[Fontmaker - Font Keyboard App](https://play.google.com/store/apps/details?id=com.takeofflabs.fontmaker)
by Takeoff Labs. It is not affiliated with, endorsed by, or a modification of Fontmaker's own
code or branding — it's a separate app built from scratch, using
[appy.fyi's market report on Fontmaker](https://appy.fyi/report/com.takeofflabs.fontmaker) as
research into what the incumbent does well and where its own users are frustrated (chiefly: a
forced 90+ second ad every 3-5 letters while drawing a handwriting font, and subscription/trial
pricing confusion).

Fontmaker does not appear to be open source. Takeoff Labs is a commercial mobile app publisher
(reporting connects it to Vungle/JetFuel's ad-growth business), and no public source repository
for the app turned up after a real search — it's a closed-source, proprietary product.

## What it does

- **Style Gallery & Presets** — browse, create, and pick a default text style (font + text color
  + background), with 3 built-in presets usable for free.
- **Text-to-Image Renderer & One-Tap Send** — renders the currently typed text as a styled PNG
  and sends it to WhatsApp in a single tap, directly from the keyboard panel — no interstitial
  screen, no ad.
- **Custom Keyboard (IME)** — a real system-wide keyboard (QWERTY + the Style & Send action bar)
  usable in any app, not just InkSend itself.
- **Guided Keyboard Setup** — in-app, step-by-step onboarding that deep-links into system
  settings to enable and select the keyboard, confirmed with a live test-typing box.
- **One-Time Purchase, No Ads** — a single $4.99 non-consumable purchase unlocks custom colors
  and the handwriting font creator. No subscription, no trial, no ads anywhere.
- **Personal Handwriting Font** (secondary) — draw all 62 required characters (A-Z, a-z, 0-9) by
  hand, compiled entirely on-device into a real, installable TrueType font — no forced wait or ad
  between glyphs.

## Requirements

- `min_sdk`: 26 (Android 8.0)
- `target_sdk`: 35

## Building

```
chmod +x ./gradlew
./gradlew clean
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
keytool -genkeypair -v -keystore inksend-upload.jks -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 -alias upload -dname "CN=InkSend Upload,O=AppyFYI,C=US" -storepass changeit -keypass changeit
RELEASE_STORE_FILE="$PWD/inksend-upload.jks" RELEASE_STORE_PASSWORD="changeit" RELEASE_KEY_ALIAS="upload" RELEASE_KEY_PASSWORD="changeit" ./gradlew bundleRelease
```

Or, with [Task](https://taskfile.dev) installed at the shared apps root: `task build`,
`task test`, `task install`, `task release`. See `Taskfile.yml`.

## Status

Two items from this build remain open, both deliberately human-only gates:

- **Trademark & privacy review** — the app's name/branding hasn't been checked for trademark
  conflicts, and the privacy policy draft hasn't been human-verified for accuracy.
- **Closed testing recruitment** — Play Console's closed/internal testing track needs real
  testers recruited before this can move toward production.
