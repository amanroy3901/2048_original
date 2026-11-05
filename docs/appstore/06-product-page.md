# 6. Product page (ASC → your app → iOS App → 1.0 Prepare for Submission)

Locale: **English (U.S.)**. Every block below is ready to paste.

## 6.1 Promotional text (170 max). You can change it anytime without review.

```
Slide twins together and watch them evolve! A new daily challenge every day, 24 medals to earn and three ways to play: Classic, Time Attack and Shooter.
```
(150 characters)

## 6.2 Description (4000 max). Pick one variant.

### Variant A: character-led (recommended)

```
Your 2048 tiles have come alive!

Every number is a little elemental sprite that grows up as you merge. Drip the water drop becomes Sprig, Sunny, Frosty, Orbi and Blaze, and on up to the legendary Ember King at 2048. Slide twins together and watch them evolve with a flash, a swirl and a shower of sparkles.

THE SPRITES ARE ALIVE
• They blink, breathe and look at their twin when a merge is close
• Poke one and it giggles (poke too often and it gets grumpy)
• Leave them alone and they start thinking, then doze off
• Get stuck and they worry; bump a wall and everyone gets dizzy

THREE WAYS TO PLAY
• Classic: the 2048 you love, on 3×3, 4×4, 5×5 or 6×6 boards
• Time Attack: one minute on the clock, and big merges buy more time
• Shooter: launch sprites up five lanes and chain the merges

SOMETHING NEW EVERY DAY
• A daily challenge with coins and a streak bonus
• 24 medals to earn across every mode
• Collect all 11 sprites
• Power-ups: Undo, Hammer, Shuffle, Freeze and Hint, paid with coins you earn by playing

FAIR AND FRIENDLY
• Free, with no ads and no in-app purchases
• Plays fully offline
• No account needed; your progress stays on your device
• Made for iPhone and iPad
```

### Variant B: search-led (more of the words people search for)

```
A cute merge puzzle that's easy to learn and hard to put down.

Swipe to slide the tiles. When two of the same sprite touch, they merge into the next element. Keep merging to reach 2048 and the Ember King.

WHY YOU'LL LOVE IT
• Classic number puzzle play with adorable living sprites
• Relaxing or fast: play Classic at your own pace, or race the clock in Time Attack
• Shooter mode: tap a lane, launch a sprite and chain merges for big combos
• Board sizes from 3×3 to 6×6
• Undo, Hammer, Shuffle, Freeze and Hint when you're stuck

COME BACK EVERY DAY
• Fresh daily challenge and streak rewards
• 24 achievements and 11 sprites to collect
• Beat your best score in every mode

NO STRESS
• Free with no ads and no purchases
• Works offline, no sign-up
• Great on iPhone and iPad
```

**ASO reasoning:**
- **Variant A** sells the one thing competitors don't have, which is living characters. It converts better from the product page, because the screenshots already prove the claim.
- **Variant B** repeats the words people type: "merge puzzle", "number puzzle", "offline", "relaxing", "brain".
- The description isn't indexed for search on iOS (the keyword field is), so pick for conversion: **A**.
- Competitors in the 2048 and merge space mostly show plain numbered squares and full-screen ads. "No ads, no purchases, offline" is a real difference, so keep it in the first screen of text and in the promo text.

## 6.3 Keywords (100 max, comma-separated, no spaces)

Don't repeat words already in the name (2048, Elemental, Sprites) or the category (puzzle, games). Apple already indexes those.

**Set A (recommended), 96 characters:**
```
merge,number,tiles,brain,logic,cute,offline,casual,swipe,slide,math,daily,challenge,evolve,combo
```

**Set B (more phrases), 94 characters:**
```
merge game,number puzzle,brain teaser,tile,cute,offline,casual,swipe,logic,kawaii,relax,family
```
Set B spends characters on phrase pairs and repeats "puzzle", which Apple already matches from the category. Start with A. After 4 weeks, check App Analytics → Sources → App Store Search and swap the weakest words.

## 6.4 URLs and copyright

| ASC field | Value |
|---|---|
| Support URL (required) | `https://amanroy3901.github.io/privacy/support_elemental_sprites.html` (upload `web/support_elemental_sprites.html` first) |
| Marketing URL (optional) | leave blank |
| Copyright | `2026 Sayne Design` (or your legal name if you publish as an individual) |
| Version | `1.0.0` (matches the build) |
| What's New | not shown for 1.0 |

## 6.5 Screenshots (`screenshots/` in this folder)

All are PNG with no alpha, captured from the real app on the simulator with the status bar set to 9:41.

| Slot (ASC label) | Required? | Size | Files |
|---|---|---|---|
| iPhone 6.3" (Dynamic Island medium) | **Yes** | 1206 × 2622 | `iphone-6.3/1-home.png`, `2-classic.png`, `3-time-attack.png`, `4-shooter.png`, `5-medals.png` |
| iPhone 6.9" (Dynamic Island large) | No. ASC scales down from the largest set you upload, so 6.3" alone is accepted. | 1320 × 2868 | not made |
| iPad 13" | **Yes**, because the app runs on iPad | 2064 × 2752 | `ipad-13/1-home.png`, `2-classic.png`, `3-medals.png` |

Limits: 1 to 10 per size; .png, .jpg or .jpeg; no transparency.

**Upload order (first 3 show in search results):**
1. **1-home:** the living sprites and the three modes (hero shot)
2. **2-classic:** a full board from Drip (2) to Prism (1024)
3. **3-time-attack:** the clock and fast play
4. **4-shooter:** the lane launcher
5. **5-medals:** 24 medals (shows depth and gives a reason to come back)

**Planned captions** if you later make framed marketing screenshots (8 frames: hero → features → trust):
1. "Your 2048 tiles are alive!"
2. "Merge twins. Watch them evolve."
3. "Beat the clock in Time Attack"
4. "Launch & chain in Shooter mode"
5. "A new challenge every day"
6. "24 medals to earn"
7. "Collect all 11 sprites"
8. "No ads. No purchases. Offline."

The raw screenshots are valid as they are. Framed ones usually convert better but are optional.

Source: [Screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications)

## 6.6 App previews (video)

**Optional.** Up to 3 per size, 15 to 30 seconds, .mov, .m4v or .mp4, recorded from the app. A good one for this game: 3 Classic merges up to Blaze, then a Time Attack burst, then a Shooter chain. Skip it for 1.0 and add it later if conversion is low.

## 6.7 Newer creative slots

Custom product pages and in-app events take their own artwork. The large header and search-result art that ASC sometimes shows is part of App Store **featuring nominations** and Today stories; Apple asks for it only when you're featured.

None of these is needed to submit. Treat any specific size as unverified until ASC asks for it on your account.

## 6.8 App icon

`app-icon-1024.png` in this folder is the same file as the build's `Assets.xcassets/AppIcon.appiconset/icon-1024.png`:
- 1024 × 1024
- RGB, no alpha (checked)
- square corners; iOS rounds them

Since Xcode 14, the store icon comes from the build itself. There's no separate upload.
