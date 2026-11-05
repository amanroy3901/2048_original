# 2. App Information (App Store Connect → your app → App Information)

## 2.1 Create the app (My Apps → + → New App)

| ASC field | Value |
|---|---|
| Platforms | iOS |
| Name | `2048 Elemental Sprites` |
| Primary language | English (U.S.) |
| Bundle ID | `com.avfusionapps.game2048` (register it first in Certificates, Identifiers & Profiles → Identifiers) |
| SKU | `elemental-sprites-ios-001` (private and never shown; any unique text works) |
| User access | Full Access |

## 2.2 Name, subtitle and categories

| ASC field | Value | Limit |
|---|---|---|
| Name | `2048 Elemental Sprites` | 22/30 |
| Subtitle | `Merge cute sprites up to 2048` | 29/30 |
| Primary category | **Games** → subcategories **Puzzle** and **Casual** | |
| Secondary category | **Entertainment** (optional; leave blank if unsure) | |

**Why:**
- Searches for 2048 and merge games happen under Games → Puzzle, and every competitor sits there.
- Casual is the second subcategory and widens the browse placement.
- The Info.plist already sets `public.app-category.puzzle-games` to match.

## 2.3 Content rights

**Does your app contain, show, or access third-party content?** → **No**

- All art, sprites and sounds were made for this game.
- The two fonts (Baloo 2 and Lilita One) are SIL Open Font License fonts that you're allowed to bundle. Licensed fonts don't make this a "Yes".
- The app shows no web or user content.

**Check before answering:** confirm the sounds in `shared/src/commonMain/composeResources/files/` are your own or licensed for commercial use. If any came from a stock site, keep the license file.

## 2.4 Age rating (App Information → Age Rating → Edit)

This uses the new questionnaire. Apple updated every app to it on Jan 31, 2026.

| Section | Question | Answer |
|---|---|---|
| In-App Controls | Parental controls | No |
| | Age assurance | No |
| Capabilities | Unrestricted web access | No |
| | User-generated content | No |
| | Social media | No |
| | Messaging and chat | No |
| | Advertising | No |
| Mature Themes | Profanity or crude humor | None |
| | Horror/fear themes | None |
| | Alcohol, tobacco or drug use or references | None |
| Medical or Wellness | Medical or treatment information | None |
| | Health or wellness topics | No |
| Sexuality or Nudity | Mature or suggestive themes | None |
| | Sexual content or nudity | None |
| | Graphic sexual content and nudity | None |
| Violence | Cartoon or fantasy violence | None (a hammer cracks a rock tile and sprites get "dizzy"; nobody is hurt) |
| | Realistic violence | None |
| | Prolonged graphic or sadistic realistic violence | None |
| | Guns or other weapons | None (the "Shooter" mode launches sprites up lanes; there are no weapons) |
| Chance-Based Activities | Gambling | No |
| | Simulated gambling | None |
| | Contests | None |
| | Loot boxes | No (the level-up chest gives a random power-up, but nothing is bought with money) |

**Result: 4+**

- **Made for Kids:** answer **No**. The Kids category has extra rules (no third-party analytics without a careful review), and the app is a general-audience puzzle game.

Source: [Age ratings values and definitions](https://developer.apple.com/help/app-store-connect/reference/app-information/age-ratings-values-and-definitions)

## 2.5 Privacy Policy URL

- ASC field **Privacy Policy URL** (App Privacy page): `https://amanroy3901.github.io/privacy/privacy_elemental_sprites_ios.html`
  - Upload `web/privacy_elemental_sprites_ios.html` from this folder to your `amanroy3901.github.io` repo under `privacy/`. The link above works only after that.
- The policy must mention:
  - Firebase Crashlytics (crash reports, on by default, can be turned off)
  - Google Analytics for Firebase (gameplay stats, only after the player taps Allow)
  - what's stored on the device, with no account or sign-in
  - no ads and no tracking
  - how to change the choices in Profile
  - children
  - contact email
  - date

  The draft covers all of these.
- **Don't reuse the Android policy URL.** That page describes Google Sign-In and cloud saves, which the iOS app doesn't have. It also doesn't mention Firebase Analytics or Crashlytics, which the Android app uses too, so it needs updating as well (see the README).

## 2.6 Account deletion (Guideline 5.1.1(v))

**Not applicable.** The iOS app has no sign-in and no accounts. Everything is stored on the device and deleted with the app.

## 2.7 Sign in with Apple (Guideline 4.8)

**Not needed.** It's required only when an app offers a third-party login such as Google. The iOS app has no login at all.

**If you add Google sign-in later,** you must also add Sign in with Apple (or another login that meets 4.8) and in-app account deletion.

Source: [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) (4.8, 5.1.1)
