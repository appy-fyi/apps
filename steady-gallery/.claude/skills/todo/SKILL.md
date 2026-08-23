---
name: todo
description: Read tasks out of todo.txt's TODO section, implement each one, verify it live on a running Android emulator (build/install/exercise/screenshot/logcat, looping until it actually works), move the finished line to the DONE section, and once every task is done push the final build to the connected phone. Use when the user says "work the todo list", "do the todos", or references todo.txt directly.
---

This project is a native Android app (`applicationId com.appyfyi.steadygridgallery`,
module `app`, Kotlin/Compose, min SDK 33). `todo.txt` at the repo root is the
task queue:

```
TODO:
- <task 1>
- <task 2>

DONE:
```

Work one task at a time, in order, and keep `todo.txt` truthful at every step
— if you stop partway through, the file should reflect exactly what's done
and what isn't.

## 0. Set up devices

Run `adb devices -l`. You need two distinct targets:

- **Emulator** — the serial starting with `emulator-`. All build/test/iterate
  cycles happen here. Never test against the phone.
- **Phone** — any other serial (a real device id, not `emulator-*`). This
  only gets touched once, at the very end, for the final push.

If no emulator is running, start one (`emulator -avd <name> -no-snapshot-load`
or whatever AVD is configured) before continuing — don't test blind. If
there's a project `run` skill or the general-purpose `run` skill covers
launching this app, you can lean on it for the install/launch step; otherwise
use the raw `adb`/`gradlew` commands below directly.

Pin the emulator as the install target for the whole loop, e.g.
`export ANDROID_SERIAL=emulator-5554` (or pass `-s <serial>` on every `adb`
call) — this prevents an accidental install onto the phone mid-loop.

## 1. Take the next task

Read `todo.txt`. Take the first line under `TODO:` that isn't already under
`DONE:`. If `TODO:` is empty, skip to step 4 (final push) — there's nothing
left to implement, but the user still asked for a push.

## 2. Implement it

Read the relevant source under `app/src/main/java/...` (and check `git
status`/`git diff` — there may already be in-progress work relevant to the
task, as with `EditorScreen.kt` for a cropping task). Make the change. Keep
it scoped to the task; don't drag in unrelated cleanup.

## 3. Verify on the emulator, loop until it's actually fixed

This is the part the user explicitly cares about — don't mark anything done
off of "the code looks right."

1. Build and install the debug build to the emulator:
   `./gradlew installDebug` (with `ANDROID_SERIAL` pinned per step 0), or
   `./gradlew assembleDebug && adb -s <emulator-serial> install -r
   app/build/outputs/apk/debug/app-debug.apk`.
2. Launch the app:
   `adb -s <emulator-serial> shell monkey -p com.appyfyi.steadygridgallery -c android.intent.category.LAUNCHER 1`.
3. Exercise the exact behavior the task describes. Drive the UI with
   `adb shell input tap|swipe|keyevent ...` (use real coordinates/timing —
   e.g. a slow multi-step drag for a "dragging gets stuck" bug, a fling for a
   swipe-transition bug), then check what happened:
   - `adb -s <emulator-serial> exec-out screencap -p > shot.png` and Read the
     image back — for gesture/animation/visual-feel bugs this is the actual
     test; take several shots through a gesture/animation if a single frame
     can't show it (e.g. mid-drag, mid-swipe).
   - `adb -s <emulator-serial> logcat -d '*:E'` (or grep for the app's
     package/tag) for crashes, exceptions, or ANRs the screenshot wouldn't
     show.
   - If the bug is really a logic bug (not a feel/gesture bug), prefer
     backing it with a real unit test (`./gradlew testDebugUnitTest`) or
     instrumented test (`./gradlew connectedDebugAndroidTest`, UiAutomator/
     Espresso are already on the classpath) in addition to the manual check
     — but manual on-device verification is required regardless, since
     that's what was asked for.
4. If it's not right yet, fix the code and go back to step 3.1. Cap it at
   roughly 5 build/test iterations on a single task before stopping to tell
   the user what's still wrong and what you tried — don't loop forever
   silently.

## 4. Mark it done

Once verified, edit `todo.txt`: remove the line from under `TODO:` and append
it under `DONE:`, unchanged (don't add commentary/dates to the line itself).
Save, then go back to step 1 for the next task.

Do not `git commit` as part of this loop unless the user separately asks —
leave the diff for them to review.

## 5. Final push to the phone

Once every task from `TODO:` has moved to `DONE:` (or `TODO:` was already
empty):

1. Do one last smoke pass on the emulator covering the app's main flows
   (open gallery, open an image in the editor, the features you just
   touched) to catch any regression from the last change before it goes to
   a real device.
2. Build and install to the phone (the non-`emulator-*` serial from step 0):
   `adb -s <phone-serial> install -r app/build/outputs/apk/debug/app-debug.apk`
   (rebuild first with `./gradlew assembleDebug` if the emulator install in
   step 3 used a now-stale APK). Use the debug build unless the user has
   asked for a signed release build and the release-signing env vars
   (`RELEASE_STORE_FILE` etc.) are actually set.
3. Confirm the install landed: `adb -s <phone-serial> shell pm list packages
   | grep com.appyfyi.steadygridgallery`.

## 6. Report

Summarize what was implemented per task, how each was verified on the
emulator (what you saw, not just "should work"), confirm the phone install,
and flag anything left under `TODO:` that you deliberately didn't attempt
(and why) or any task you stopped mid-loop on.
