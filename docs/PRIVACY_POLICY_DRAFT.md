# Steady Gallery Privacy Policy (DRAFT -- not yet verified for accuracy)

**This is a draft only.** `legal.privacy_policy_accurate` is `false` in the build spec on
purpose: a human must verify every claim below against the shipped app before it is published
at the real URL, `https://www.appyfyi.com/privacy/steady-gallery`.

## Data collected

Steady Gallery does not collect any data. It has no analytics SDK, no crash reporting SDK, no
remote config, no account system, and no server backend. The app manifest does not request the
`INTERNET` permission.

## What the app can access on your device

- **Photos and videos** (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`): used only to display your
  existing folders, media, and thumbnails, and to let you crop/rotate/filter and export edited
  copies back into your device's Photos/Videos library. Nothing leaves the device.
- **Biometric unlock** (`USE_BIOMETRIC`): used only to unlock the app's local hidden-folders
  feature using your device's existing fingerprint/face enrollment. The app never receives your
  biometric data itself -- only a success/failure signal from the Android system.

## Local-only storage

- Folder organization, sort order, and app preferences are stored in a local database on your
  device (Room/SQLite) and are never transmitted anywhere.
- A PIN used to protect hidden folders is never stored in plain text: only a salted PBKDF2 hash
  of it is kept, in Android's `EncryptedSharedPreferences`.
- Purchase status for the one-time Pro unlock is cached locally (also in
  `EncryptedSharedPreferences`) after Google Play Billing confirms the purchase. Steady Gallery
  does not run its own purchase-validation server.
- Items you move to the Recycle Bin are copied to an app-private folder on your device until you
  restore or permanently delete them. This copy never leaves the device either.

## Third parties

Google Play Billing is used solely to process the one-time in-app purchase. Google's own privacy
policy governs that transaction; Steady Gallery does not receive or store your payment details.

## Contact

_A human must add a real contact address/method here before publishing._

## Regulated data category

`legal.regulated_category` in the build spec is `none`. A human should confirm this still holds
for the shipped feature set (in particular biometric unlock) before submitting the Play Data
Safety form.
