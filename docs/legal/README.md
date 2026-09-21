# Legal & store documents (Network Rush)

Drafts prepared for the first Play release. **Not legal advice**: have them read once by a lawyer or, at least, by you. Placeholders to fill are written `[LIKE THIS]`.

| File | Purpose | Where it goes |
|---|---|---|
| `PRIVACY_POLICY.fr.md`, `PRIVACY_POLICY.en.md` | Privacy policy (mandatory with ads and Play Billing) | Host on a public URL, paste the URL in Play Console (App content > Privacy policy) and in `gradle.properties` as `privacyPolicyUrl` |
| `TERMS_OF_USE.md` | Short terms of use (optional but recommended: tips, ads, data, liability) | Same host, link from the store listing |
| `DATA_ATTRIBUTION.md` | Etalab Open Licence v2.0 attribution for the Strasbourg open data + CTS trademark note | Shown in Settings > Legal (already wired) and in the store description |
| `PLAY_DATA_SAFETY.md` | Answers for the Play "Data safety" form, ads declaration, content rating, target audience | Play Console |
| `STORE_LISTING.md` | Title, short/long descriptions fr + en, keywords, screenshots list | Play Console > Main store listing |

## Pre-publication to-do (legal)
1. Fill the placeholders: `[PUBLISHER NAME]`, `[POSTAL ADDRESS OR "on request"]`, `[CONTACT EMAIL]`, `[DATE]`. Play requires a real contact email and, for individual developer accounts, the address is shown on the listing.
2. Host the privacy policy on a stable public HTTPS URL (GitHub Pages of a public repo is enough) and set `privacyPolicyUrl=...` in `~/.gradle/gradle.properties`.
3. Publisher status: as an individual selling in-app tips you may need to declare yourself as a trader in the EU (DSA "trader status" in Play Console) and, in France, register the activity if it becomes regular (micro-entreprise). Check with an accountant.
4. Tips and ad revenue are income: declare them.
5. Trademark search (TMview / INPI, classes 9 and 41) for "Network Rush" is still open (see `TODO.md`).
6. Re-read these documents whenever data collection changes (analytics, crash reporting, leaderboard, accounts).
