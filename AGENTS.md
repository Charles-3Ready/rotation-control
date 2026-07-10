# Rotation Control

Follow the root workspace rules in `C:\Users\Charles\AI Projects\AGENTS.md`.

Android app that applies **per-app screen orientation** rules (e.g. force games to landscape) using:

1. Accessibility service — detects which app is in the foreground
2. `WRITE_SETTINGS` — applies system rotation lock / unlock

**No root.** Do not reintroduce `su` / Magisk / `wm` shell paths unless Charles explicitly asks.

## Guardrails

- Do not add API keys, tokens, passwords, or device-specific private data to tracked files.
- Accessibility must only watch foreground package and apply orientation; no keystroke capture, screenshots, or remote control.
- Prefer conservative system settings changes (`ACCELEROMETER_ROTATION` / `USER_ROTATION`) over invasive overlays unless the user asks.
- Setup scripts should be inspectable ADB helpers only.

## Build / install

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Or `.\scripts\install-adb.ps1` when a device/emulator is connected.
