# Manual test plan

Scenarios from the build spec's `test_plan` that are `"kind": "manual"` — not automatable,
run by a person on a real device.

## Guided Keyboard Setup with Live Test

**Scenario:** a first-time user must reach a working keyboard using only in-app guidance.

**Steps:**
1. On a fresh device/emulator with InkSend just installed, follow only the in-app Keyboard
   Setup screen — no other instructions.
2. Tap "Enable in Settings", enable the keyboard, return to the app, tap "Choose Keyboard",
   select InkSend.
3. Type into the live test-typing box on the Keyboard Setup screen.

**Expected:** a first-time user reaches a fully working, selected InkSend keyboard using only
the in-app guided steps, confirmed by successfully typing in the live test box — unlike the
incumbent's own reviews flagging missing Samsung setup directions.

## One-Time Purchase, No Ads (Play Billing test-purchase flow)

The `test_plan` billing scenario ("Trial/subscription confusion") is written as an instrumented
test, but Play Billing's actual purchase confirmation requires a device signed into a Google
account with access to this app's testing track in Play Console — that's outside what this build
environment can drive end to end. The structural half (the app only ever queries/launches the
single non-consumable `unlock_all_styles` INAPP product, never a subscription product id) is
covered by `BillingRepository`'s implementation directly. To manually confirm the full purchase
UX once the app is uploaded to internal testing (see `/appy:publish`):

1. Install the internal-testing build on a device signed into a licensed tester account.
2. Open Style Editor and tap into a color picker while unpurchased — confirm the purchase prompt
   shows only the single $4.99 one-time price, no trial or subscription language.
3. Complete a test purchase through Play Billing's own UI.
4. Confirm custom colors and the Handwriting Font Creator unlock immediately, and that
   "Restore purchase" on a fresh install (same Google account) unlocks them again without
   repurchasing.
