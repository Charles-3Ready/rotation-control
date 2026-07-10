# Publish Rotation Control to Google Play

This is the practical path from this repo to a listing on the Play Store.

## Reality check (read first)

This app uses **Accessibility** + **Modify system settings** + (currently) **Query all packages**. Google reviews those carefully.

| Item | Play impact |
|------|-------------|
| Accessibility service | Must declare use in Play Console; description must match code (foreground package only → orientation). Misleading = rejection. |
| `WRITE_SETTINGS` | Allowed; explain in listing (“changes rotation lock”). |
| `QUERY_ALL_PACKAGES` | **Restricted.** Often rejected for a simple utility. Prefer listing launchable apps via `<queries>` / launcher intent (we should switch before submit). |
| Privacy policy URL | **Required** (HTTPS). Host on your site or GitHub Pages. |
| App Bundle (`.aab`) | Required for new apps (not a raw APK). |
| Developer account | One-time **$25** fee: [play.google.com/console](https://play.google.com/console) |
| Review time | Often 3–7+ days for accessibility apps; first app can take longer. |

Sideloading via ADB remains fine without Play.

---

## Step 1 — Google Play developer account

1. Open [Google Play Console](https://play.google.com/console)
2. Sign in with the Google account you want as the publisher (e.g. `charles.3ready@gmail.com` or a dedicated one)
3. Pay the one-time registration fee
4. Complete identity / account details when asked

---

## Step 2 — Create an upload keystore (once, keep forever)

**Do not commit the keystore or passwords.** Store them offline + backup.

In PowerShell (change the path if you want):

```powershell
cd "C:\Users\Charles\AI Projects\rotation-control"
$keyDir = "$env:USERPROFILE\AndroidKeys"
New-Item -ItemType Directory -Force -Path $keyDir | Out-Null

keytool -genkeypair -v `
  -keystore "$keyDir\rotation-control-upload.jks" `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias rotation-control `
  -storepass "PUT_A_LONG_PASSWORD_HERE" `
  -keypass "PUT_A_LONG_PASSWORD_HERE" `
  -dname "CN=Charles Luminary, OU=Mobile, O=Luminary Studios, L=Manila, ST=NCR, C=PH"
```

Copy the example props file and fill secrets **locally only**:

```powershell
copy keystore.properties.example keystore.properties
# edit keystore.properties — it is gitignored
```

`keystore.properties` example:

```properties
storeFile=C:/Users/Charles/AndroidKeys/rotation-control-upload.jks
storePassword=...
keyAlias=rotation-control
keyPassword=...
```

If you lose this keystore, you **cannot** update the same Play listing with a normal key (Google Play App Signing helps for the *app* key, but you still need the *upload* key).

---

## Step 3 — Build a signed App Bundle

```powershell
cd "C:\Users\Charles\AI Projects\rotation-control"
.\gradlew.bat bundleRelease
```

Output:

```text
app\build\outputs\bundle\release\app-release.aab
```

Optional APK for sideload testing of the release build:

```powershell
.\gradlew.bat assembleRelease
```

---

## Step 4 — Create the app in Play Console

1. **Create app** → name: `Rotation Control` (or your store title)
2. Default language, app type: **App**, free/paid
3. Declarations: privacy policy, export compliance, ads (likely **no ads**), etc.

### Store listing (minimum)

| Field | Suggestion |
|-------|------------|
| Short description | Per-app screen orientation. Force games to landscape, readers to portrait. |
| Full description | What it does, how to enable Accessibility + Modify system settings, what it does **not** do (no password reading, no ads). |
| App icon | 512×512 PNG — use `assets/icon/icon-full-1024.png` resized to 512 |
| Feature graphic | 1024×500 PNG (required) |
| Screenshots | Phone **and** 7" tablet if you target tablets (iPlay is a tablet) — at least 2 phone shots |
| Category | Tools |
| Contact email | Your public support email |

### Data safety

Declare:

- Data collected: **none** (or “app interactions” only if you later add analytics — currently local SharedPreferences only)
- Data shared: **no**
- Security practices: data not encrypted in transit if no network — “app does not collect user data” is simplest if accurate

### Sensitive permissions

**Accessibility**

- Purpose: detect which app is in the foreground so a saved orientation rule can be applied
- Explicitly: does **not** read screen content, keystrokes, or credentials

**Photos / other**

- Not used — don’t claim them

If you keep `QUERY_ALL_PACKAGES`, fill the restricted permission form honestly. Prefer removing it before submit (see `AGENTS.md` / follow-up code change).

---

## Step 5 — Upload & release track

1. **Release → Testing → Internal testing** (recommended first)
2. Create a new release → upload `app-release.aab`
3. Add yourself as a tester (email list)
4. Roll out internal → install from the Play opt-in link on the tablet
5. When solid: **Closed / Open testing**, then **Production**

Production can require a completed store listing + policy forms.

---

## Step 6 — Privacy policy (required)

Host a short HTTPS page, e.g. on luminarystudiosph.com or GitHub Pages. Include:

- App name and package: `com.charles.rotationcontrol`
- Permissions used and why (Accessibility, write settings, boot, notifications if used)
- Data: rules stored **only on device**; no account; no sale of data
- Contact email
- Link this URL in Play Console → App content → Privacy policy

A draft lives in `docs/privacy-policy-draft.md` — publish it somewhere public before submit.

---

## Checklist before Production

- [ ] Dial icon shipped in release build
- [ ] `versionCode` / `versionName` bumped for each upload
- [ ] Signed **AAB** with upload keystore
- [ ] Privacy policy URL live
- [ ] Accessibility declaration written carefully
- [ ] `QUERY_ALL_PACKAGES` removed or justified
- [ ] Screenshots + 512 icon + feature graphic
- [ ] Tested on a clean device from the internal track install
- [ ] Keystore backed up offline

---

## Commands cheat sheet

```powershell
# Icon (dial concept)
python .\scripts\build-icons.py

# Release bundle
.\gradlew.bat bundleRelease

# Install release APK over USB (not Play)
adb install -r app\build\outputs\apk\release\app-release.apk
```

---

## Cost & timeline (typical)

| Item | Notes |
|------|--------|
| Developer fee | $25 once |
| Review | Days; accessibility apps often slower |
| Updates | Free; each needs new `versionCode` |

---

## If you only want “on Google” for yourself

- **Internal testing** is enough for you + a few devices without a public listing.
- Full public production needs the complete store presence and policy review.
