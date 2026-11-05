# 4. Pricing & Availability, and 5. In-App Purchases

## 4.1 Pricing and Availability (ASC → your app → Pricing and Availability)

| ASC field | Value | Why |
|---|---|---|
| Price | **Free** (USD 0.00, base country United States) | Matches Android. The game has no IAP, so a price would only reduce installs. |
| Availability | **All countries or regions** | It's English-only, but a puzzle game needs little text. To skip the EU for now, remove the EU countries here, but you still have to declare DSA trader status. |
| Pre-orders | **No** | Not useful for a 1.0 launch; it delays the first reviews. |
| Tax category | App Store software (default) | |
| Distribution | Public on the App Store | |
| Apple silicon Mac / Vision Pro availability | **Turn off "Make this app available"** on Mac and Vision Pro | The touch-only layout hasn't been tested there; avoid reviews from untested devices. |

When the app goes live depends on the release option you choose in section 9. The recommendation there is manual release after approval.

## 5. In-App Purchases and subscriptions

**None in version 1.0.** Coins are earned only by playing (merges, medals, the daily challenge), and power-ups are bought only with those coins. No real money is involved anywhere.

So for this version:
- no subscription group
- no products
- no Paid Apps Agreement
- no StoreKit configuration, restore button or paywall links (Guideline 3.1.2)

**Check:** search the code for `StoreKit` and `SKProduct`. The search should find nothing in `iosApp/` or `shared/`.

**If you add coin packs or "remove ads" later:**
1. Sign the Paid Apps Agreement, then add banking and tax (section 1).
2. Create consumable IAPs in ASC → Monetization → In-App Purchases. Each needs:
   - a reference name
   - a product ID like `com.avfusionapps.game2048.coins_500`
   - a price
   - a display name (30 characters) and description (45 characters)
   - a review screenshot
3. Submit the first IAPs **together with an app version**: on each IAP, use "Add for Review" and add it to the version's draft submission.
4. Update the privacy answers (Purchases), the review notes and the age rating (Advertising).
