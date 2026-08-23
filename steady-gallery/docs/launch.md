# Steady Gallery — Launch Instructions

Update 2026-08-21: claimed `ownership` on `appy.fyi` for
(`com.simplemobiletools.gallery.pro` → `com.appyfyi.steadygridgallery`) and
published this app's `appy.fyi` app page at
**https://appy.fyi/app/com.appyfyi.steadygridgallery**, with its icon
(`store_listing/play_store_icon_512.png`) and all 5 screenshot slots
uploaded (real screenshots captured live off a running emulator build —
folder grid, a folder's media view, full-screen viewer, editor, and
settings).

The `privacy_policy` upload initially 500'd (a stray `app_id not null`
column on appy.fyi's live `privacy_policy` table, unrelated to this repo —
root-caused and fixed server-side by appy.fyi's own team). Retried after
their fix landed and it succeeded.

Update 2026-08-21 (later): `docs/PRIVACY_POLICY_DRAFT.md` has been finished
and re-published — real contact email added (gilad.kutiel@gmail.com),
"hidden folders" language corrected to "Hidden Photos" (matching the
todo.txt feature rename), and the regulated-data-category note confirmed
against `LockCredentialStore`/`SecureStorage` (biometric unlock only reads a
success/failure signal from Android's BiometricPrompt; no biometric data is
collected or stored, so `regulated_category: "none"` holds). It's now live
at **https://appy.fyi/app/com.appyfyi.steadygridgallery/privacy** (the
earlier `/legal/.../1` URL from the first, still-placeholder upload has been
superseded by this one). `legal.privacy_policy_url` and
`legal.privacy_policy_accurate` in the build spec have been updated to this
URL and `true` respectively — §3.1 below is done except for the trademark
half of that gate.

Note: `app/build.gradle.kts`'s `applicationId` was renamed from
`com.appyfyi.steadygridgallery` to `fyi.appy.steadygridgallery` on
2026-08-21 (commit `c97c10e`), but the `appy.fyi` app page, its screenshots,
and this privacy policy are all still published under the *old* id,
`com.appyfyi.steadygridgallery` — `user_play_id` can never change once
claimed on appy.fyi, and re-claiming under the new id would spend one of
this account's two total claims and orphan everything already uploaded. The
mismatch doesn't block anything here (appy.fyi is enrichment/hosting, not
the actual Play Store listing — that gets whatever `applicationId` the
signed bundle has), but flag it before Play Console setup in §3.3 so the
real package id used there is `fyi.appy.steadygridgallery`, and update
`README.md`'s "Application ID" line (still says the old id) to match.

Status as of 2026-08-20: **the app is feature-complete and ready for the
human-only launch steps.** All build-spec features and every item in
`todo.txt`'s DONE list are implemented, unit tests pass, and the app's
positioning matches what the incumbent's own recent reviews are asking for.
What's left is the work only a human can do: trademark/legal sign-off, a
real icon, Play Console setup, and device testing.

## 1. What I checked before writing this

- **Read `com.simplemobiletools.gallery.pro-build-spec.json`** — all 9
  screens, all 7 features, and the data model are implemented under
  `app/src/main/java/com/appyfyi/steadygridgallery/`.
- **Called the `appy.fyi` API** (key found in `.env`) for the incumbent
  (Simple Gallery Pro):
  - `app_info` — score 4.46, $2.99 one-time, last update 2026-08-12 (so it's
    *not* actually abandoned as of today — see risk note below).
  - `app_photos` — downloaded and viewed all 8 store screenshots. The
    incumbent's editor is far more advanced (Transform/Filter/Adjust/Focus/
    Sticker tabs, custom color theming, pattern/PIN/biometric lock). Our
    spec deliberately scopes v1 down to crop/rotate/filters, which is a
    documented non-goal, not an oversight — fine to ship, worth flagging
    that "advanced editor" is a fast-follow if users ask for it.
  - `reviews` — read all 30 recent reviews. They confirm the exact
    complaints the build spec was designed around: folders/photos
    randomly disappearing, the Advanced Editor hanging at 0%/"preparing to
    export" forever, Camera folder hidden by default, settings resetting,
    and a couple of one-star reviews specifically about the developer being
    acquired and no longer trustworthy. No new complaint theme showed up
    that isn't already covered by the spec's features — I didn't add any
    new scope based on this.
- **Ran `./gradlew testDebugUnitTest`** — build succeeded, all unit tests
  pass.
- **Checked `AndroidManifest.xml`** — only `READ_MEDIA_IMAGES`,
  `READ_MEDIA_VIDEO`, `USE_BIOMETRIC`; no `INTERNET`, no
  `MANAGE_EXTERNAL_STORAGE`. Matches spec.
- **Checked `.gitignore`** — `*.jks` and `.env` are excluded and not
  tracked by git, so the keystore and API key are not at risk of being
  pushed.
- **Spot-checked `todo.txt`'s DONE list against the code** — Save/Save a
  Copy buttons, video playback controls, pinch-to-zoom, swipe transitions,
  video badge on grid thumbnails, and the `es`/`fr` localized string files
  are all genuinely present in the source, not just claimed done.

## 2. Risk note: the incumbent updated 12 days before this report

`app_info.last_update` is 2026-08-12. The "Abandonment / no updates"
complaint the whole spec is positioned around is weaker today than when the
report was written — several August reviews already say some issues were
fixed. This doesn't mean don't launch, but it does mean the store listing
and README's "actively maintained" positioning should lean more on the
*concrete* differentiators that still hold up in this week's reviews
(reliable recycle bin, camera folder always visible, settings that persist,
an editor that doesn't hang) rather than purely on "the other app is dead,"
which is no longer fully true. I'd soften any launch copy that flatly
claims Simple Gallery Pro is unmaintained.

## 3. Things you need to do before this can go live

These are exactly the two `human_gates_required` in the build spec, broken
into concrete steps, plus the launch mechanics.

### 3.1 Trademark & privacy review (`trademark_and_privacy_review` gate)

- [ ] `trademark_cleared` is `false` in the build spec. Before submitting to
      Play Console, confirm the app name "Steady Gallery," the package id
      `com.appyfyi.steadygridgallery`, and the store icon/screenshots don't
      use Simple Mobile Tools' name, logo, or trade dress. The README and
      store listing description currently say "alternative to Simple
      Gallery Pro" with a Play Store link — that kind of comparative
      reference is generally fine, but have this looked at before
      publishing, especially the icon (see 3.2) and any store screenshots.
  - Reference: `README.md`, `store_listing.long_description` /
    `short_description` in the build spec.
- [x] `legal.privacy_policy_accurate` is now `true`. `docs/PRIVACY_POLICY_DRAFT.md`
      has been checked against the shipped app (`LockCredentialStore`,
      `SecureStorage`, `HiddenMediaRepository`, `RecycleRepository`,
      `BillingRepository`), "hidden folders" wording corrected to "Hidden
      Photos," a real contact email added (gilad.kutiel@gmail.com), and
      published to `appy.fyi`'s `privacy_policy` endpoint (the earlier 500
      was appy.fyi's server bug, already fixed). Live at
      **https://appy.fyi/app/com.appyfyi.steadygridgallery/privacy**, and
      `legal.privacy_policy_url` in the build spec now points there instead
      of the never-hosted placeholder.
- [x] Confirmed `legal.regulated_category: "none"` still holds given the
      biometric-unlock feature (see note above) — use this when filling in
      Play's Data Safety form.

### 3.2 Generate the real app icon — done

The launcher icon is generated (2026-08-20) via the `build-from-spec-plugin`'s
deterministic icon generator (`genLauncherIcon.ts`), which reads
`design_system.icon_name` (`images`) and `color_primary_hex` (`#1565C0`) from
the build spec and writes a real adaptive-icon resource set — no more
placeholder comment in `ic_launcher_foreground.xml`:

- [x] `app/src/main/res/drawable/ic_launcher_background.xml` — flat
      `#1565C0` fill.
- [x] `app/src/main/res/drawable/ic_launcher_foreground.xml` — white
      Phosphor "images" glyph (photo-stack/frame), auto-picked for contrast
      against the background.
- [x] `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and
      `ic_launcher_round.xml` — adaptive-icon wrappers. No legacy density
      PNG mipmaps needed since `min_sdk` is 33 (adaptive icons require API
      26+, so `--legacy` fallback rasterization wasn't run).
- [x] `store_listing/play_store_icon_512.png` — flat 512×512 RGBA PNG of the
      same background+glyph, for the Play Console app icon upload field
      (adaptive icon layers aren't accepted there directly).

Note this is **not** the icon described by `store_listing.icon_prompt`
(deep blue + white photo frame + green shield badge + restore arrow) — that
prompt is explicitly unused by this generation path (it's a plain
brand-color-fill + single glyph, not the shield/arrow composition). The
result is simpler than the placeholder XML it replaced (which already had a
hand-drawn frame/mountain/shield/arrow design) but is a real, finished,
non-placeholder asset. If you want the fuller shield/restore-arrow icon
described in the spec's prompt instead, that still needs to be produced with
an image-generation tool by hand — flag this to me if you'd like that
version instead of the deterministic one.

### 3.3 Google Play Console setup

- [ ] Create the app entry with `package_id: com.appyfyi.steadygridgallery`.
- [ ] Configure the one-time in-app product `steady_gallery_pro_unlock`
      as `ProductType.INAPP` at $2.99 — this must exist in Play Console
      *before* `BillingRepository` can query it successfully.
- [ ] Fill in the Play Data Safety form using `docs/PRIVACY_POLICY_DRAFT.md`
      (once verified) as the source of truth — the answer should be "no
      data collected" across the board.
- [ ] Enter store listing text from `store_listing` in the build spec
      (title, short/long description, category: Photography, keywords).
- [ ] Upload real screenshots of **Steady Gallery itself** — the ones I
      downloaded (`/tmp/.../scratchpad/appy/*.png`) are the *incumbent's*
      screenshots, pulled only for UX reference. Take fresh screenshots
      from a running build of this app before submitting.
- [ ] `human_gates_required` also lists `closed_testing_recruitment`:
      recruit the testers needed for Play's closed-testing track (Play
      requires 12 testers opted in for 14 continuous days before a new
      developer account can request production access). Start this early
      since it's the long pole — everything else here can be done in
      parallel, but this one has a hard 14-day minimum.

### 3.4 Build and sign the release

Run from the repo root (the keystore already exists at
`steady-gallery-upload.jks`, gitignored, generated with the placeholder
password `changeit` from `build_instructions`):

```bash
chmod +x ./gradlew
./gradlew clean
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest   # needs a running emulator/device, see 3.5
```

- [ ] **Before building the real release artifact**, regenerate the
      keystore with a strong, unique password instead of `changeit` — that
      value only exists as a local dev convenience in the build spec's
      example command, not something to ship with. Store the real
      passwords in a password manager, not in the shell history or any
      committed file.
- [ ] Then build the signed bundle:
  ```bash
  RELEASE_STORE_FILE="$PWD/steady-gallery-upload.jks" \
  RELEASE_STORE_PASSWORD="<your real password>" \
  RELEASE_KEY_ALIAS="upload" \
  RELEASE_KEY_PASSWORD="<your real password>" \
  ./gradlew bundleRelease
  ```
- [ ] Upload the resulting `.aab` from `app/build/outputs/bundle/release/`
      to Play Console's internal testing track first.

### 3.5 Device testing (do this before wide release)

The build spec's `test_plan` has 9 scenarios (4 instrumented, 2 unit — unit
already passing, 3 manual). The instrumented and manual ones need an actual
device/emulator and haven't been run yet:

- [ ] Run `./gradlew connectedDebugAndroidTest` against an API 35
      emulator or device for the 4 instrumented scenarios (editor export
      progress, hidden-folder data safety, Camera folder ordering,
      recycle-bin restore).
- [ ] Manually walk the 3 manual scenarios: fresh-install permission flow
      on Android 15, a real Play Billing test purchase using a license
      tester account, and reading the manifest/Settings screen to confirm
      the privacy/current-Android messaging is visible.

## 4. Suggested order of operations

1. Kick off tester recruitment for closed testing now (3.3) — it's the
   14-day floor, start the clock immediately.
2. Icon is generated (3.2, done) — get trademark/privacy sign-off (3.1),
   which doesn't block on anything else here.
3. Run the instrumented + manual test pass (3.5) on an emulator.
4. Regenerate the release keystore password, build the signed AAB (3.4),
   and upload to internal testing.
5. Once privacy policy is published and testers have accumulated 14 days,
   fill in the rest of Play Console (3.3) and submit for production.

Everything in this doc is a human/account-level step — there's no more
code work I'd recommend before you start on it. Let me know if you want me
to soften the "unmaintained" framing in the README/store listing per the
risk note in §2, or make any other content changes while you work through
the checklist above.
