# Steady Gallery

**A free, open source alternative to [Simple Gallery Pro](https://play.google.com/store/apps/details?id=com.simplemobiletools.gallery.pro).**

Simple Gallery Pro's source went closed and its updates slowed down, leaving
users worried about abandonment and data loss. Steady Gallery is a native
Android gallery app built from scratch to fill that gap: fully open source,
actively maintained, and built for current Android versions from day one.

Your photos and videos never leave your device. No account, no cloud sync,
no analytics, no AI processing.

## Features

- **Folder browsing** — Local folders built from `MediaStore` with stable
  folder keys, so folders don't randomly disappear or reset. Your Camera
  folder always surfaces first.
- **Bulletproof recycle bin** — Deleting an item copies and verifies it
  first, then removes the original, so you never lose the only copy of a
  photo to a failed delete.
- **Hidden folders** — Hide folders from the main gallery and unlock them
  with a PIN or biometrics. Hiding never moves, renames, or re-encrypts your
  files — it's just a view-level lock.
- **Photo editor** — Crop (including fixed vertical/horizontal ratios),
  90° rotate, and filters (Grayscale, Sepia, High Contrast), with a
  reliable export that shows real progress instead of hanging at 0%.
- **Video playback** — Play videos from your library with on-screen
  play/pause and a scrubbable progress bar, and spot them at a glance via a
  video badge on grid thumbnails.
- **Pinch to zoom** — Pinch in and out on any photo in the full-screen
  viewer.
- **Swipe transitions** — Smooth animated transitions when swiping between
  photos and videos.
- **Persistent settings** — Sort order, grid size, and theme are saved
  locally and survive app restarts — they don't silently reset.
- **Privacy-first by design** — No `INTERNET` permission, no analytics, no
  crash reporting, no remote config. Everything runs entirely on-device.

## Get it

Steady Gallery is free and open source. Build it yourself from this
repository, or grab it from the Play Store.

## Building from source

Steady Gallery is a native Android app written in Kotlin with Jetpack
Compose.

- **Module:** `app`
- **Application ID:** `com.appyfyi.steadygridgallery`
- **Min SDK:** 33 · **Target SDK:** 35

```
./gradlew assembleDebug
```

Open the project in Android Studio, or install the debug build directly
with `./gradlew installDebug` on a connected device or emulator.

## Contributing

Issues and pull requests are welcome. Since Steady Gallery exists precisely
because another gallery app stalled, keeping this one responsive to bug
reports and feature requests is the whole point.

## License

See the repository for license details.
