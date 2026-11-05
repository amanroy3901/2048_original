# App Store submission kit: 2048 Elemental Sprites (iOS 1.0.0)

Everything needed to go from the Xcode project to **Submitted for Review**. It follows the 10 deliverables of the App Store submission prompt. Apple requirements were checked against Apple's own pages on **Nov 5, 2026**, and each file cites its sources.

| File | Covers |
|---|---|
| `01-business-setup.md` | Agreements, bank, US tax (W-8BEN), other tax forms, EU DSA trader status, contacts |
| `02-app-information.md` | Name, subtitle, categories, content rights, age rating (every question), privacy URL, account deletion, Sign in with Apple |
| `03-app-privacy.md` | Privacy label for every data type, ATT, privacy manifest, required-reason APIs |
| `04-pricing-and-purchases.md` | Price, countries, pre-order, in-app purchases (none) |
| `06-product-page.md` | Promo text, 2 descriptions, 2 keyword sets, URLs, copyright, screenshots and captions, previews, icon |
| `07-build-and-export.md` | Version/build rules, signing, archive and upload, TestFlight, export compliance, dSYMs |
| `08-app-review.md` | Review contact fields and the full review notes |
| `09-release-and-submit.md` | Release option, phased release, final checklist, timeline, rejections and top 10 risks |
| `screenshots/iphone-6.3/` | 5 screenshots, 1206 × 2622 (required iPhone size) |
| `screenshots/ipad-13/` | 3 screenshots, 2064 × 2752 (required, since the app runs on iPad) |
| `app-icon-1024.png` | 1024 × 1024 store icon, no alpha (same file as in the build) |
| `web/privacy_elemental_sprites_ios.html` | Privacy policy, ready to upload to your GitHub Pages site |
| `web/support_elemental_sprites.html` | Support page (the Support URL is required) |

## The app in one line

A free 2048 merge puzzle with living sprites and three modes (Classic, Time Attack, Shooter). It has **no sign-in, no ads and no in-app purchases**, and plays offline. Crash reports are on by default and can be turned off. Gameplay stats are sent only after the player taps Allow.

## Changes since Apple's rules a year ago (last 12 months)

- **Age ratings:** a new questionnaire with 13+, 16+ and 18+ tiers. Answers were due by **Jan 31, 2026**. Our answers give **4+**.
- **Xcode 26 / iOS 26 SDK** required for uploads since **Apr 28, 2026**. This Mac has Xcode 26.3.
- **Minimum deployment target iOS 13** from **Sep 9, 2026**. The app targets 16.0.
- **Screenshots:** the only required iPhone size is now **6.3" (1206 × 2622)**; 6.9" is optional.
- **EU DSA:** apps without a trader status were removed from the EU store from **Feb 17, 2025**, so declare it.

## Master checklist

Who: **You** = you type or click it in App Store Connect or Xcode. **Done** = already in the project or this folder.

| # | Section | ASC field | Value | Who | Status |
|---|---|---|---|---|---|
| 1 | Business | Free Apps agreement | Covered by Developer Program membership | — | ✅ nothing to do |
| 2 | Business | Paid Apps Agreement / bank / tax | **Not needed** (free, no IAP) | You, later | ⏭ skip for 1.0 |
| 3 | Business | DSA trader status | Trader or Non-trader (see 01 §1.4) | You | ⬜ |
| 4 | Business | Contacts | Senior Management + Technical | You | ⬜ |
| 5 | Identifiers | Bundle ID | `com.avfusionapps.game2048` | You (or Xcode on first archive) | ⬜ |
| 6 | New App | Name / SKU / language | `2048 Elemental Sprites` / `elemental-sprites-ios-001` / English (U.S.) | You | ⬜ |
| 7 | App Information | Subtitle | `Merge cute sprites up to 2048` | You (paste) | ✅ text ready |
| 8 | App Information | Category | Games → Puzzle + Casual; secondary Entertainment | You | ✅ decided |
| 9 | App Information | Content rights | No third-party content | You | ✅ decided (check sound licenses) |
| 10 | App Information | Age rating | All "None"/"No" → **4+** | You | ✅ answers ready |
| 11 | App Information | Made for Kids | No | You | ✅ |
| 12 | App Privacy | Privacy Policy URL | `https://amanroy3901.github.io/privacy/privacy_elemental_sprites_ios.html` | You (upload `web/` file first) | ⬜ upload |
| 13 | App Privacy | Data types | Crash, Performance, Other Diagnostic, Product Interaction, Device ID, Coarse Location; all not linked, not tracking | You (enter) → **Publish** | ✅ answers ready |
| 14 | App Privacy | ATT | Not needed | — | ✅ |
| 15 | Build | Privacy manifest | `PrivacyInfo.xcprivacy` | Done | ✅ |
| 16 | Build | Export compliance | `ITSAppUsesNonExemptEncryption = NO` | Done | ✅ |
| 17 | Build | Version / build | 1.0.0 (1) | Done | ✅ |
| 18 | Build | Signing team | Pick your team in Signing & Capabilities | You | ⬜ |
| 19 | Build | Archive + upload | Product → Archive → Distribute → App Store Connect | You | ⬜ |
| 20 | TestFlight | Real-device check | 07 §7.5 list | You | ⬜ |
| 21 | Pricing | Price / availability | Free / all countries / no pre-order / Mac and Vision Pro off | You | ✅ decided |
| 22 | IAP | Products | None | — | ✅ |
| 23 | Version | Promotional text | 06 §6.1 | You (paste) | ✅ text ready |
| 24 | Version | Description | 06 §6.2 Variant A | You (paste) | ✅ text ready |
| 25 | Version | Keywords | 06 §6.3 Set A | You (paste) | ✅ text ready |
| 26 | Version | Support URL | `https://amanroy3901.github.io/privacy/support_elemental_sprites.html` | You (upload `web/` file first) | ⬜ upload |
| 27 | Version | Marketing URL | blank | — | ✅ |
| 28 | Version | Copyright | `2026 Sayne Design` | You (paste) | ✅ text ready |
| 29 | Version | iPhone 6.3" screenshots | `screenshots/iphone-6.3/` 1 → 5 | You (drag in order) | ✅ files ready |
| 30 | Version | iPad 13" screenshots | `screenshots/ipad-13/` 1 → 3 | You (drag in order) | ✅ files ready |
| 31 | Version | App preview video | Optional, skip for 1.0 | — | ⏭ |
| 32 | Version | Build | Select 1.0.0 (1) after processing | You | ⬜ |
| 33 | App Review | Sign-in required | No | You | ✅ |
| 34 | App Review | Contact | Your name, +91 phone, email | You (type) | ⬜ |
| 35 | App Review | Notes | 08 (paste the block) | You (paste) | ✅ text ready |
| 36 | Release | Version release | Manually release | You | ✅ decided |
| 37 | Release | Phased release | Off for 1.0, on from 1.0.1 | — | ✅ |
| 38 | Submit | Add for Review → Submit to App Review | after every row above is green | You | ⬜ |

## Copy-paste blocks

They live next to their field in each file:
- **Name, subtitle:** `02-app-information.md` §2.2
- **Promotional text, description (A and B), keywords (A and B), copyright, URLs:** `06-product-page.md`
- **Review notes:** `08-app-review.md`
- **Privacy answers:** `03-app-privacy.md` §3.1

## Open questions (only what changes an answer)

1. **Seller type:** are you enrolled as an **individual** or as **Sayne Design the company**? This changes:
   - the copyright line (your legal name for an individual)
   - the W-8BEN vs W-8BEN-E form (only once you go paid)
   - the DSA trader details
2. **DSA:** **Trader** (contact details shown publicly in the EU) or **Non-trader** (free personal project)? See 01 §1.4.
3. **Sounds:** confirm the 17 sound effects are your own or licensed for commercial use. That keeps the content-rights answer "No".

## Not done here (needs you)

- Upload the two `web/` pages to the `amanroy3901.github.io` repository and open both links once. Publishing is a public change, so I left it for you.
- Everything in App Store Connect and Xcode signing, which needs your Apple ID.
- Real-iPhone testing through TestFlight.

**Android note:** the Android privacy policy (`privacy_neon_rush_2048.html`) still describes Google Sign-In only. It doesn't mention Firebase Analytics or Crashlytics, which the Android app uses, so update it before the 4.0.0 Play release.
