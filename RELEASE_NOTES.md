# Rotation Control v0.2.0 — *Stop fighting your tablet*

Your tablet has one job: show stuff the way **you** want. Portrait for reading. Landscape for raid night. Auto when you’re half-horizontal on the couch watching Stremio.

**Rotation Control** makes Android obey — per app — with **no root**.

---

## What’s in the box

- **Per-app rules** — Landscape / Portrait / Reverse / Auto / System default  
- **Auto that actually works** — sensor-driven force locks (not the fake FULL_SENSOR that tablets ignore)  
- **Vanishing lock bubble** — freeze the current angle, icon peaces out so it doesn’t sit on your video  
- **Onboarding that grants permissions** — Accessibility + Modify system settings right in the tutorial  
- **No stuck landscape home screen** — leave the app, rotation restores  

---

## Install

1. Grab **`RotationControl-0.2.0-release.apk`** below (prefer release over debug)  
2. Sideload it  
3. Open app → turn on **Accessibility** + **Modify system settings**  
4. Set Hearthstone → Landscape. Set Stremio → Auto. Smile.

```bash
adb install -r RotationControl-0.2.0-release.apk
```

---

## Screenshots

See the repo README for full-size renders of the main UI, onboarding, and lock bubble.

---

## Honest fine print

- Accessibility only sees the **foreground package name** (not your passwords)  
- Some apps still try to fight; Landscape/Auto force-locks usually win  
- Built for real tablets (tested on iPlay 70 Mini Pro)

---

**Tagline for the ages:**  
*Stop fighting your tablet. Start fighting the boss.*

— Grokked with love · v0.2.0
