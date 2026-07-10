# Rotation Control

### Your tablet. Your rules. Landscape when you want it, Auto when you don’t.

<p align="center">
  <img src="docs/storefront.png" alt="Rotation Control storefront renders" width="900" />
</p>

**Force any app into portrait, landscape, or sensor-driven Auto** — without root, without yelling at Android’s rotation lock.

Built for tablets (hello iPlay) that love portrait mode *exactly* when you’re mid-raid or mid-movie.

---

## Why does this exist?

Because:

- Games that *should* be landscape… aren’t  
- Video apps that *could* rotate… refuse  
- Auto-rotate is either always-on chaos or always-off brick  

**Rotation Control** watches which app is open and applies *your* orientation rule. Leave the app → your tablet goes back to normal. No stuck landscape home screen. No root. No drama.

---

## Screenshots

| Main rules | Onboarding setup |
|------------|------------------|
| <img src="docs/storefront.png" width="420" alt="App UI" /> | <img src="docs/onboarding-mock.png" width="420" alt="Onboarding" /> |

---

## Features that slap

| Feature | What it does |
|---------|----------------|
| **Per-app rules** | Landscape / Portrait / Reverse / Auto / System default |
| **Auto (sensor)** | Follows tilt with the same force-lock that actually works on stubborn OEMs |
| **Lock bubble** | Floating lock icon freezes the current angle, then **disappears** so it stays out of your face |
| **Onboarding** | Grant Accessibility + Modify system settings *in the tutorial* |
| **No root** | Accessibility overlay + system settings only |

---

## Install (sideload)

1. Download **`RotationControl-0.2.0-release.apk`** from [Releases](../../releases)  
2. Install on your Android device (Unknown sources / “Install unknown apps”)  
3. Open the app → finish onboarding permissions  
4. Set a game to **Landscape**, a video app to **Auto**, go live  

Or with ADB:

```bash
adb install -r RotationControl-0.2.0-release.apk
```

---

## Permissions

1. **Accessibility** *(required)* — sees the foreground app package only (not keystrokes, not screen content)  
2. **Modify system settings** *(recommended)* — applies force orientation locks  

Privacy one-liner: we only care which **app** is open, so we can rotate the glass.

---

## How Auto works (the spicy part)

Many tablets ignore “sensor free rotate” overlays. So Auto **listens to the accelerometer** and applies the same fixed force-lock as Landscape — just following your tilt. Tap the vanishing lock icon to freeze mid-watch.

---

## Build from source

```powershell
cd rotation-control
.\gradlew.bat assembleRelease
# APK: app\build\outputs\apk\release\app-release.apk
```

Requires Android SDK 35 / JDK 17.

---

## Limits (honest)

- Some apps hard-fight orientation; Landscape usually wins, Auto almost always does  
- OEM battery savers can delay Accessibility — whitelist the app if it feels sleepy  
- Not a Play Store listing (yet) — GitHub Releases only  

---

## License

Personal / open use. Don’t sell it as your own product. Do enjoy horizontal Hearthstone.

---

<p align="center"><b>Stop fighting your tablet. Start fighting the boss.</b><br/>Rotation Control · v0.2.0</p>
