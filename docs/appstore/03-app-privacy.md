# 3. App Privacy (App Store Connect → your app → App Privacy)

This label is built from the code, not guessed:
- **SDKs:** Firebase iOS SDK 13.1. The products are `FirebaseAnalytics` (without the ad-ID add-on) and `FirebaseCrashlytics`.
- **Shared layer:** `shared/.../platform/Telemetry.kt`
- **Swift backend:** `iosApp/iosApp/FirebaseTelemetry.swift`
- **Privacy manifest:** `iosApp/iosApp/PrivacyInfo.xcprivacy`, which declares the same data types.

## How the app collects data

| What | Default | Player control |
|---|---|---|
| **Crash reports** (Crashlytics) | **On** | Profile → Privacy → "Share crash reports" |
| **Gameplay stats** (Analytics) | **Off** until the player taps **Allow** on the one-time card after their first game | The card's "Not now" button, or Profile → Privacy → "Share gameplay stats" |
| Ads, ad ID (IDFA), IDFV | Never: AdSupport isn't linked, IDFV collection is off in Info.plist, and ad consent is always denied | — |
| Account, name, email, contacts, photos, precise location | Never; there's no sign-in | — |

Info.plist starts **both** collectors switched off (`FIREBASE_ANALYTICS_COLLECTION_ENABLED` and `FirebaseCrashlyticsCollectionEnabled` set to false). The app turns each one on at launch only if the player's saved choice allows it.

## 3.1 Answers in App Store Connect

**Do you or your third-party partners collect data from this app?** → **Yes, we collect data from this app**

Select these data types:

| Category | Data type | Collected | Linked to the user? | Used for tracking? | Purposes |
|---|---|---|---|---|---|
| Diagnostics | **Crash Data** | Yes | **No** | **No** | App Functionality |
| Diagnostics | **Performance Data** | Yes | No | No | App Functionality |
| Diagnostics | **Other Diagnostic Data** | Yes | No | No | App Functionality |
| Usage Data | **Product Interaction** | Yes | No | No | Analytics |
| Identifiers | **Device ID** | Yes | No | No | Analytics, App Functionality |
| Location | **Coarse Location** | Yes | No | No | Analytics |

Everything else is **not collected**:
- Contact info, health and fitness, financial info, precise location, sensitive info
- Contacts, user content, browsing or search history
- User ID, purchases
- Advertising data, other usage data

**Why each one:**
- **Crash Data, Other Diagnostic Data:** Crashlytics sends stack traces, device and OS model, and the short breadcrumbs the app adds ("game_start classic").
- **Performance Data:** Crashlytics' session data (app launches and sessions, used to count crash-free users).
- **Product Interaction:** the gameplay events (`game_start`, `power_up_used`, `achievement_unlocked`, `daily_challenge_*`, `level_unlocked`, `shooter_level_up`), plus screen views and sessions. Sent only after the player taps Allow.
- **Device ID:** the Firebase app-instance ID and the Crashlytics installation ID. These are random IDs, not IDFA or IDFV. Declaring them is the safe answer, because Apple's definition includes "other device-level ID".
- **Coarse Location:** Google Analytics works out a rough country or region from the masked IP address.
- **Not linked:** there's no sign-in, user ID or account, and the app never sets a Firebase user ID. Keep it that way, or change these answers.
- **Not tracking:** nothing is combined with other companies' data for ads, and nothing is shared with data brokers.

Sources: [App privacy details](https://developer.apple.com/app-store/app-privacy-details/) · [Firebase: App Store data collection](https://firebase.google.com/docs/ios/app-store-data-collection) · [Google Analytics: Apple data disclosure](https://support.google.com/analytics/answer/10285841)

## 3.2 App Tracking Transparency

**Not needed.** The app doesn't track, as Apple defines it: no IDFA, no AdSupport, no ad networks.
- Don't add `NSUserTrackingUsageDescription`. Showing the ATT prompt without tracking confuses players and reviewers.
- If you ever add ads, AdSupport or `FirebaseAnalyticsIdentitySupport`, you'll need ATT and must change the Tracking answers above.

## 3.3 Privacy manifest and required-reason APIs

`iosApp/iosApp/PrivacyInfo.xcprivacy` is in the build:

| Key | Value |
|---|---|
| NSPrivacyTracking | false |
| NSPrivacyTrackingDomains | none |
| NSPrivacyCollectedDataTypes | the 6 types above, all not linked and not tracking |
| File timestamp APIs (`stat`/`fstat`, from the Kotlin/Compose runtime) | `C617.1`, `0A2A.1` |
| UserDefaults (saved game, scores, settings) | `CA92.1` |

Firebase ships its own privacy manifests inside its packages, so you don't need to add anything for Firebase's required-reason APIs.

**How to re-check after adding an SDK:**
1. Product → Archive.
2. In the Organizer, right-click the archive → **Generate Privacy Report**.
3. Compare the PDF with the table above.

Source: [Privacy manifest files](https://developer.apple.com/documentation/bundleresources/privacy-manifest-files)

## 3.4 OAuth / CASA

Not applicable: there's no Google or Microsoft sign-in.
