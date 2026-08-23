# Pro plan

Steady Gallery is free, with two features gated behind a single one-time unlock:
**Editor** and **Hidden Photos**. This document covers what's gated, how the
unlock actually works under the hood, what happens across reinstalls/devices,
and how to comp Pro to testers or as a gift.

## What's Pro

| Feature | Gate location |
|---|---|
| Editor (crop, rotate, save/save-a-copy) | `EditorViewModel.load()` — blocks with `EditorPhase.PURCHASE_REQUIRED` if not entitled |
| Hidden Photos (hide/unhide into the PIN/biometric-locked folder) | `MediaGridViewModel.hideSelected()` — sends `RequiresPurchaseToHide` if not entitled |

Everything else — the media grid, folders, viewer (photo/video playback,
swipe transitions, pinch zoom), Recycle Bin, and Settings — is free and
ungated. There is no subscription, no tiered feature set, and no server
account: it's a single "Unlock Pro" purchase.

Product copy (`purchase_description` in `strings.xml`):
> "Unlock the editor and hidden photos forever with a single, one-time
> purchase. No subscription, no account required."

Default listed price is $2.99 (`purchase_default_price`), overridden at
runtime by whatever Play Console has configured for the product.

## How the unlock is implemented

- **Product type**: a single Google Play Billing **one-time product (INAPP)**,
  ID `steady_gallery_pro_unlock` (`PurchaseEntitlementStore.kt`). Not a
  subscription, not consumable — it's the same model as "remove ads" buttons.
- **Purchase flow**: `BillingRepository` wraps the Play Billing Library.
  `launchPurchaseFlow()` opens Play's purchase sheet; the result comes back
  through `PurchasesUpdatedListener` → `handlePurchase()`, which acknowledges
  the purchase with Play (required within 3 days or Play auto-refunds it) and
  then persists a local entitlement flag.
- **Local entitlement cache**: `PurchaseEntitlementStore` writes
  `isPurchased = true` plus a SHA-256 hash of the purchase token into
  `EncryptedSharedPreferences` (`SecureStorage.kt`, AES-256-GCM, keyed by an
  Android Keystore master key). The full purchase token isn't stored, only
  its hash — enough to notice tampering, not enough to be a credential worth
  stealing.
- **No server-side receipt validation** (documented explicitly in
  `BillingRepository`'s class doc): the app trusts Play Billing's on-device
  acknowledgement. There's no backend, so this is a deliberate v1 tradeoff —
  see "Can this be cracked?" below.
- **Restore path**: the "Restore purchase" button on the Unlock Pro screen
  calls `BillingRepository.refreshPurchases()`, which asks Play Billing
  (`queryPurchasesAsync`) what the signed-in Google account currently owns
  and re-derives the local entitlement from that — it does not talk to any
  Anthropic/appy.fyi server, only Play.
- **Startup check**: `MainActivity` calls
  `billingRepository.startConnectionAndLoad()` on every app launch, which
  itself calls `refreshPurchases()` once the Billing connection is up. So the
  "restore" logic effectively runs automatically every time the app opens,
  not just when the user taps the button.

## Reinstalls, new devices, and app data

The entitlement flag lives only in local `EncryptedSharedPreferences` — it is
**not** backed up:

```xml
<!-- AndroidManifest.xml -->
android:allowBackup="false"
android:fullBackupContent="false"
android:dataExtractionRules="@xml/data_extraction_rules"
```

`data_extraction_rules.xml` excludes `sharedpref`, `database`, and `file`
domains from cloud backup entirely. This is intentional (it's the same file
that also protects Hidden Photos' PIN/credential store from being pulled off
the device via `adb backup` or Play's cloud backup) — but it means:

- **Reinstall the app (same device, same Google account)**: local entitlement
  is wiped with the app data. On next launch, `startConnectionAndLoad()` →
  `refreshPurchases()` asks Play "what does this account own?", finds the
  existing INAPP purchase (Play tracks one-time purchase ownership per Google
  account, not per install), and silently re-persists the local flag. **Pro
  comes back automatically**, no purchase, no manual restore needed — as
  long as the device has network and Play Store access at that moment.
- **New device, same Google account**: identical situation to a reinstall —
  Play still considers the account entitled, so it restores the same way.
- **New device or reinstall, offline**: Pro will show as locked until the
  next successful `refreshPurchases()` call (i.e., until Play Billing can
  reach Play). There's no error state telling the user "you're offline, Pro
  will come back once you reconnect" — it just looks unpurchased until the
  connection succeeds. Worth a UX note if this causes support questions.
- **Different Google account on the same or a new device**: not entitled,
  same as any other Play app — the purchase belongs to the account, not the
  device or the app install. This is standard and expected.
- **Uninstall Google Play services / sideload without Play**: `BillingUnavailable`
  state — Pro can't be verified or purchased at all in that environment.

## Debug/test bypass (not shippable)

`BillingRepository.debugGrantEntitlement()` /
`debugRevokeEntitlement()` let you flip the local entitlement flag directly,
skipping Play Billing entirely. Both are guarded by `BuildConfig.DEBUG`, and
the whole UI block that exposes them in `PurchaseScreen` is wrapped in the
same check — since `BuildConfig.DEBUG` is a compile-time constant, this code
is stripped out of release builds by R8, not just hidden behind a flag. It
only exists to let Editor/Hidden Photos be exercised in development before
the app is registered in Play Console. It cannot be used to comp real users.

## Gifting Pro / free tester access

There's no in-app gifting, promo-code redemption, or license-key system —
v1 has no backend to issue or validate that kind of grant. The available
options are the ones Google Play itself provides for one-time IAP products:

1. **Play Console Closed/Internal testing + license testers** (best fit for
   QA/beta testers). Add testers' Google accounts under Play Console →
   Setup → License testing (or as testers on an Internal/Closed testing
   track). Play Billing then either lets them purchase with a test payment
   method that's never charged, or — depending on how the product/track is
   configured — shows the purchase as free for license testers. This is the
   standard way to let testers exercise `launchPurchaseFlow()` end-to-end
   without paying.
2. **Promo codes** — **not available for managed in-app products** on Google
   Play. Play's promo code system only covers paid *app* installs and
   subscriptions in some cases; one-time managed products like
   `steady_gallery_pro_unlock` cannot be distributed via promo code. (If Pro
   ever became a subscription, promo codes would be back on the table.)
3. **Manually grant + Play refund** — buy it normally on a tester's behalf
   and refund via Play Console, or have the tester buy and get refunded.
   Clunky, and refunding also revokes the local entitlement next time
   `refreshPurchases()` runs, so this only works for a one-off demo, not a
   standing "gift."
4. **A debug/QA build** — hand a tester a build with `debugGrantEntitlement()`
   reachable (i.e. a `DEBUG` build, or temporarily relax that guard for an
   internal QA build channel). Works today with zero new code, but it's a
   different APK than what ships, and it's an all-or-nothing switch (no
   per-user tracking of who has a comped Pro).

**If real gifting/comping becomes a recurring need** (press, influencers,
support goodwill), the durable fix is a small addition: either (a) a
server-issued signed entitlement token the app can validate offline, or
(b) switching the debug-grant path into a "redeem code" flow gated by a
lightweight backend that mints one-time codes. Both require standing up a
backend Steady Gallery doesn't currently have — out of scope for v1.

## Can this be cracked?

Yes, easily, and that's a known/accepted v1 tradeoff, not an oversight:
since there's no server-side receipt validation, a rooted/patched APK could
force `PurchaseEntitlementStore.setEntitlement(isPurchased = true)` or bypass
the check outright. For a $2.99 one-time unlock on a niche open-source
gallery app, server-side validation hasn't been worth the backend cost. If
that calculus changes (piracy becomes visible, or Pro pricing goes up),
revisit with Play's server-side purchase verification (Play Developer API)
issuing a signed token the client checks — the same infrastructure that
would also unlock proper gifting (above).
