# Site Photos 219-220 (Android)

Take site photos per **Project (219 / 220)** and **Building**, keep them hidden inside the app
(never in the phone Gallery), and upload them to **Google Drive** when you press **SYNC**.

## How it works

1. Open the app → choose **Project 219** or **Project 220**.
2. Tap a **building** → the camera opens → take as many photos as you need → **‹ Done**.
3. Photos wait inside the app (private storage — not in Gallery / Google Photos).
   The counter on each building shows how many are waiting. *Review / delete* lets you remove bad shots.
4. Press **SYNC TO GOOGLE DRIVE**. First time only, choose your Google account and allow access.
5. Each photo is uploaded, then deleted from the phone. If the internet drops, nothing is lost —
   the remaining photos stay in the app; press Sync again.

### Google Drive folder structure (created automatically)

```
My Drive/
└── Site Photos - Projects 219 & 220/
    ├── 219 - Address Hotel & SA/
    │   ├── H01/
    │   │   └── 2026-11-25/
    │   │       ├── 219_H01_20261125_143005_123.jpg
    │   │       └── ...
    │   └── H02, R03, R04/
    └── 220 - Ritz Hotel & BR/
        └── H04, H05, R05, R09/
```

The app uses the limited `drive.file` permission: it can only see/create its own folder and photos,
not the rest of your Drive.

## Security (PIN + fingerprint)

- First launch: create a 4-digit PIN, then choose whether to unlock with fingerprint.
- The app asks for the PIN / fingerprint every time it is opened, and again after 1 minute in the background.
- 5 wrong PINs → 30 s wait (doubles with further wrong tries).
- Change PIN or turn fingerprint on/off: **🔒 Security** on the home screen.
- Forgot PIN: phone Settings › Apps › Site Photos › Storage › Clear storage
  (also deletes photos not synced yet – sync first).

## Change project / building names

Edit **`app/src/main/java/com/sitephotos/app/AppConfig.kt`** — the list of projects, buildings,
and the Drive folder names. Rebuild the APK afterwards.

---

## One-time setup: allow the app to use Google Drive (≈10 min)

Google requires every app that uploads to Drive to be registered once in Google Cloud.
Do this with the Google account that should own the photos.

1. Go to <https://console.cloud.google.com/> → create a project (e.g. `Site Photos 219-220`).
2. **APIs & Services → Library** → search **Google Drive API** → **Enable**.
3. **APIs & Services → OAuth consent screen** (Google Auth Platform → Branding/Audience):
   - App name: `Site Photos 219-220`, your email as support & developer contact.
   - Audience: **External** (or **Internal** if you use a Google Workspace company account).
   - Data access / scopes: add `.../auth/drive.file`.
   - If External and left in *Testing*: add every user's Gmail under **Test users**
     (or press **Publish app** — `drive.file` does not need Google verification).
4. **APIs & Services → Credentials → Create credentials → OAuth client ID**:
   - Application type: **Android**
   - Package name: `com.sitephotos.p219220`
   - SHA-1 certificate fingerprint:
     ```
     22:9B:09:A3:1D:18:D8:4B:47:FF:88:72:C5:84:8A:17:8A:FF:26:45
     ```
   - Create. (No file needs to be downloaded or added to the app.)

That fingerprint belongs to the signing key included in `app/keystore/sitephotos.jks`,
which is used for every build — so the APK works no matter which computer builds it.
**Keep this project/repository private**, since the key is inside it.

## Build the APK

**Option A — Android Studio (simplest)**
1. Install Android Studio, *File → Open* this folder, wait for Gradle sync.
2. *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
3. APK: `app/build/outputs/apk/debug/app-debug.apk` → copy to the phone and install
   (allow "install unknown apps").

**Option B — GitHub (no software to install)**
1. Create a **private** GitHub repository and upload this whole folder.
2. GitHub builds automatically (*Actions* tab → *Build APK*).
3. Open the finished run → download **SitePhotos-219-220-apk** → unzip → install the APK on the phone.

Requirements: Android 7.0 or newer, Google Play services, a Google account on the phone.

## Notes
- Uninstalling the app or *Clear storage* deletes photos that were not synced yet — Sync first.
- Photos keep their date folder from when they were taken, even if synced days later.
- Landscape shots are stored with correct orientation.
