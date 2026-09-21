# Play Console answers (verify against the final build)

## App content > Privacy policy
URL of the hosted privacy policy (see `README.md`).

## Data safety form
- **Does the app collect or share required user data types?** Yes (through the Google Mobile Ads SDK).
- **Is all data encrypted in transit?** Yes (HTTPS).
- **Can users request data deletion?** The app stores nothing on servers; ad data is handled by Google (link Google's controls). Answer per the form wording: "No account, nothing to delete".
- Data types (collected **and** shared with Google AdMob, purpose: **Advertising or marketing**, not required for the app to work if the user declines personalised ads):
  | Category | Type | Collected | Shared | Purpose |
  |---|---|---|---|---|
  | Device or other IDs | Advertising ID | Yes | Yes (Google) | Advertising |
  | App activity | Ad interactions / app interactions (via SDK) | Yes | Yes (Google) | Advertising, analytics of ads |
  | App info and performance | Diagnostics / crash logs (Google SDK) | Yes | Yes (Google) | Analytics |
  | Location | Approximate location (IP based, via SDK) | Yes | Yes (Google) | Advertising |
- Play Billing: payment info is handled by Google Play; the app collects none (answer "No" for financial info).
- **Not collected:** name, email, precise location, contacts, photos, files, health, messages, audio.
- Review the exact list in AdMob's "Data disclosure" guide for the SDK version in use (play-services-ads 25.x) before submitting; it changes.

## Ads declaration
"Yes, my app contains ads." (AdMob banner on the home screen).

## Target audience and content
- Target age group: **13+ or 16+** (do not select under-13: it triggers the Families policy and prohibits most ad setups).
- Not primarily directed at children. Content rating questionnaire (IARC): no violence, no user-generated content, no gambling, no in-app currency; ads present; in-app purchases (tips) present. Expected rating: PEGI 3 / Everyone.

## Other declarations
- **Financial features / government / health apps:** none.
- **News app:** no.
- **COVID/contact tracing:** no.
- **Advertising ID permission (`com.google.android.gms.permission.AD_ID`):** declare "used for advertising"; it is added by the Ads SDK (target 13+ requires the declaration).
- **Content of the listing:** no CTS logo, no claim of official status.
- **DSA trader status (EU):** provide name, address, phone/email if you monetise as an individual.
- **Closed testing** requirement for new personal developer accounts (roughly 12 testers for 14 days): check the current rule in Play Console.
- **Account deletion:** not applicable (no accounts).
