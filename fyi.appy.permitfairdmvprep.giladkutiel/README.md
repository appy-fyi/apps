# PermitFair DMV Prep

A DMV permit prep app that lets a learner finish a complete practice test before seeing any
paywall, and offers a single affordable lifetime unlock instead of a weekly subscription.

**PermitFair DMV Prep is an independent alternative to [Zutobi: Permit & Driving Prep](https://play.google.com/store/apps/details?id=com.driverlicenseapp) — not affiliated with, endorsed by, or a modification of it.** It was built from market research in [appy.fyi's report on Zutobi](https://appy.fyi/report/com.driverlicenseapp).

Zutobi is closed-source: a commercial product from Zutobi AB / Zutobi Inc., with no public
repository found for it.

## Why it's different

- One complete free practice test before any paywall or unlock prompt
- One optional $4.99 lifetime unlock — no weekly subscription, no free trial
- No account required, no cloud sync, no personal data collected
- Lessons and quiz questions based on public DMV handbook content for the launch states

## What it does

- **State Selection** — pick a bundled launch-state content pack (California, Texas at launch).
- **Home** — dashboard of progress, lesson list, and the entry point into a full practice test.
- **Lesson** — handbook-based study material with a completion checkmark and a per-lesson quiz.
- **Quiz** — one question at a time, with answers saved immediately so process death never
  loses progress.
- **Quiz Results** — score, full answer review (selected answer, correct answer, explanation,
  handbook section) for every question, and the first unlock prompt after a completed free
  practice test.
- **Unlock** — the one-time $4.99 lifetime unlock via Google Play Billing, plus Restore purchase.
- **Settings** — change state, purchase status, pricing disclosure, and privacy note.

## Requirements

- `minSdk` 26 (Android 8.0+)
- `targetSdk` 35

## Building

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
keytool -genkeypair -v -keystore release.keystore -storepass permitfair-release-pass -keypass permitfair-release-pass -alias permitfair -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=PermitFair, OU=SoloBuilder, O=PermitFair, L=Internet, S=NA, C=US"
ORG_GRADLE_PROJECT_RELEASE_STORE_FILE=$PWD/release.keystore ORG_GRADLE_PROJECT_RELEASE_STORE_PASSWORD=permitfair-release-pass ORG_GRADLE_PROJECT_RELEASE_KEY_ALIAS=permitfair ORG_GRADLE_PROJECT_RELEASE_KEY_PASSWORD=permitfair-release-pass ./gradlew bundleRelease
```

Or via the bundled `Taskfile.yml`: `task build`, `task test`, `task test-instrumented`,
`task install`, `task release`, `task clean`, and every `task api-*` command.

The sample keystore password above is a placeholder for local builds only — replace it with a
real secret before any actual release.

## Status — open human gates

This build has not yet cleared the following, per its build spec's `human_gates_required`:

- **Trademark and privacy review** — `working_name`/`package_id`/store listing have not been
  legally cleared, and the drafted privacy policy's claims have not been verified against the
  actual data practices of a shipped build.
- **Closed testing recruitment** — no closed/internal testers have been recruited on Google Play
  yet.

No claim of trademark clearance or privacy-claim accuracy is made anywhere in this repository.
