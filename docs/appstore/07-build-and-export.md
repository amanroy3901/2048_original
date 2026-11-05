# 7. Build and export compliance

## 7.1 Version and build numbers

| Setting (Xcode → target iosApp → General) | Value now | Rule |
|---|---|---|
| Version (`MARKETING_VERSION`) | `1.0.0` | What players see. Raise it for each App Store release (1.0.1, 1.1.0). |
| Build (`CURRENT_PROJECT_VERSION`) | `1` | Must **increase with every upload** for the same version (1, 2, 3, …), including uploads rejected by processing. |

The iOS version is separate from Android (Android is 4.0.0 / versionCode 10). iOS starts at 1.0.0 because it's a new App Store listing.

## 7.2 Requirements that apply right now

- **Xcode 26 / iOS 26 SDK** has been required for uploads since Apr 28, 2026. This Mac has **Xcode 26.3** ✓.
- **Minimum iOS:** the app targets iOS 16.0. Apple's floor is iOS 13 from Sep 9, 2026, and Firebase 13 needs iOS 15 ✓.

Source: [Upcoming requirements](https://developer.apple.com/news/upcoming-requirements/)

## 7.3 One-time signing setup (you do this in Xcode)

1. Xcode → Settings → Accounts → **+** → sign in with your Apple Developer account. You type the password in Xcode's own window.
2. Open `iosApp/iosApp.xcodeproj`, then target **iosApp** → **Signing & Capabilities**:
   - **Team:** your developer team. The project has no team set yet, so archiving fails until you pick one.
   - **Automatically manage signing:** on (already set: `CODE_SIGN_STYLE = Automatic`)
   - **Bundle Identifier:** `com.avfusionapps.game2048`. Xcode registers it on first archive if it doesn't exist.
3. Distribution needs **no registered device**. With automatic signing, Xcode makes the Apple Distribution certificate and the App Store profile during upload. A device is only needed to *run* a development build on your own iPhone.

## 7.4 Archive and upload

1. Close Android Studio and other Gradle builds if you can. The first Release build compiles the Kotlin framework (several minutes).
2. In the toolbar, set the destination to **Any iOS Device (arm64)**.
3. **Product → Archive.**
   - The "Compile Kotlin Framework" phase runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` with Android Studio's Java automatically.
   - If Gradle complains about Java 25, Android Studio's bundled JDK 21 is missing. Install Android Studio, or set `JAVA_HOME` to a JDK 17–21.
4. The Organizer opens. Select the archive → **Distribute App** → **App Store Connect** → **Upload** → keep the defaults (upload symbols on, manage version and build number off) → **Upload**.
5. About 10 to 30 minutes later the build appears in ASC → TestFlight, and Apple emails you if processing finds a problem.

**Folder note:** this project lives under `Documents`, which macOS syncs to iCloud. A build output folder *inside* the project failed to sign ("resource fork, Finder information, or similar detritus not allowed"). Xcode's default DerivedData folder (`~/Library/Developer/Xcode/DerivedData`) works. Don't change Xcode's build location to a folder inside the project.

## 7.5 TestFlight before submitting

ASC → TestFlight → **Internal Testing** → add yourself → install the TestFlight app on your iPhone → open the build.

On a real device, check:
- **First launch:** the menu appears and sound plays.
- **Classic:** play one game, go back to the menu, and the consent card appears. Tap Not now: the card never comes back.
- **Profile switches:** both work, and they're still set after you force-quit and reopen the app.
- **Time Attack:** play to time-up; the best score is kept.
- **Shooter:** play a chain.
- **iPad (if you have one):** portrait and landscape.
- **Firebase:** within 24 hours, Crashlytics shows the app, and Analytics → DebugView or Realtime shows events (only on the device where you tapped Allow).

## 7.6 Export compliance

`Info.plist` has `ITSAppUsesNonExemptEncryption = NO`, so ASC skips the encryption questions for each build.

**Why NO is correct:**
- The app's only encryption is standard HTTPS/TLS. Firebase uses it through Apple's networking.
- Using the OS's encryption for HTTPS is exempt.
- The app has no custom cryptography, no VPN and no end-to-end messaging.
- No annual self-classification report or ERN is needed for this case.

If ASC still asks: "Does your app use encryption?" → **Yes**. "Does it qualify for any exemptions?" → **Yes**: it uses only encryption that's exempt (standard HTTPS through the OS).

Source: [Complying with encryption export regulations](https://developer.apple.com/documentation/security/complying-with-encryption-export-regulations)

## 7.7 dSYMs and third-party SDK warnings

- **App dSYM:** Release builds use `DEBUG_INFORMATION_FORMAT = dwarf-with-dsym`. The **Upload Crashlytics dSYMs** build phase sends the app's dSYM to Crashlytics. It runs only when `CONFIGURATION = Release`, and Debug builds skip it.
- **Kotlin framework:** the shared framework is static and linked into the app binary, so its symbols are in the app's dSYM.
- **"Upload Symbols Failed" warnings for Firebase or Google frameworks:** these are **harmless**. Those SDKs ship as prebuilt binaries without dSYMs for your build. Your own crashes still get symbolicated. Keep "Upload your app's symbols" checked when distributing.

## 7.8 Debug-only scripts in Release

| Build phase | In Release? | Note |
|---|---|---|
| Compile Kotlin Framework | Yes (needed) | Builds the release Kotlin framework |
| Upload Crashlytics dSYMs | Yes (Release only) | Does nothing in Debug |
| `-route` launch argument (`iOSApp.swift`) | Present but inert | It's read only when the app is launched with `-route …` (the screenshot tool), which never happens in a store install. Safe to keep. |

There are no debug menus or test endpoints in the code.
