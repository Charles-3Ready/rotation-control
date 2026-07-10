# Rotation Control v0.2.0

Per-app screen orientation for Android. Force landscape or portrait per app, or use Auto so orientation follows the device tilt. No root.

## What’s new

- **Per-app rules** — System default, Portrait, Landscape, reverse modes, Auto (sensor)
- **Auto that holds on tablets** — sensor-driven fixed locks instead of free sensor mode (many OEMs ignore that)
- **Lock icon** — while Auto is on, freeze the current angle; the control auto-hides after a few seconds
- **Onboarding** — grant Accessibility and Modify system settings in the setup flow
- **Clean restore** — leaving a ruled app restores prior system rotation (no stuck landscape home)

## Install

Download **`RotationControl-0.2.0-release.apk`** from this release.

```bash
adb install -r RotationControl-0.2.0-release.apk
```

Then open the app, enable Accessibility, allow Modify system settings, and set rules for your apps.

## Assets in this release

| File | Notes |
|------|--------|
| `RotationControl-0.2.0-release.apk` | Recommended build |
| `RotationControl-0.2.0-debug.apk` | Debug build |
| `SHA256SUMS.txt` | Checksums |
| `storefront.png` / `onboarding.png` | UI renders |

## Notes

- Accessibility only reads the foreground **package name**.
- If an app still refuses to rotate, try a fixed Landscape or Portrait rule.
- Tested on iPlay 70 Mini Pro (Android 16).

Source and docs: https://github.com/Charles-3Ready/rotation-control
