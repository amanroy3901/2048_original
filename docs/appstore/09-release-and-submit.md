# 9. Version release and submit

## 9.1 Release option (version page → Version Release)

| Option | Recommendation |
|---|---|
| **Manually release this version** | **Use this for 1.0.** After approval you pick the moment: check the live listing, the crash dashboard and the Firebase events first, then tap Release. |
| Automatically release after approval | Fine for later bug-fix updates |
| Automatically release after a date | For a planned launch day |

## 9.2 Phased release for automatic updates

**Not used for 1.0.** A first version has no existing users to phase. Turn it **on** for 1.0.1 and later: it rolls out over 7 days to users with auto-update, and you can pause it if Crashlytics shows a problem.

## 9.3 Final pre-submit checklist

Every row must be green in ASC before **Add for Review** is enabled:

- [ ] Business: DSA trader status declared (section 1.4)
- [ ] App Information: name, subtitle, categories, content rights, age rating 4+ (section 2)
- [ ] App Privacy: privacy policy URL live, data types answered and **published** (section 3)
- [ ] Pricing and Availability: Free, countries chosen (section 4)
- [ ] Version page:
  - [ ] promo text, description, keywords, support URL, copyright (section 6)
  - [ ] screenshots: iPhone 6.3" ×5, iPad 13" ×3 (section 6.5)
  - [ ] build 1.0.0 (1) selected; export compliance answered or auto-filled from Info.plist (section 7)
  - [ ] App Review Information: contact and notes (section 8)
  - [ ] Version Release: Manual (section 9.1)
- [ ] Tested on a real iPhone through **TestFlight**: one Classic game, the consent card, both Profile switches, Time Attack to time-up, a Shooter chain (section 7.5)

Then: **Add for Review** → **Submit to App Review** (it goes in as a draft submission first).

## 9.4 Timeline

- **Review:** Apple says most submissions are reviewed within 24 to 48 hours, often less. A first app from a new account can take a few days.
- **"Waiting for Review" → "In Review":** usually hours.
- **After approval with manual release:** the "Release" button appears, and the app is live in all regions within about 24 hours of tapping it.

## 9.5 If App Review rejects it (Resolution Center)

1. Read the exact guideline number in the message.
2. If it's a misunderstanding, **reply in Resolution Center** (no new build) with clear steps or a short screen recording. Be brief and factual.
3. If it's a real issue, fix it, increase the **build** number (keep version 1.0.0), upload, then select the new build on the same version and resubmit.
4. If you disagree after replying, use the [App Review Board appeal](https://developer.apple.com/contact/app-store/?topic=appeal).

## 9.6 Top 10 rejection risks for this app, and what's already done

| # | Guideline | Risk | Status / fix |
|---|---|---|---|
| 1 | 4.1 Copycats | The "2048" name and genre are common, and the reviewer may flag a clone | The art, sprites and modes are original. Keep "2048" as a genre word only; never use another game's name or look. |
| 2 | 2.3.8 Metadata | Screenshots must show the real app | They are raw simulator captures of the real app. Don't add device frames that hide UI, or features the app lacks. |
| 3 | 5.1.1 Privacy | Collecting data without consent, or a label that doesn't match | Stats need opt-in, crash reports can be switched off, and the label matches the manifest (section 3). |
| 4 | 5.1.2 / ATT | Using a device ID for tracking without ATT | No IDFA and no tracking. The label says "not used for tracking". |
| 5 | 2.1 Performance | A crash on review devices (iPad) | Test the iPad layout in TestFlight. Crashlytics is installed. |
| 6 | 4.0 Design / iPad | An iPhone-only layout stretched on iPad | Portrait iPad layouts are checked (see the iPad screenshots). Landscape on iPad is allowed in Info.plist but **not tested yet**: check it in TestFlight, or limit iPad to portrait. |
| 7 | 2.3.10 | Mentioning Android or "Google Play" in metadata | The texts in section 6 don't. Keep it that way. |
| 8 | 5.1.1(i) | Privacy policy URL broken or not about this app | Upload `web/privacy_elemental_sprites_ios.html` before submitting and open the link once. |
| 9 | 3.1.1 | Something that looks like buying coins | There's no store screen and coins can't be bought. The review notes say so. |
| 10 | 2.1 | Incomplete info, such as a missing support URL or contact | Upload `web/support_elemental_sprites.html` and fill in section 8. |

Sources: [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) · [App Review](https://developer.apple.com/distribute/app-review/)
