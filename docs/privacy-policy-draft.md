# Privacy Policy — Rotation Control

**Package:** `com.charles.rotationcontrol`  
**Developer:** Charles Luminary  
**Contact:** charles.3ready@gmail.com  
**Last updated:** 2026-07-10

## What the app does

Rotation Control lets you set a preferred screen orientation per installed app (for example, landscape for a game). When that app is in the foreground, the app adjusts the device orientation settings; when you leave it, previous settings are restored when possible.

## Data we collect

**We do not collect, transmit, or sell personal data.**

- Orientation rules are stored **only on your device** (local app storage).
- The app does **not** require an account.
- The app does **not** include analytics, advertising, or crash-reporting SDKs that send data off-device (unless added later; this policy will be updated if that changes).

## Permissions

| Permission | Why |
|------------|-----|
| Accessibility | Detect which app is currently in the foreground so the correct orientation rule can be applied. The service is limited to window/package changes for this purpose. It is **not** used to read passwords, keystrokes, or screen content for any other purpose. |
| Modify system settings | Change system auto-rotate / user rotation to apply your rules. |
| Query installed apps (if present) | Show the list of apps so you can assign rules. |
| Boot completed | Optional hook after reboot; Accessibility must still be enabled by you. |
| Notifications (if requested) | Optional status; not required for core function. |

You can revoke Accessibility or “modify system settings” at any time in Android Settings; the app will stop enforcing rules.

## Children’s privacy

The app is not directed at children under 13. We do not knowingly collect children’s data.

## Changes

We may update this policy. The “Last updated” date will change when we do. Continued use after changes means you accept the updated policy.

## Contact

Questions: **charles.3ready@gmail.com**
