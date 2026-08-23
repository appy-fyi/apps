# Steady Gallery Privacy Policy

_Last updated: August 21, 2026_

## Data collected

Steady Gallery does not collect any data. It has no analytics SDK, no crash reporting SDK, no
remote config, no account system, and no server backend. The app manifest does not request the
`INTERNET` permission.

## What the app can access on your device

- **Photos and videos** (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`): used only to display your
  existing folders, media, and thumbnails, and to let you crop/rotate/filter and export edited
  copies back into your device's Photos/Videos library. Nothing leaves the device.
- **Biometric unlock** (`USE_BIOMETRIC`): used only to unlock the app's local Hidden Photos
  feature using your device's existing fingerprint/face enrollment. The app never receives your
  biometric data itself — only a success/failure signal from the Android system.

## Local-only storage

- Folder organization, sort order, and app preferences are stored in a local database on your
  device (Room/SQLite) and are never transmitted anywhere.
- **Hidden Photos**: when you hide individual photos or videos, the app copies them into an
  app-private folder on your device and removes them from your visible library. Hidden items stay
  in that app-private folder — never transmitted anywhere — until you unhide them or uninstall the
  app, and viewing them again requires unlocking with your PIN or biometrics. If you leave the
  Hidden Photos screen (or use the "hide all now" switch in Settings), it re-locks automatically.
- A PIN used to protect Hidden Photos is never stored in plain text: only a salted PBKDF2 hash of
  it is kept, in Android's `EncryptedSharedPreferences`.
- Purchase status for the one-time Pro unlock is cached locally (also in
  `EncryptedSharedPreferences`) after Google Play Billing confirms the purchase. Steady Gallery
  does not run its own purchase-validation server.
- Items you move to the Recycle Bin are copied to an app-private folder on your device until you
  restore or permanently delete them. This copy never leaves the device either.

## Third parties

Google Play Billing is used solely to process the one-time in-app purchase. Google's own privacy
policy governs that transaction; Steady Gallery does not receive or store your payment details.

## Contact

Questions about this policy or the app can be sent to gilad.kutiel@gmail.com.

## Regulated data category

Steady Gallery does not collect, transmit, or store biometric data — biometric unlock only reads a
local success/failure signal from the Android system, never raw biometric material. It does not
fall into a regulated data category for the Play Data Safety form.
