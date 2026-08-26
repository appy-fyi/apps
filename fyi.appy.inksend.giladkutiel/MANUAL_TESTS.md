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

## On-device translation of the typed text (ML Kit)

Mood detection scores an English-only dictionary, so non-English text is translated to English
on-device first. The translation is async + needs a one-time per-language model download, which
is not unit-testable.

**Steps:**
1. On a device with network, enable the accessibility + overlay permissions and open WhatsApp.
2. Type a clearly non-English, clearly moody line — e.g. Spanish `te quiero mucho mi amor`
   (romantic) or French `je suis vraiment furieux` (angry). Wait ~1s after you stop typing.
3. Watch the floating button: within a second or two of the pause it should recolour/reglyph
   to the matching mood (romantic pink + ❤️, angry red + 😤), not stay on the neutral 🫟.
4. Tap it and confirm the rendered image uses that mood's look **and still shows your original
   (non-English) text**, not an English translation.
5. Turn off all networking and repeat step 2 with a *new* language whose model never downloaded:
   the button stays neutral (translation unavailable) but emoji in the text still drive the mood,
   and the app never blocks or crashes.

**Expected:** after the brief debounce, non-English text drives the correct mood; the rendered
image keeps the user's original text; offline with no model, it degrades quietly to neutral /
emoji-only.

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
