# Data attribution

## Strasbourg open data
Station names and the network structure come from the dataset **"Stations de tram"**, published by **Ville et Eurométropole de Strasbourg** on the open data portal:
https://data.strasbourg.eu/explore/dataset/stations_tram/

- Licence: **Licence Ouverte v2.0 (Etalab)** https://www.etalab.gouv.fr/wp-content/uploads/2017/04/ETALAB-Licence-Ouverte-v2.0.pdf
- Metadata checked on 2026-09-21: portal states the licence above, publisher "Ville et eurométropole de Strasbourg", dataset last modified 2026-06-17.
- The licence requires **mentioning the source and the date of the last update** of the data. It allows reuse, including commercial, and adaptation.

### Mention to display (in-app, Settings > Legal, and on the store listing)
> Station data: « Stations de tram », Ville et Eurométropole de Strasbourg, Licence Ouverte v2.0 (Etalab), data.strasbourg.eu. Data adapted (ordering by line, schematic diagram coordinates). Last update of the source: 17 June 2026 (accessed 21 September 2026).

### Adaptation notice
The dataset gives station positions. The app **adapts** it: the order of stations along each line and the schematic diagram coordinates were curated by hand and are not official geographic positions. The Ville et Eurométropole de Strasbourg does not endorse the app and is not responsible for its content (the Etalab licence forbids implying official endorsement).

### Provenance (confirmed by the author, 2026-09-21)
The station names come from the dataset above (table view sorted by `nom_arret`, filter `ligne_s`), accessed on 21 September 2026. The portal metadata gives the last modification of the dataset as 2026-06-17. The line order and schematic coordinates in `app/src/main/assets/strasbourg_stations.json` are the app's own adaptation (mention above). If the dataset is refreshed, update the date in this file and in `SettingsScreen.kt` (`LegalSection`).

## CTS (Compagnie des Transports Strasbourgeois)
Line names (A to F...) and their colours reproduce the public identity of the CTS network to identify lines. "CTS" and its logos are the property of the CTS. Network Rush is an independent, unofficial fan/educational app, **not affiliated with, sponsored or endorsed by the CTS or the Eurométropole**; the CTS logo is not used. Keep it that way: do not use CTS logos or its wordmark in the icon, screenshots or the listing.

## Fonts
- Bricolage Grotesque and DM Sans, SIL Open Font License 1.1 (bundled in `res/font`). Keep the licence texts in the repository/about screen.

## Third-party libraries
Jetpack Compose, Hilt, Room, DataStore, Gson, Kotlin coroutines (Apache 2.0); Google Mobile Ads SDK, User Messaging Platform, Play Billing (Google terms). An open source licence screen can be generated later (`oss-licenses-plugin`) if wanted.
