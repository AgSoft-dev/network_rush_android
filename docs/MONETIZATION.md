# Monetization setup (banner + tips)

Implemented: **A** (AdMob banner on the home screen only) and **B2** (tips through Google Play Billing). Nothing gates gameplay. Anyone who tipped is a *supporter*: the banner is hidden and a heart shows next to the level on the home screen.

## Code map
- `domain/monetization/Monetization.kt`: `SupportTier`, `SupportRepository`, `AdsController`, `MonetizationPolicy`.
- `data/monetization/AdsControllerImpl.kt`: Google UMP consent (GDPR) first, AdMob SDK started only once ads may be requested; content rating capped at G.
- `data/monetization/BillingSupportRepository.kt`: Play Billing, tips are consumed right away (repeatable), sets the local `supporter` flag. No server-side verification (nothing valuable is unlocked).
- `presentation/common/AdBanner.kt`: adaptive banner, used only in `HomeScreen`.
- Settings: "Support Next Stop" section (prices from the store, thank-you snackbar) + "Privacy choices (ads)" when UMP requires it.

## Before publishing (checklist)
1. **AdMob**: create the app + a banner ad unit, then put the real ids in `~/.gradle/gradle.properties` (never commit them):
   ```
   admobAppId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
   admobBannerId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
   ```
   Without them, builds use Google's public **test** ids (test ads only, zero revenue). `adsEnabled=false` disables ads entirely (kill switch).
2. **AdMob > Privacy & messaging**: create the GDPR message (UMP) for the app, otherwise no consent form is shown and EEA users get no ads.
3. **Play Console > Monetize > In-app products**: create two *consumable* products, ids exactly `support_coffee` (~2 EUR) and `support_tram_ticket` (~1.60 EUR), and activate them. The app must be uploaded to an internal/closed testing track for Billing to return products; add license testers to try purchases without paying.
4. **Data safety form + privacy policy**: declare advertising ID / device identifiers collected by AdMob and purchase history (Play Billing); publish a privacy policy URL.
5. **Play Ads declaration**: tick "contains ads" in the Play Console app content.
6. Sign the release build and check it on a real device (test ads first, with your device registered as a test device, then real ads: never click your own ads).

## Behaviour notes
- On an emulator the consent round trip can take ~20 s: the banner appears once UMP answers.
- Tips are hidden ("not available right now") while offline or before the products exist in the Play Console.
- Pending (slow payment) purchases are picked up on the next launch.
