# Google Play release 4.0.0: 2048 Elemental Sprites

Package `com.avfusionapps.game_2048` · versionCode 10 · versionName 4.0.0 · min SDK 27 (Android 8.1) · target SDK 36

Everything for the Play Console is in this folder:

| File | Play Console field | Spec |
|---|---|---|
| `icon-512.png` | App icon | 512 × 512 PNG |
| `feature-1024x500.png` | Feature graphic | 1024 × 500 PNG |
| `phone/phone-1 … 8` | Phone screenshots | 8 × 1080 × 1920 (9:16) |
| `tablet/tablet-1 … 4` | 7-inch **and** 10-inch tablet screenshots (upload the same 4 to both) | 4 × 1920 × 1080 (16:9) |
| `app-release.aab` (after signing, see step 1 below) | App bundle | |

The 9:16 phone shots and 16:9 tablet shots, at 1080 px or more, meet Google's size rules for featuring an app. Upload phone shots in the numbered order: the first 3 are what most people see without scrolling.

---

## Main store listing  (Grow → Store presence → Main store listing)

### App name  (30 max)

```
2048 Elemental Sprites: Merge
```
29 characters. Google ranks the title most heavily, so "Merge" is in it as a keyword. If you'd rather it match the home-screen name exactly, use `2048 Elemental Sprites` (22).

### Short description  (80 max)

```
Cute 2048 merge puzzle: grow sprites from Drip to the Ember King. Play offline!
```
79 characters.

### Full description  (4000 max)

```
Your 2048 tiles have come alive!

2048 Elemental Sprites is the classic 2048 number puzzle with a cute twist: every tile is a little sprite that grows up as you merge. Slide twins together and watch tiny Drip the water drop evolve into Sprig, Sunny, Frosty, Orbi, Blaze and on up to the legendary Ember King at 2048.

Formerly 2048 Neon Rush. Updating? Your best score and saved game are kept.

THE SPRITES ARE ALIVE
• They blink, breathe and look at their twin when a merge is close
• Poke one and it giggles (poke too often and it gets grumpy)
• Leave them alone and they start thinking, then fall asleep
• Get stuck and they worry; bump a wall and everyone goes dizzy
• Every new sprite you make gets its own celebration

THREE WAYS TO PLAY
• Classic: the 2048 merge puzzle you love, on 3×3, 4×4, 5×5 or 6×6 boards
• Time Attack: 60 seconds on the clock, and big merges buy you more time
• Shooter: launch sprites up five lanes and chain merges for huge combos

SOMETHING NEW EVERY DAY
• A daily challenge with coin rewards and a streak bonus
• 24 achievement medals to earn across all modes
• Collect all 11 elemental sprites in your collection book
• Level up as you play and earn coins

POWER-UPS FOR TRICKY BOARDS
• Undo your last move
• Hammer away one sprite
• Shuffle the board
• Freeze new tiles for a few moves
• Hint shows your best next swipe

EASY TO LEARN, HARD TO PUT DOWN
Swipe in any direction to slide every sprite. When two twins touch, they merge into the next element. Plan ahead, keep your biggest sprite in a corner and see how far you can grow. It's a relaxing brain game for quick breaks or long sessions.

• Plays offline: no internet needed to play
• Sign in with Google (optional) to keep your scores, coins, medals and collection safe across devices
• Works on phones and tablets, in portrait and landscape
• Small download

Ready to grow your first Ember King? Swipe, merge and watch your sprites evolve!
```

### What's new / release notes  (500 max, Release → Production → Create release)

```
2048 Neon Rush is now 2048 Elemental Sprites! Every tile is a living sprite that evolves as you merge, from little Drip to the Ember King. New: a fresh home screen, daily challenges with streak rewards, 24 achievements, a sprite collection, power-ups (Undo, Hammer, Shuffle, Freeze, Hint), new sounds, and a new look for Classic, Time Attack and Shooter. The app is now smaller and faster. Your best score and saved game are kept.
```

### Graphics

- App icon: `icon-512.png`
- Feature graphic: `feature-1024x500.png`
- Phone screenshots: `phone/` in order 1 → 8
- 7-inch tablet screenshots: `tablet/` 1 → 4
- 10-inch tablet screenshots: `tablet/` 1 → 4
- Video (optional): leave empty, or add a 30-second YouTube gameplay clip later. Google shows it before the screenshots.

---

## Store settings  (Grow → Store presence → Store settings)

| Field | Value |
|---|---|
| App or game | Game |
| Category | **Puzzle** |
| Tags (up to 5, choose from Google's list) | Puzzle, Merge, Casual, Offline, Number / Brain games (whichever of these appear in the picker) |
| Email | *your support email* |
| Website | `https://amanroy3901.github.io/` (optional) |
| Privacy policy | `https://amanroy3901.github.io/privacy/privacy_neon_rush_2048.html` (this is the link inside the app; see "Check before publishing") |

---

## App content  (Policy → App content)

These match what the app actually contains (checked in the code on 5 Nov 2026).

**Ads:** No, the app contains no ads. There is no ads SDK.

**In-app purchases:** none. Coins are earned only by playing.

**Advertising ID** (the app declares `com.google.android.gms.permission.AD_ID` through Firebase Analytics):
Yes, the app uses the advertising ID, for **Analytics** only.

**Target audience and content:** **13 and over** (13–15, 16–17, 18+).
The game is cute, so the form will ask whether it "could unintentionally appeal to children". Answer honestly. Choosing an under-13 age group would bring in Google's Families policy, and Firebase Analytics with the advertising ID isn't allowed there unless analytics is reconfigured first.

**Content rating (IARC questionnaire):** category *Game / Puzzle*. Answer **No** to violence, fear, sexual content, gambling, drugs, crude language and controlled substances. Users can't talk to each other or share content: Google sign-in only saves progress. Location isn't shared, and there are no purchases of digital goods.
Expected result: **Everyone / PEGI 3**.

**Data safety:**

| Data type | Collected | Shared | Purpose | Optional? |
|---|---|---|---|---|
| Personal info → Name | Yes | No | Account management | Yes (only if they sign in with Google) |
| Personal info → Email address | Yes | No | Account management | Yes (only if they sign in with Google) |
| Personal info → User IDs | Yes | No | App functionality, account management | Yes |
| App activity → In-app actions (analytics events) | Yes | No | Analytics | No |
| App activity → Other actions (game progress, scores, medals, synced to Firestore) | Yes | No | App functionality | Yes (synced only when signed in) |
| App info and performance → Crash logs | Yes | No | Analytics | No |
| App info and performance → Diagnostics | Yes | No | Analytics | No |
| Device or other IDs | Yes | No | Analytics | No |

Also answer:
- Is the data encrypted in transit? **Yes**, Firebase uses HTTPS.
- Can users request deletion? **Yes.** Play requires a web link for this because sign-in creates an account (see below).

**Account deletion URL:** required because Google sign-in creates an account. Add a page, for example on your GitHub Pages site, that explains how to request deletion (by email or in the app). Then enter its URL here.

**Government apps, financial features, health, news, COVID:** No / not applicable.

---

## Release steps

1. **Sign the bundle with your Play upload key.** The bundle built here is unsigned, and Play won't accept an unsigned bundle.
   - Copy `keystore.properties.example` (project root) to `keystore.properties` and fill in your upload keystore path, alias and passwords. Git ignores that file.
   - Then run:
     ```
     ./gradlew :app:bundleRelease
     ```
     Output: `app/build/outputs/bundle/release/app-release.aab`
   - Or, in Android Studio: **Build → Generate Signed App Bundle**.
2. **Version:** versionCode **10** / versionName **4.0.0**. If Play already has versionCode 10 or higher, raise `versionCode` in `app/build.gradle.kts` first.
3. **Deploy the Firestore rules.** Without them, cloud sync of medals and collection does nothing, though the game itself still works:
   ```
   firebase deploy --only firestore:rules --project original-64f6c
   ```
4. **Check that the Play app-signing SHA-1 is in Firebase** (Play Console → Setup → App signing). If it's missing, Google sign-in fails in the Play build.
5. Update the main store listing, store settings and app content using the values above.
6. **Release → Production → Create release.** Upload the signed `.aab`, paste the release notes, and start a **staged rollout of about 10%**.
7. Watch Crashlytics and Android vitals for 2–3 days, then roll out to 100%.

## Check before publishing

- [ ] The privacy policy page is still named "Neon Rush" (`privacy_neon_rush_2048.html`). Update its text to say 2048 Elemental Sprites. It also has to mention Google sign-in (name, email), Firebase Analytics, Crashlytics and the advertising ID.
- [ ] The account deletion web page exists.
- [ ] You've tested the signed bundle on a real phone (internal testing track): cold start, Google sign-in, a long Classic game, Shooter, and Time Attack to time-up.
- [ ] ASO, after launch: compare the two titles (with and without ": Merge") using a Store listing experiment (Grow → Store listing experiments).
