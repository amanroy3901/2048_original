# 1. Business setup (App Store Connect → Business)

**Short version:** 2048 Elemental Sprites for iOS is **free with no in-app purchases**, so you do **not** need the Paid Apps Agreement, banking or tax forms to publish it. Joining the Apple Developer Program already covers free apps. You **do** need to declare your **EU trader status**, or the app can't be on the EU App Store.

Set up banking and tax only when you add something paid (a paid app, in-app purchases or subscriptions). The steps are below so they're ready when you need them.

Never paste bank numbers, PAN or passwords into a chat. Type them only into App Store Connect.

## 1.1 Agreements (only the Account Holder can do this)

| Step | Where | What to do |
|---|---|---|
| Free apps | Nothing to sign | Covered by the Apple Developer Program License Agreement you accepted when you joined. |
| Paid Apps Agreement | Business → Agreements → Paid Apps → **View and Agree to Terms** | **Only when you add IAP or a price.** Accepting can't be undone. Then do banking, then tax. The agreement shows "Active" only after all three are done. |
| New agreement versions | Business → Agreements | When Apple publishes a new version, you can't create apps or IAPs until you accept it. Check this page if ASC blocks you. |

Source: [Sign and update agreements](https://developer.apple.com/help/app-store-connect/manage-agreements/sign-and-update-agreements)

## 1.2 Bank account (only with the Paid Apps Agreement)

ASC → Business → your entity → **Bank Account → Add**. For an Indian account, ASC asks for:

| ASC field | What goes there (you type it) |
|---|---|
| Bank country | India |
| Currency | INR (Apple pays Indian accounts in INR) |
| Bank name / branch | Pick the bank from Apple's list, usually by **IFSC** code |
| SWIFT/BIC | Only if ASC asks; your bank's SWIFT code is on its website or your passbook |
| Account holder name | **Exactly** as on your bank records (initials, spelling, order) |
| Account type | Savings or Current; an individual normally uses Savings |
| Account number | Type it yourself, twice |

**Common rejections:**
- The name doesn't match the bank record. This is the most common one.
- The account holder name doesn't match your developer account name.
- A wrong IFSC, or an NRE/NRO account picked by mistake.
- A business (Current) account used under an individual membership.

## 1.3 US tax form (only with the Paid Apps Agreement)

ASC → Business → **Tax Forms**. ASC asks a few questions and then gives you the right form. You don't choose it yourself.

- **Individual seller (your own name):** **W-8BEN**. **Company** (Sayne Design as a registered business): **W-8BEN-E**. See Open questions in the README.
- **Treaty claim (India–US):** Country = India, Article = **12 (Royalties)**, rate = **15%**. This is the figure Indian developers usually enter for app sales.
  - **Verify before you sign.** I couldn't confirm 15% from a primary source. Blog guides say 15%, but the IRS table is a PDF I couldn't read here. Check India's row in [IRS Tax Treaty Table 1](https://www.irs.gov/pub/irs-lbi/tax-treaty-table-1.pdf), or ask your CA.
- **Without a valid form:** Apple withholds US tax at the full statutory rate, which is 30% for non-US persons under US law. That applies only to sales to US customers.
- **Foreign TIN:** your **PAN**. You type it into the form yourself.
- **Certification:** you confirm under penalty of perjury that you're the beneficial owner, live in India, and aren't a US person. Your typed name counts as your signature.
- Once submitted, you can't edit it in ASC. Corrections go through Apple support.

Source: [Provide tax information](https://developer.apple.com/help/app-store-connect/manage-tax-information/provide-tax-information)

**Other countries' tax forms in ASC:** Australia, Brazil, Canada, Ireland, Mexico, Singapore, South Korea, Taiwan, Thailand and others. They apply only if you're registered for tax there, so skip them all. India GST: Apple's page doesn't list an India-specific form. Add a GSTIN only if you're GST-registered and ASC asks for it.

## 1.4 EU Digital Services Act trader status (**required now**)

ASC → **Business** (account level), then App Information → App Store Regulations and Permits → Digital Services Act for this app.

| Choice | When it fits | What happens |
|---|---|---|
| **Non-trader** | The app is free, has no IAP or ads, and isn't developed as a business | No contact details are shown. EU users are told consumer-protection rights don't apply. |
| **Trader** | You earn money from it or develop it as a business (VAT/GST registered, a company, advertising it commercially) | Address (or P.O. Box), phone and email are **shown publicly** on the EU product page. You must verify email and phone with 2FA and upload a document proving your business name and address. |

**Recommendation:**
- If Sayne Design is a registered business, or you plan to add IAP, choose **Trader**.
- If this is a free personal project for now, **Non-trader** is honest and keeps your details private.
- You can change it per app later. Apple can't decide this for you; ask a legal adviser if you're unsure.

Since Feb 17, 2025, apps with **no** trader status declared are removed from the EU App Store until one is declared.

Sources: [DSA trader requirements](https://developer.apple.com/help/app-store-connect/manage-compliance-information/manage-european-union-digital-services-act-trader-requirements) · [Upcoming requirements](https://developer.apple.com/news/upcoming-requirements/)

## 1.5 Contacts (ASC → Business → your entity → Contacts)

| Role | Needed now? | Who |
|---|---|---|
| Senior Management | Yes (needed for agreements) | You |
| Technical | Yes | You |
| Marketing | Optional | You |
| Legal | With paid apps | You |
| Finance | With paid apps | You |

Each contact needs a name, email and phone with country code (+91 …). You can use the same person for every role.
