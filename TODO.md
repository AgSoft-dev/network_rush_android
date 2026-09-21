# Network Rush (TriviaMap) — Revue technique & gameplay

Revue du 2026-09-20 sur `master` (v0.1.0, POC). Statut : `[ ]` à faire · `[~]` en cours · `[x]` fait.
Priorités : **P0** bug bloquant / perte de données · **P1** important · **P2** confort · **P3** idée.
Références au format `fichier:ligne` quand pertinent (lignes indicatives, à re-vérifier).

> **Périmètre actuel (2026-09-20)** : on se concentre sur **Station Sprint**. **Trace Network** est parké : bouton visible uniquement en build debug (`BuildConfig.DEBUG`, libellé "TRACE NETWORK (DEV)"), ses items restent listés (§3) pour la prochaine étape.
> **Avancement (2026-09-21)** : §1 P0/P1 Sprint, §2 (tests, extraction domaine, deps), §4 Sprint, progression/daily, monétisation (code) et identité Terminus sont faits ; 53 tests unitaires OK, lint + release/R8 OK, vérifié sur émulateur. Reste avant publication : voir **« Checklist avant mise en production »** ci-dessous.


---

## 0. Synthèse

Le jeu tient sur deux modes :
- **Trace Network** (`GameplayViewModel`) : redessiner le réseau de tram de mémoire.
- **Station Sprint** (`SprintViewModel`) : réordonner / classer des stations sous timer (le vrai cœur du jeu actuel).

Constats principaux :
1. Le mode Sprint est le plus abouti et le plus fun, mais sa boucle de jeu a des trous d'équilibrage exploitables (skip gratuit, timer qui plafonne, pas de fin réelle).
2. Le mode Trace est **incohérent** : le score n'utilise plus la précision du tracé (Fréchet, RDP, Bézier codés mais jamais appelés), le README décrit un scoring qui n'existe plus, et les difficultés n'ont pas le même sens entre les deux modes.
3. Techniquement : architecture propre (Hilt/Room/Compose) mais quelques bugs réels (race au démarrage, streak, best scores, sérialisation des résultats) et **aucun test**. Le dossier `app/build/` est **suivi par git** (1927 fichiers) malgré le `.gitignore`.

---

## 1. Bugs & correctness (P0/P1)

- [x] **P0 — `app/build/` versionné dans git** : 1927 fichiers trackés, chaque build pollue `git status`. Le `.gitignore` est correct mais arrive après coup. → `git rm -r --cached app/build build .gradle` puis commit. Vérifier aussi `local.properties`.
  - ✅ `git rm -r --cached app/build` fait (changements stagés, pas commités). `.gradle`/`build`/`local.properties` racine n'étaient pas suivis.
- [x] **P0 — Race chargement des données** (`MainActivity.kt`) : `syncFromAssets()` est lancé dans un `CoroutineScope(Dispatchers.IO)` orphelin, sans garantie d'être fini quand un ViewModel démarre. Les VM font `if (lines.isEmpty()) return@collectLatest` → écran Loading infini si le parse échoue (aucune gestion d'erreur, aucun état `Error`). → charger via un `init`/lazy dans le repository (ou `Flow` qui émet au premier collect), exposer `Result`/état d'erreur, afficher un écran d'erreur + retry.
  - ✅ `TramLineRepository` expose `state: Loading/Loaded/Error` + `load()` idempotent (Mutex, try/catch). `MainActivity` utilise `lifecycleScope`; `SprintViewModel` appelle `load()` lui-même et affiche un écran d'erreur + RETRY (`loadFailed`, `retryLoad()`).
- [x] **P0 — Le timer de Sprint ne s'arrête jamais correctement** (`SprintViewModel.startTimer/endGame`) : `endGame()` est un `viewModelScope.launch` déclenché à la sortie de boucle, mais la phase passe `Validating` dans `_state.update` **avant** ; `submit()`/`moveTile()` ne vérifient que `Drawing`. Cas limite : pénalité qui ramène `timeLeftMs` à 0 dans `submit()` → la boucle sort, OK, mais `handleCorrectAnswer` peut relancer `generateChallenge()` (remet `phase = Drawing`) **après** la fin de partie → partie zombie / `endGame` double si le timer redémarre (`if (timerJob == null) startTimer()` n'est jamais remis à null).
  - ✅ flag `finished` (fin idempotente), `generateChallenge/submit/skip/moveTile/burst timeout` inactifs après la fin ; boucle timer sortie propre puis `endGame()` une seule fois.
- [x] **P1 — Streak quotidien faux** (`UserPreferencesRepositoryImpl.updateStreak`) : calcule `diffDays` sur un delta de 24 h glissant, pas sur des jours calendaires. Jouer à 23h puis à 8h le lendemain (9 h d'écart) → `diffDays == 0` → pas d'incrément alors que c'est un nouveau jour. Inversement, jouer à 8h puis à 7h deux jours plus tard (47 h) → `diffDays == 1` → streak +1 alors qu'un jour a été sauté. Utiliser `LocalDate` (fuseau device) : `today - lastDay == 0 / 1 / >1`, avec `Clock` injectable pour les tests.
  - ✅ calcul par jour calendaire (`LocalDate`/epoch day, clé DataStore `last_played_epoch_day`), `Clock` interne prêt pour l'injection. Reste : tests unitaires.
- [ ] **P1 — Streak incrémenté uniquement en Sprint** ; le mode Trace ne l'alimente pas alors que la Home l'affiche comme "daily streak" général. Décider de la règle.
  - ⏸ sans objet tant que Trace est en dev-only ; à retrancher quand Trace revient.
- [x] **P1 — Requête `getBestScores` douteuse** (`GameResultDao`) : `HAVING MAX(score)` est un booléen SQLite (vrai dès que MAX ≠ 0), pas un filtre "la ligne du max". Avec `GROUP BY lineId` on récupère un `id` arbitraire par groupe (bare column) → le "meilleur score par ligne" n'est pas garanti. Et Sprint enregistre `lineId = "SPRINT"`, Trace `"ALL"` → l'écran Stats affiche "Line SPRINT" / "Line ALL". → clé de regroupement `(mode, difficulty)`, requête correcte (`JOIN` sur `MAX(score)` ou window function), libellés propres.
  - ✅ regroupement par `(mode, difficulty)` avec jointure sur `MAX(score)` ; Stats affiche "Station Sprint / Trace Network" + niveau atteint (plus de "Line SPRINT").
- [x] **P1 — `GameResult` détourné pour Sprint** : `completionScore = level.toFloat()`, `speedBonusScore` "repurposé" en `maxCombo`, `durationMs = 0`, `ScoreBreakdown.completion/speedBonus` utilisés comme fourre-tout. Ça fonctionne mais casse tout futur calcul (ex. `ResultsScreen` affiche `accuracy*100` sur un champ qui est un ratio, `completion` sur un level). → modèles dédiés `SprintResult` / `TraceResult` (sealed) ou colonnes explicites (`level`, `maxCombo`, `accuracy`), migration Room.
  - ✅ nouvelles colonnes `level`, `maxCombo`, `accuracy` (entity + domain), migration Room 1→2 qui rapatrie les anciennes lignes Sprint. `completionScore/speedBonusScore` ne sont plus détournés. Le `ScoreBreakdown` de Sprint ne porte plus que `stationOrder` (=accuracy) + `total`.
- [x] **P1 — `fallbackToDestructiveMigration()` en prod** (`AppModule.kt`) : chaque changement de schéma efface les scores des joueurs. Acceptable en POC, à retirer avant toute release ; `exportSchema = false` empêche d'écrire des migrations testables → passer à `true` + `schemas/`.
  - ✅ remplacé par `addMigrations(MIGRATION_1_2)`, `exportSchema = true` (`app/schemas/`). ⚠ migration non testée sur un vrai appareil (pas de schéma v1 exporté) → à valider en installant l'ancien APK puis le nouveau.
- [x] **P1 — Timer Sprint : pas d'arrêt en arrière-plan / rotation** : `System.currentTimeMillis()` + `delay(50)` continue si l'app passe en background (le delta cumulé fait perdre du temps au retour, ou triche en pause via l'OS). Utiliser `SystemClock.elapsedRealtime()` + pause sur `ON_STOP`/`ON_START` (lifecycle), idem timer de Trace (`elapsedMs` = wall clock).
  - ✅ `SystemClock.elapsedRealtime()` + `pause()/resume()` branchés sur `ON_STOP/ON_START` (timer principal, burst, chrono de la question). Reste : timer de Trace (`GameplayViewModel`).
- [x] **P1 — Sprint : `SPEED_BURST` peut planter** (`generateSpeedBurstChallenge`) : `(0..(line.stations.size - count)).random()` OK si ≥3 stations, mais **aucune garde** si une ligne future en a moins. Idem `generateReorderChallenge` stage 1 : le filtre `indexOf(s)` est O(n²) et peut laisser < 3 stations (hubs + terminus) → `count` non respecté silencieusement.
  - ✅ lignes < 3 stations filtrées au chargement, stage 1 retombe sur la ligne complète si < 3 stations filtrées, CLASSIFY → REORDER si < 2 lignes, bounds de repli sur les stations.
- [ ] **P1 — `generateClassifyChallenge` : cas dégénérés** : `allLines.filter { it.id != line1.id }.random()` OK, mais `stations1/stations2 take(totalCount/2)` + `hubs` puis `.take(totalCount)` → on peut couper des hubs (donc l'ordre de tri `minOf(idx1, idx2)` mélange deux lignes : l'"ordre correct" pour des stations de lignes différentes est **arbitraire** — cf. §3 gameplay). `999` en sentinelle fragile.
  - ⏸ garde-fous ajoutés ; le problème de fond (ordre `minOf(idx1, idx2)` arbitraire entre deux lignes) est traité au §4 (CLASSIFY).
- [x] **P1 — Skip = gratuit et compte comme "submission"** (`skipQuestion` → `handleCorrectAnswer(isSkip = true)`) : donne `timeGain` complet (base + type bonus + combo remis à 0 mais `comboBonus` calculé sur `newCombo = 0`), donc **skip = +8–12 s sans effort**. Exploit trivial : skipper en boucle maintient le timer indéfiniment (plafonné par `maxTimeMs` mais jamais de perte). Le raccourci est en plus câblé sur un `clickable` du **bloc score** (`SprintTopBar`) → skip accidentel très probable.
  - ✅ skip coûte 4 s + reset du combo, aucun gain de temps/points, ne fait pas progresser le niveau ; bouton explicite "SKIP -4s" (plus de clic caché sur le score) ; entrées ignorées pendant le feedback de 700 ms. Restent au §4 : quota de skips, équilibrage global.
- [x] **P1 — Hard/Trace : `showGhost`, noms et difficultés incohérents** : Home décrit Easy = "Sequential stops segment", Medium = "Random stops from one line", Hard = "Truly random network stops" (description pensée pour Sprint) alors que `Difficulty` est documentée `EASY = names + positions + ghost hint` pour Trace. Le même enum fait deux choses. Voir §3.
  - ✅ côté Sprint : textes de difficulté de Home décrivent enfin l'horloge/gains réels. Le sens de `Difficulty` pour Trace reste à traiter au §3.
- [ ] **P1 — Pas de gestion de la rotation / process death** : `screenOrientation="portrait"` masque le problème de rotation, mais l'état de partie (timer, score, tuiles) vit uniquement dans le `MutableStateFlow` du VM → process death = partie perdue sans message. `SavedStateHandle` n'est utilisé que pour lire `difficulty`.
  - ⏸ non traité : nécessite de sérialiser l'état de run (SavedStateHandle ou Room). À faire avec l'extraction des règles de jeu (§2).
- [ ] **P2 — Navigation Results** : `onRetry` et `onHome` font la même chose (retour Home) ; `popUpTo(Route.Home.path)` sans `inclusive` OK mais le bouton "Retry" n'en est pas un. Les résultats sont passés par **arguments de route** (score/accuracy/level…) → falsifiables via deeplink si un jour exportées, non restaurés proprement. Préférer un `resultId` Room.
  - ⏸ non traité (passage par id de résultat + vrai "Rejouer" avec la difficulté).
- [ ] **P2 — `Route.Results` route string** contient `?accuracy=...` avec Float en string (locale ? `Float.toString` est `Locale`-indépendant, OK) — mais fragile ; passer à un id.
- [x] **P2 — `Icons.Default.ArrowBack` déprécié** dans Gameplay/Stats/Home alors que Sprint utilise `AutoMirrored` → incohérent, warnings, mauvais rendu RTL (`supportsRtl=true`).
  - ✅ remplacé par `AutoMirrored` (Stats, Gameplay).
- [x] **P2 — `LineSelectionScreen.kt` orphelin** : aucune route, code mort (à supprimer ou brancher). Idem `strasbourg_stations_old.json` et `strasbourg_tram.geojson` (3 Ko, **non utilisés** : le parser lit `strasbourg_stations.json`) — le README décrit encore le pipeline GeoJSON/`curl` open-data qui ne correspond plus.
  - ✅ supprimés (stagés) : `LineSelectionScreen.kt`, `strasbourg_stations_old.json`, `strasbourg_tram.geojson`. Le README les mentionne encore (à réécrire).
- [ ] **P2 — `onDrawEnd()` vide** (`GameplayViewModel`) ; `detectTransformGestures` + `awaitPointerEvent` sur la même surface : le dessin ne se déclenche que si `changes.size == 1` mais le pan à un doigt de `detectTransformGestures` **entre en conflit** avec le dessin (les deux consomment le même geste à un doigt). Le README affirme le contraire. À tester sur device ; sans doute nécessiter un mode "dessin/déplacement" explicite (bouton) ou dessin à 1 doigt / pan à 2 doigts uniquement.
  - ⏸ spécifique à Trace (parké, dev-only) → à traiter avec le §3.
- [ ] **P2 — Le dessin re-crée `pointerInput` à chaque changement de `scale/offset`** (`pointerInput(bounds, activeLineId, scale, offsetX, offsetY)`) → coroutine d'entrée redémarrée en plein geste de zoom, points perdus. Utiliser `rememberUpdatedState` / lire les états dans la lambda.
  - ⏸ spécifique à Trace (parké, dev-only) → à traiter avec le §3.
- [ ] **P2 — `onDraw` copie toute la liste à chaque point** (`currentPath + point`, `playerPaths + ...`) → O(n²) sur un tracé long, une recomposition + un `_state.update` par événement `Move`. Échantillonner (distance min entre points), utiliser une liste mutable côté UI, ne publier qu'au `onDrawEnd`.
  - ⏸ spécifique à Trace (parké, dev-only) → à traiter avec le §3.
- [ ] **P2 — `Paint()` alloué à chaque station à chaque frame** (`TramMapCanvas`) → garbage en pleine animation de pan/zoom. Hisser dans `remember`. Idem `smoothPath` recalculé à chaque recomposition (mémoïser avec `remember(path)`), `visited.any {}` O(n) par station.
  - ⏸ spécifique à Trace (parké, dev-only) → à traiter avec le §3.
- [ ] **P2 — Collision de labels** : Medium affiche **tous** les noms (~150 stations sur 7 lignes) sans anti-collision (déjà en roadmap v0.2). Illisible sans zoom.
  - ⏸ spécifique à Trace (parké, dev-only) → à traiter avec le §3.
- [ ] **P2 — Accessibilité** : `contentDescription = null` sur la plupart des icônes, drag & drop **sans alternative** (boutons monter/descendre, TalkBack), pas de support `fontScale` (textes en `sp` mais layouts rigides), couleurs ligne/texte non vérifiées en contraste (surtout lignes jaunes/claires avec `textColor` blanc), daltonisme (rouge/vert Success/Error comme seul signal).
- [x] **P2 — Chaînes en dur** : extraites en ressources en/fr/de (2026-09-21). Restent en anglais : écran Trace et variantes Sprint (dev-only).
- [x] **P3 — Release** : `proguard-rules.pro` référencé mais **absent** → le build release (`isMinifyEnabled = true`) échouera / Gson (`GeoPointDto`, `Array<GeoPointDto>`) sera cassé par R8 sans règles keep. Prévoir règles ou migrer vers `kotlinx.serialization`.
  - ✅ `proguard-rules.pro` créé (keep `GeoPointDto` pour Gson) ; `assembleDebug` et `minifyReleaseWithR8` passent.
- [x] **P3 — `allowBackup="true"`** sans `dataExtractionRules`/`fullBackupContent` (Android 12+ warning) ; scores + badges seraient restaurés sans cohérence avec la DB.
  - ✅ `allowBackup="false"`.

---

## 2. Dette technique / architecture

- [x] **P1 — Aucun test** (`app/src/test` et `androidTest` inexistants). Le code le plus critique est pur et testable : `ScoringEngine`, `GeometryEngine`, `GeoJsonParser`, génération des défis Sprint, streak. → tests JUnit + Turbine (Flows) ; commencer par : LCS/ordre, `getStage`, générateurs (invariants : `correctOrder` cohérent, tailles, unicité), streak sur plusieurs dates (injecter un `Clock`).
- [x] **P1 — Logique de jeu dans les ViewModels** (`SprintViewModel` ~490 lignes) : génération de défis, règles de score, timers, badges, persistance, tout dans une classe. → extraire `ChallengeGenerator`, `SprintRules` (score/temps/pénalités) purs + injection d'un `Random` seedable et d'un `Clock`. Prérequis du daily challenge (§4) et des tests.
  - ✅ extraction dans `domain/sprint` : `ChallengeGenerator` (Random injecté), `Challenge.isSolvedBy`, `SprintRules` (stage, gains, pénalités, points). `SprintViewModel` réduit à l'orchestration ; `Random`, `Clock` et `TimeSource` injectés via `RuntimeModule`. Streak extrait en `StreakCalculator`. Bonus : une tuile ne démarre plus déjà dans le bon ordre.
- [ ] **P1 — Code mort du moteur de tracé** : `simplifyPolyline` (RDP), `discreteFrechetDistance` (récursif → **StackOverflow** sur ≥ ~5000 points, doit être itératif), `TramLine.geometry` scoring, `pathAccuracyScore` toujours à `0f`… Soit on ré-intègre la précision (§3), soit on supprime pour ne pas laisser croire que c'est actif. Le `ScoreBreakdown` de Trace ignore `playerPaths` (param `playerPaths` inutilisé dans `computeGlobal`).
  - ⏸ Fréchet réécrit en itératif (plus de StackOverflow, test à 3000 points). Le reste (scoring Trace ignorant le tracé, RDP/Bézier) est parké avec Trace (§3).
- [x] **P1 — README désynchronisé** : décrit poids 30/35/25/10, snap 150 m, Fréchet 800 m, `LineSelectionScreen`, `StatsViewModel`, pipeline GeoJSON lon/lat + Etalab… Le code : coordonnées **schématiques** (x∈[353,937], y∈[270,740], pas de lon/lat), snap `30.0` unités, poids 50/40/10 · 60/30/10 · 60/20/20, données curées à la main. Réécrire ; source/licence des données à clarifier (provenance des coordonnées "diagramme" ?).
  - ✅ README réécrit (Sprint, architecture réelle, données schématiques, build/JDK, scoring). Section Trace réduite à un renvoi vers le TODO ; provenance/licence des données restent à documenter (§5).
- [x] **P2 — `Repositories.kt` dupliqué** (`data/repository` et `domain/repository` portent le même nom de fichier) + plusieurs classes par fichier (`UseCases.kt`, `Models.kt`). Renommer par classe.
  - ✅ fichiers scindés : `TramLineRepository(.Impl)`, `GameResultRepository(.Impl)` ; `Models.kt` → `TramNetwork.kt`, `GameMode.kt`, `GameResult.kt`.
- [x] **P2 — Use cases anémiques** (`GetLineUseCase`, `GetLineResultsUseCase` : simples délégations, `GetLineUseCase` non utilisé). SprintViewModel injecte **directement** les repositories en plus des use cases → couche domain incohérente. Choisir : soit repos partout, soit use cases partout.
  - ✅ use cases supprimés ; ViewModels injectent directement les repositories (choix : app petite, pas de logique dans les use cases).
- [x] **P2 — `TramLineRepositoryImpl`** : `MutableStateFlow(emptyList())` comme "loading state" est ambigu (vide = pas chargé ou pas de données ?). Modéliser `Loading/Loaded/Error`.
  - ✅ `LinesState` Loading/Loaded/Error (fait au §1).
- [x] **P2 — `GeoBounds.from` : padding 10% / 25% "pour l'UI" codé dans la util géométrique** → mélange couche présentation/util ; `toNormalized` divise par zéro si une ligne est parfaitement horizontale/verticale (bounds nulles).
  - ✅ padding paramétrable (`padX`, `padY`, défauts inchangés) et `width/height` jamais nuls (plus de division par zéro). Le padding « UI » n'est pas encore déplacé côté présentation.
- [ ] **P2 — Ratio d'aspect déformé** : la projection normalise x et y indépendamment sur `size.width/height` du canvas → le réseau est **étiré** selon l'écran (distances et angles non conservés). Utiliser un scale uniforme (fit-center) ; impacte aussi le rayon de snap (30 unités ≠ même distance en px selon l'axe).
  - ⏸ concerne Trace + fond Sprint ; à traiter avec le §3.
- [x] **P2 — Dépendances datées** : Kotlin 1.9.23, Compose BOM 2024.05, AGP 8.13.2 (décalage avec Kotlin/Gradle), `compileSdk/targetSdk 34` (Play exige 35 depuis 2025-08), Gson (maintenance mode), Material 2 (roadmap M3 abandonnée en pratique). Planifier bump : Kotlin 2.x + plugin Compose compiler, BOM récent, targetSdk 35/36, `kotlinx.serialization`.
  - ✅ 2026-09-21 : Gradle 8.13→9.7.1, AGP 8.13.2→9.4.1 (Kotlin intégré), Kotlin 1.9.23→2.3.21 (plugin Compose), KSP 2.3.12, Hilt 2.59.2, Room 2.8.5, Compose BOM 2026.09.00, Navigation 2.10.1, Lifecycle 2.10.0, Activity 1.12.4, DataStore 1.2.1, coroutines 1.10.2, Gson 2.13.2, compileSdk 37 / targetSdk 35, JDK daemon épinglé à 21 (`gradle-daemon-jvm.properties` + foojay). Build vérifié : `testDebugUnitTest` (21 tests OK), `lintDebug`, `assembleDebug`, `assembleRelease` (R8). Corrigé : `animateItemPlacement`→`animateItem`, `hiltViewModel`/`LocalLifecycleOwner` déplacés, lint `NonObservableLocale`.
  - ✅ 2026-09-21 : targetSdk 35→36 (insets barres système ajoutés Settings/Progress/Results/Sprint, `enableOnBackInvokedCallback`, UI centrée max 600dp car l'orientation portrait est ignorée sur écrans ≥ 600dp ; non testé sur émulateur API 36).
  - ⏸ Reste : migration Material 3, `kotlinx.serialization` à la place de Gson.
- [x] **P2 — Pas de CI / lint** : ajouter GitHub Actions (`./gradlew lintDebug testDebugUnitTest assembleDebug`), ktlint/detekt.
  - ✅ `.github/workflows/ci.yml` (tests + lintDebug + assembleDebug, JDK 17). Reste : ktlint/detekt, lint jamais exécuté localement.
- [ ] **P2 — Logging / analytics / crash reporting** : rien. Au minimum Crashlytics (ou équivalent respectueux de la vie privée) avant beta.
  - ⏸ non traité (choix produit : outil, vie privée).
- [ ] **P3 — Pas de `versionName` / changelog / signature release** planifiés.
- [ ] **P3 — Perf de démarrage** : parse JSON 32 Ko sur IO, trivial ; RAS. À surveiller si multi-villes (§5) → prévoir parsing lazy par ville et cache.
- [ ] **P3 — Modularisation** (`:core:model`, `:feature:sprint`, `:feature:trace`) seulement si multi-villes/équipe ; pas prioritaire.

---

## 3. Gameplay — mode Trace Network

Problème de fond : **la promesse ("redessine le réseau de mémoire") n'est pas tenue par le scoring actuel.**

- [ ] **P0 — Le score ne dépend plus du tracé** : `computeGlobal` n'utilise que l'ordre des stations *touchées* (LCS) et la complétion. Un joueur qui zigzague sur l'écran en passant par les stations dans l'ordre obtient un score parfait. La précision géométrique a été retirée ("Removed accuracy scoring") sans remplacement. → décider de la vision :
  - (a) jeu de **mémoire topologique** : ce qui compte = ordre + connexions (stations reliées consécutivement) → scorer les **arêtes** (paire de stations consécutives touchées d'affilée) plutôt que le LCS ;
  - (b) jeu de **mémoire géographique** : réintégrer Fréchet/Hausdorff normalisé (attention à la déformation, cf. §2).
- [ ] **P1 — Détection de visite trop permissive et bruitée** : snap radius fixe 30 unités sur un plan de ~580×470 → ~5 % de la largeur, alors que des stations de lignes différentes sont parfois à < 1 unité (min mesuré 0.65). En traçant la ligne A près d'un croisement, on valide les stations d'autres lignes proches ; l'ordre est l'ordre de **première visite** seulement (repasser sur une station ne change rien). Traiter : rayon relatif au zoom, hystérésis, séquence = ordre de passage réel (avec doublons pour les allers-retours), et snapping par ligne active uniquement (déjà le cas, mais hubs partagés IDs entre lignes → ok).
- [ ] **P1 — Direction non vérifiée** : LCS non orienté dans le sens : un tracé inversé donne un mauvais score, mais le joueur n'a **aucune consigne** de direction. Accepter les deux sens (max(LCS, LCS reversed)).
- [ ] **P1 — Pas de feedback de fin** : après validation, retour direct sur l'écran de résultat avec un total ; aucune visualisation de ce qui était juste/faux (README roadmap : "animated validation overlay"). C'est **le** moment pédagogique du jeu → superposer réseau réel vs tracé, stations manquées en rouge.
- [ ] **P1 — Une seule partie = tout le réseau (7 lignes, ~165 stations) en 3–5 min** : charge énorme pour du mobile, tracé au doigt imprécis, et un seul score global (`lineId = "ALL"`). Proposer des parties **par ligne** (`LineSelectionScreen` existe, orphelin) et des paliers (1 ligne → 2 → réseau complet).
- [ ] **P1 — Difficulté peu lisible** : EASY affiche les fantômes + noms au fur et à mesure, MEDIUM affiche **tous les noms** (donc plus facile que "mémoire" : on lit la carte), HARD masque les noms mais pas les positions/cercles des stations → on retrouve l'itinéraire à vue. Une vraie difficulté "mémoire" = ne montrer ni positions ni noms, seulement une liste de stations à placer/relier. Revoir les 3 niveaux.
- [ ] **P1 — Limite de temps non appliquée** : `timeLimitMs` sert seulement au bonus de vitesse, aucun arrêt à 0 ; le timer HUD monte indéfiniment (`elapsedMs`) sans limite visible.
- [ ] **P2 — Pas d'annulation partielle** : "Clear" efface toute la ligne active ; ajouter undo du dernier trait.
- [ ] **P2 — Outil de tracé** : dessin libre au doigt (imprécis, le doigt masque la cible). Alternative plus jouable : **relier des stations en tapant** (tap station → tap suivante) avec le tracé auto-lissé ; garder le dessin libre en mode expert.
- [ ] **P2 — Onboarding absent** : aucun tutoriel (pan à 2 doigts ? pinch ? comment dessiner ?), aucune explication du score sur Home.
- [ ] **P2 — Fond de carte** : aucun repère (Rhin, centre-ville, gares) → sans contexte, difficile de "se placer" ; un fond minimal (contour communes/eau) en `Path` statique aiderait sans casser le style.

---

## 4. Gameplay — mode Station Sprint

Le mode le plus prometteur (boucle courte, tension du timer, combos). Points d'équilibrage et de design :

- [x] **P0 — Pas de vraie condition de défaite ni de courbe de difficulté finie** : la seule fin est le timer à 0 ; skip gratuit (cf. §1) + gains de temps 8–12 s pour ≤ 8 tuiles → un joueur régulier peut jouer indéfiniment. Ajouter : skip coûteux (−temps, −combo, quota limité), et/ou décroissance du gain de temps avec le niveau (au-delà du stage 5, "else" = stage 5 permanent, plus aucune montée).
  - ✅ 2026-09-21 : skips limités à 3 par run (−4 s, combo remis à 0, sans gain) ; gain de temps proportionnel à la taille du puzzle et **décroissant avec le niveau** (`SprintRules`), jusqu'à un plancher inférieur au temps de résolution → toute partie finit. Vérifié par `SprintEconomyTest` (un joueur parfait et rapide meurt en < 25 min simulées).
- [x] **P0 — Stage 5 infini à difficulté constante** : `getStage` plafonne à 5 dès le niveau 21 ; count 7–8 constant, gain de temps `coerceAtLeast(4000)`. Ajouter une montée continue (moins de temps, plus de tuiles, distracteurs, lignes moins connues) pour qu'une partie ait un plafond de compétence.
  - ✅ 2026-09-21 : au-delà du niveau 20 le nombre de tuiles continue de croître (7 → 10), le burst se resserre (8 s → 6 s), la pénalité d'erreur monte avec le stage et le gain de temps continue de décroître jusqu'au plancher. Non fait : distracteurs, lignes « moins connues ».
- [x] **P1 — Les gains/pénalités de temps créent une boucle "positive"** : gain (≥ 4 s + combo jusqu'à +5 s + 5 s classify) vs pénalité (5–9 s) ; à partir de ~5 réponses justes le joueur est net positif quelle que soit la difficulté (plafond `maxTimeMs` de 30–60 s seulement). Simuler l'économie (script) : temps moyen de résolution par type × gains, cible ~ 60–90 s de survie pour un joueur moyen.
  - ✅ 2026-09-21 : économie refaite et simulée (`SprintEconomyTest`, mêmes modèles de joueur que le script d'analyse) : joueur moyen ≈ 90 s en Medium (60–140 s exigés par le test), survie croissante avec le niveau de compétence, Easy > Medium > Hard. Constantes : gain/tuile 1.3/1.05/0.95 s, décroissance 3 %/niveau, plancher 40/35/30 %. À réajuster avec de vraies parties.
- [x] **P1 — CLASSIFY : l'"ordre correct" est mal défini** (cf. §1) : trier des stations de deux lignes différentes par `minOf(idx1, idx2)` n'a pas de sens géographique/pédagogique (l'index 3 de la ligne A n'est pas "avant" l'index 5 de la ligne D). Le joueur ne peut pas déduire l'ordre → frustration/aléatoire. Séparer : (1) classer gauche/droite/hub, (2) ordonner **dans chaque colonne**.
  - ✅ 2026-09-21 : l'ordre n'est plus comparé entre colonnes : chaque colonne (ligne 1 / hub / ligne 2) suit l'ordre de sa propre ligne (les hubs suivent la ligne 1), `Challenge.columnOrders`. Le sens (avant/arrière) s'applique à chaque colonne.
- [x] **P1 — CLASSIFY : ambiguïté hub** : une station "hub" (commune aux deux lignes) est correcte uniquement au centre ; mais l'UI ne montre que trois offsets de 24 dp (translationX) : très faible affordance (les tuiles bougent à peine), seuil 30 dp au relâché. Zones de dépôt visibles nécessaires (colonnes colorées).
  - ✅ 2026-09-21 : tuiles à 62 % de largeur qui glissent entre 3 colonnes visibles (fond coloré, colonne cible surlignée pendant le drag, snap animé, vibration au changement de colonne, seuil = mi-course). À valider au doigt.
- [x] **P1 — Direction "FOLLOW / REVERSE" mal expliquée** : une flèche ↓/↑ minuscule ; l'ordre "reverse" est la source principale d'échecs involontaires. Afficher les **terminus** ("de Graffenstaden vers Parc des Sports") au lieu d'une flèche.
  - ✅ 2026-09-21 : chips « [ligne] ↓ toward <terminus> » + rappel « first station on top » (une ligne par ligne en CLASSIFY), à la place de la flèche.
- [x] **P1 — Stage 1 filtre "stations à correspondance + terminus"** : n'a pas de sens si `Station.lines` ne contient qu'**une** ligne (le parser met `listOf(lineId)` "stations are per line") → `s.lines.size > 1` est **toujours faux**, le filtre ne garde que les 2 terminus → `availableStations.size <= count` → tuiles = 2 stations, jamais 3–4. À vérifier / corriger (dériver `lines` en croisant les IDs partagés entre lignes).
  - ✅ le parser dérive maintenant `Station.lines` des ids partagés entre lignes (les hubs sont détectés) ; repli sur la ligne complète si < 3 stations. À vérifier en jeu : les tuiles du stage 1 = hubs + terminus, pas forcément contigus.
- [x] **P1 — Répétition des défis** : `allLines.random()` sans mémoire → mêmes lignes/segments de suite, aucune progression de "connaissance". Ajouter un tirage pondéré par erreurs passées (répétition espacée), et éviter les doublons consécutifs.
  - ✅ 2026-09-21 : répétition espacée : poids par station (`StationStat.weight` : jamais vue 2.0, ratée jusqu'à 4.0, maîtrisée 0.5) pour choisir fenêtres/échantillons, et mémoire des 4 derniers segments pour éviter les répétitions immédiates. Désactivé pour le daily (identique pour tous).
- [x] **P1 — Pas de feedback pédagogique** : sur une mauvaise réponse → "WRONG! −5s" (et le libellé `-5s` est **codé en dur** alors que la pénalité est 5–9 s), pas de correction affichée. Montrer la bonne réponse (ou au moins quelles tuiles étaient mal placées) : c'est ce qui fait apprendre le réseau.
  - ✅ 2026-09-21 : après une mauvaise réponse : tuiles vertes (bien placées) / rouges (mal placées) jusqu'au prochain déplacement, « x / y in place », libellé de pénalité réel. Le calcul « mal placé » utilise la plus longue sous-séquence correcte (un seul déplacement fautif ne marque pas ses voisines). Reste : afficher la bonne réponse après N échecs.
- [x] **P1 — Un seul submit à valider = grille tout-ou-rien** : 8 tuiles, une erreur → 0 point + pénalité. Ajouter un retour partiel (nombre de tuiles bien placées façon Wordle) pour un flux plus doux.
  - ✅ 2026-09-21 : retour partiel (« x / y in place », tuiles marquées) + pénalité réduite jusqu'à −50 % quand la réponse est presque juste. Pas de points partiels (les points restent réservés aux réponses exactes).
- [x] **P2 — SPEED_BURST** : `burstTimer` de 6 s fixe avec 3 tuiles, et le timeout **retire 3 s** mais ne compte pas comme échec de `totalSubmissions`/`combo` reset (il reset le combo, ok) ; le mode n'apparaît qu'au stage 5 avec 20 % de chance → jamais vu par la majorité. Le rendre plus visible (stage 2–3) ou en faire un mode à part.
  - ✅ 2026-09-21 : apparaît dès le stage 2 (10 %), 15 % aux stages 3-4, 20 % au stage 5 ; limite 8 s → 6 s selon le niveau ; le timeout compte comme une tentative ratée (log, stats, `totalSubmissions`).
- [x] **P2 — Score** : `basePoints * speedFactor * comboFactor` : le `speedFactor` plafonne à 1.5 pour < 10 s et vaut 1.0 après 20 s, donc rapidité peu récompensée ; combo à ×2 max atteint à 10 → plafond bas. `isNewRecord = score > previousHigh && previousHigh > 0` → **le premier score n'est jamais un record**, bien que ce soit compréhensible, l'UX du "premier run" est vide.
  - ✅ 2026-09-21 : vitesse jugée relativement à la taille du puzzle (×1.0–2.0), combo jusqu'à ×3 (à 20) ; le premier score d'une difficulté compte désormais comme record (`score > 0 && score > previousHigh`).
- [x] **P2 — Difficulté = simple durée de timer** (60/45/30 s) : ne change ni le contenu ni les règles. Ajouter des variantes (masquer partiellement les noms, lignes moins connues, sens inverse forcé).
  - ✅ 2026-09-21 : Easy : segments contigus, sens avant jusqu'au stage 2 ; Medium : sens avant au stage 1, ensembles non contigus (30 %) dès le stage 3 ; Hard : sens inversé possible dès le début, ensembles non contigus (60 %) dès le stage 2, 1 hub de plus en CLASSIFY. Non fait : masquage partiel des noms.
- [x] **P2 — Feedback tactile/sonore** : haptique présent au drag, mais pas de vibration succès/échec, aucun son. Le jeu vit de ce feedback (combos).
  - ✅ 2026-09-21 : vibrations succès (`Confirm`) / échec (`Reject`), tic au changement de colonne, réglage « Vibrations » dans Settings. Aucun son (pas d'assets audio).
- [x] **P2 — Le fond `SprintCanvas` à 12 % d'opacité** dessine la ligne en arrière-plan : décoratif, mais peut **donner la réponse** (la géométrie de la ligne et les stations sont visibles derrière). Vérifier que l'ordre spatial ne fuit pas.
  - ✅ 2026-09-21 : les stations de la ligne courante ne sont plus dessinées ; il ne reste que le tracé des segments déjà résolus.
- [x] **P2 — Badges** : seulement `hub_expert` et `night_rider` (22h–4h, condition `hour >= 22 || hour <= 4`) attribués silencieusement, jamais affichés dans le code lu → écran badges absent.
  - ✅ 2026-09-21 : catalogue de 10 badges (`Badges`), attribués en fin de partie, annonce sur l'écran de résultat et écran Progress (débloqués / verrouillés). Ajoutés : combo ×10, niveau 21, séries de 3 et 7 jours, daily, 200 placements, ligne maîtrisée.
- [ ] **P3 — Modes additionnels** (déjà en roadmap) : *Daily challenge* (graine par jour → requiert `Random` seedable, cf. §2), *"Quelle ligne dessert ces stations ?"*, *"Quelle est la prochaine station ?"*, *Trouve le segment manquant*, *Terminus/correspondances*.

---

### Drag & drop des tuiles : ressenti peu réactif (investigation 2026-09-21)

Analyse statique de `TileList` (`SprintScreen.kt` ~l.324-460) et de `SprintViewModel` ; **rien n'a été mesuré sur appareil** (à confirmer avec le Layout Inspector, un compteur de recompositions ou JankStats). Causes probables, par ordre d'impact estimé :

- [x] **P1 — Conflit drag ↔ scroll du `LazyColumn`.** Le `pointerInput { detectDragGestures }` est posé sur le modifier du `LazyColumn`, donc parent du scrollable : le scroll reçoit les événements en premier. Or dès 6 tuiles (stage 3+) la liste déborde sur un téléphone standard (≈ 64 dp par tuile + 100 dp de `contentPadding` bas, ~370 dp utiles, estimation à vérifier) → le geste est tantôt un scroll, tantôt un drag, avec un seuil de slop avant tout retour visuel. *Solution :* `userScrollEnabled = draggedStationId == null`, démarrer le drag sur **appui long court** (`detectDragGesturesAfterLongPress`) ou sur la poignée seule, avec auto-scroll aux bords ; ou supprimer le scroll (hauteur de tuile adaptative pour que la liste tienne toujours).
  - ✅ geste détecté sur la passe `Initial` du `LazyColumn` (avant son scroll) : appui sur la poignée (zone de 80 dp à droite) = drag immédiat ; appui long sur une tuile = drag ; sinon le scroll fonctionne normalement. `userScrollEnabled = false` pendant un drag et les événements du drag sont consommés. À valider au doigt sur appareil.
- [x] **P1 — Pas de `onDragCancel`** : si le geste est annulé (scroll qui prend la main, changement de phase, feedback), `draggedStationId` reste non nul → tuile « collée » en surbrillance, offsets faux. *Solution :* réinitialiser l'état dans `onDragCancel` et quand la phase quitte `Drawing`.
  - ✅ état réinitialisé si le geste est annulé (`finally`), à chaque nouveau défi (`LaunchedEffect(tiles)`) et au début de la cascade de succès.
- [x] **P1 — `animateItem()` appliqué aussi à la tuile déplacée** : à chaque échange elle est réanimée vers son nouvel emplacement pendant qu'on compense `dragOffsetY` à la main → saut / traînée. *Solution :* ne l'appliquer qu'aux autres tuiles (`if (!isDragging) Modifier.animateItem() else Modifier`), ou adopter une lib éprouvée (`sh.calvin.reorderable` : slop, auto-scroll, animations, accessibilité).
  - ✅ `animateItem()` retiré de la tuile glissée uniquement.
- [x] **P1 — Chaque échange passe par le ViewModel et un `StateFlow` global.** `moveTile` ré-émet tout `SprintUiState`, qui est aussi mis à jour toutes les 50 ms par le timer (et toutes les 30 ms en SPEED_BURST) → `SprintScreen` se recompose ~20-30×/s pendant le drag. *Solution :* (a) isoler le temps (`timeLeftMs`, `burstTimer`) dans son propre flux lu seulement par `MetroTimerBar` ; (b) garder l'ordre des tuiles dans un `mutableStateListOf` local à `TileList` pendant le geste et ne le commiter au VM qu'à `onDragEnd` ; (c) marquer `Station`/`SprintUiState` `@Immutable`/`@Stable` (strong skipping est actif avec Kotlin 2.3 mais ne remplace pas (a)).
  - ✅ partiellement fait (point b) : l'ordre est tenu localement pendant le drag et commité une seule fois au relâchement (`SprintViewModel.setTileOrder`, `moveTile` supprimé). Restent (a) isoler le temps dans son propre flux et (c) `@Immutable`.
- [ ] **P2 — Échanges sur une mise en page périmée** : après `onMove`, `layoutInfo` n'est à jour qu'à la frame suivante ; des `onDrag` intermédiaires peuvent recalculer un échange avec des offsets obsolètes (double swap / va-et-vient). *Solution :* cible calculée depuis la position du doigt avec hystérésis (~50 % de la hauteur voisine) et pas de nouveau swap avant la mesure suivante.
- [ ] **P2 — Fin de drag sans transition** : `onDragEnd` remet `draggedStationId = null` sans animer le retour → « téléportation ». *Solution :* `Animatable` sur l'offset de la tuile relâchée, `spring` vers 0 / vers la colonne (CLASSIFY).
- [ ] **P2 — Retour visuel tardif au démarrage** : rien ne change avant le franchissement du touch slop. *Solution :* feedback dès l'appui (scale ~1.03, ombre, haptique `LongPress`), zone de saisie ≥ 48 dp.
- [x] **P2 — CLASSIFY : décalage horizontal trop faible et non animé** (±24 dp, seuil 30 dp au relâché, `translationX` non interpolé) → le joueur ne voit pas s'il a « déposé » la tuile. *Solution :* colonnes de dépôt visibles (gauche / hub / droite) surlignées pendant le drag, snap animé, seuil relatif à la largeur d'écran (voir aussi CLASSIFY plus haut).
  - ✅ 2026-09-21 : colonnes de dépôt visibles et snap animé (voir CLASSIFY hub plus haut).
- [ ] **P2 — `MetroTimerBar` : `animateFloatAsState` relancé toutes les 50 ms** + pulsation infinie sur la même arborescence. *Solution :* alimenter la barre par une valeur non animée (le pas de 50 ms suffit) et la lire dans `drawWithContent`/`graphicsLayer` pour rester hors composition.
- [ ] **P3 — Mesure & non-régression :** Macrobenchmark/JankStats sur « réordonner 8 tuiles » (budget : aucune frame > 16 ms pendant un drag) et test UI (`ComposeTestRule` + `performTouchInput { swipe }`) du réordonnancement.
- [x] **P2 — Mode gaucher** (2026-09-21) : réglage `Left-handed mode` dans un nouvel écran Settings (Home → SETTINGS), stocké en DataStore (`left_handed`). Poignée de drag et zone de saisie passent à gauche, l'icône de direction à droite. Test manuel à faire au doigt.
- [ ] **P3 — Settings à étoffer** : mode gaucher et vibrations existent ; candidats : sons, langue, réinitialiser les scores / la progression.
- [ ] **P3 — Alternative d'accessibilité :** boutons monter/descendre par tuile (TalkBack, précision), utile aussi comme repli sur petits écrans.

## 5. Produit / contenu / croissance

- [ ] **P1 — Qualité et provenance des données** : coordonnées `x/y` "diagramme" (non géographiques) → documenter comment elles ont été produites, source, licence (le README cite Etalab mais le fichier source a changé). Valider : noms de stations officiels CTS, ordre des stations par ligne (fourches de ligne A/D, branches E/F), stations partagées (38 noms sur plusieurs lignes), lignes **G** et **H**, tram-train, extensions récentes (mises à jour de réseau). Script de validation en test unitaire (IDs uniques, ordre, pas de doublons, nombre de stations attendu).
- [ ] **P1 — Cible & promesse** : le jeu est ultra-local (Strasbourg). Définir le public (habitants/étudiants ? touristes ?) et la valeur (apprendre le réseau ? défis entre amis ?). Sans multi-villes, plafond d'audience faible → prioriser l'abstraction ville (`CityDataSource`, README v1.0) tôt, car le modèle actuel (`strasbourg_stations.json` hardcodé dans le repo) est un couplage fort.
- [x] **P2 — Boucle de rétention** : streak + badges existent en base mais peu visibles ; ajouter un daily challenge partageable (résultat texte/emoji façon Wordle) — meilleur levier organique pour un jeu de niche.
  - ✅ 2026-09-21 : **Daily Challenge** (mode `DAILY_SPRINT`, bouton sur l'accueil, une tentative par jour, règles Medium, défis identiques pour tous), streak et niveau visibles sur l'accueil, **partage** texte façon Wordle (🟩🟥⬜) depuis les résultats et l'accueil, bonus de 50 XP. Reste : notifications de rappel, classement.
  - ✅ 2026-09-21 : **format refondu** : 8 questions à difficulté croissante (niveaux 1,4,7…22), une seule tentative par question (l'erreur révèle la solution puis on passe à la suite), pas de compte à rebours (temps écoulé = simple départage), graine = jour + numéro de question, partage `🚇 6/8 · 2:14` + 1 carré par question. À tester à la main sur émulateur/appareil (fin de série, fenêtre de révélation, reprise après pause).
- [x] **P2 — Progression** : XP/niveaux de "connaissance du réseau" (par ligne, % de stations maîtrisées) affichés dans Stats au lieu d'un simple top score.
  - ✅ 2026-09-21 : XP par bonne réponse (10 + 2×stage + combo) + score/100 + bonus daily ; niveaux avec titres (Passenger → Network Master) ; maîtrise par station (3 placements corrects d'affilée) agrégée par ligne dans l'écran Progress.
- [x] **P2 — Stats** : afficher évolution, taux d'erreur par ligne/station, stations les plus ratées (donnée disponible si on journalise les réponses).
  - ✅ 2026-09-21 : Statistiques par station persistées (`station_stats`, Room v3) ; écran Progress : niveau/XP, historique des 20 derniers runs, connaissance par ligne (maîtrisées/vues), stations les plus ratées (≥ 3 essais), badges, meilleurs scores par mode.
- [ ] **P3 — Multi / social** : ghost replays, classement (Firebase) → nécessite anti-triche (score calculé côté client aujourd'hui).
- [~] **P3 — Monétisation** : A (bannière accueil) et B2 (tips Play Billing) **implémentés côté code** (2026-09-21), reste la configuration store : voir `docs/MONETIZATION.md`. Autres options ci-dessous.
- [~] **P3 — Store** : icône adaptative + monochrome faite (Terminus). Reste : captures, fiche Play, politique de confidentialité (obligatoire avec pubs).

---

### Suivi technique de la session 2026-09-21 (Sprint P0-P2 + progression)

- [x] **P1 — Tester la migration Room 1→2→3 sur appareil** (schémas `2.json` et `3.json` exportés ; ajouter un test instrumenté `MigrationTestHelper`). Nouveautés v3 : colonne `game_results.answerLog`, table `station_stats`.
  - ✅ 2026-09-21 : `MigrationTest` instrumenté (5 tests verts sur émulateur API 35) : 1→2 (lignes SPRINT/TRACE, cas où `mode` existe déjà), 2→3, 1→2→3 chaîné, ouverture Room finale + DAOs. v1 recréé en SQL brut (pas de `1.json`). Aucun bug de migration trouvé.
- [ ] **P1 — Valider au doigt** : détection des colonnes CLASSIFY (zone de poignée avec tuile décalée, mode gaucher), drag avec 8-10 tuiles, marques vert/rouge, haptique.
- [ ] **P2 — Retuner l'économie avec des parties réelles** : les modèles de joueur de `SprintEconomyTest` (temps de résolution 2 s + 1.3 s/tuile, précision 75/85/95 %) sont des hypothèses. Journaliser durée par question et taux d'erreur réels.
- [ ] **P2 — Le daily est « une tentative par jour » mais non protégé** (données locales : réinstaller ou effacer les données permet de rejouer). Suffisant sans classement ; à revoir avec un classement en ligne.
- [ ] **P2 — Le daily dépend du jeu de données** : modifier `strasbourg_stations.json` change les défis du jour (l'ordre/ID des stations entre dans la graine). Versionner les données ou figer la graine par version.
- [ ] **P2 — Une statistique par station, pas par ligne** : une correspondance partage les stats entre ses lignes (voulu). Ne distingue pas un sens de parcours ; à affiner si besoin.
- [ ] **P3 — Stats : évolution limitée** aux 20 derniers runs Sprint/daily ; ajouter filtres (mode/difficulté) et graphe de maîtrise dans le temps (nécessite d'historiser `station_stats`).

### Monétisation — options ouvertes (2026-09-21)

Principe retenu : **non intrusif**. Jamais de pub pendant une partie (le chrono et le drag & drop sont le coeur du jeu), rien qui bloque la progression. Piste préférée : **bannière discrète sur l'accueil + don « offre-moi un café / un ticket de tram »**, éventuellement complétés par « supprimer la pub ».

**A. Bannière sur l'écran d'accueil (AdMob)**
- [ ] Bannière adaptative en bas de l'accueil uniquement (pas dans Sprint, ni Results, ni Progress).
- Revenu faible (quelques €/1000 affichages), croît avec le nombre de sessions ; le Daily Challenge fait revenir chaque jour.
- Coûts : SDK Google Mobile Ads (poids, temps de démarrage), formulaire de consentement RGPD/UMP obligatoire (public européen), déclaration « Data safety » Play Store, fiche « pub » dans la politique de confidentialité, pubs de test en debug (jamais en `BuildConfig.DEBUG` réel).
- Alternative sans SDK tiers : encart maison / partenaire local (CTS, offices de tourisme, commerces) — plus de travail commercial, aucun tracking.

**B. Don « Buy me a coffee / a tram ticket »**
- [ ] Écran ou bouton « Soutenir » dans Settings (et sur Results après un bon run, sans pop-up).
- Option B1, lien externe (Ko-fi, Buy Me a Coffee, Liberapay, PayPal.me) : zéro code de facturation, mais **la politique Play interdit de contourner Google Play Billing pour du contenu numérique dans l'app** ; un don sans contrepartie est toléré s'il est clairement présenté comme tel, à vérifier au moment de publier (risque de refus).
- Option B2, achat in-app consommable via Google Play Billing : « Un café » (~2 €), « Un ticket de tram » (~1,60 €, clin d'oeil au tarif CTS), « Un abonnement mensuel » (~10 €). Conforme aux règles Play, commission 15 % (première tranche de 1 M$/an), demande la bibliothèque Play Billing + écran de remerciement.
- Contrepartie symbolique possible (non bloquante) : badge « Supporter », titre spécial, thème de couleur.

**C. Achat unique « Sans pub » / Supporter**
- [ ] Achat non consommable (~2-3 €) : retire la bannière et donne le badge Supporter. Combine A et B2 avec un seul produit Billing. Recommandé si A est retenue.

**D. Contenu premium (plus tard, multi-villes)**
- [ ] Packs de villes (Lyon, Paris, Bordeaux…) payants ou premier pack gratuit + suivants payants. Cohérent avec l'abstraction multi-villes (§5) ; à décider seulement une fois le pilote Strasbourg validé.
- [ ] Modes/entraînements supplémentaires (révision ciblée des stations ratées, statistiques avancées) en « Plus » ; à ne pas coupler au Daily, qui doit rester gratuit.

**E. Pubs récompensées (optionnel, volontaires uniquement)**
- [ ] « Regarder une pub pour une seconde chance / +1 skip ». Plus rentable mais plus intrusif et casse l'équilibre du chrono (à éviter au départ, ou limiter à un usage hors-run).

**F. Partenariats / sponsoring**
- [ ] Mention ou défi sponsorisé (office de tourisme, CTS, événements) ; pas de SDK, revenu incertain mais cohérent avec un jeu local.

**Écartés (trop intrusif)** : interstitiels entre les parties, pubs vidéo forcées, vies/énergie payantes, timers d'attente, bannière pendant le jeu.

**Prérequis techniques communs**
- [ ] Compte développeur Play, politique de confidentialité, écran de consentement RGPD (si pubs), formulaire Data safety.
- [ ] Abstraire derrière une interface `Monetization` (ads on/off, `isSupporter`) stockée dans DataStore pour ne rien coder en dur dans l'UI.
- [ ] Ne rien activer avant que la boucle de rétention (daily, streak) soit validée avec de vrais joueurs : sans audience, la monétisation ne rapporte rien.

**État d'implémentation (A + B2)**
- [x] Bannière AdMob adaptative sur l'accueil uniquement, derrière consentement UMP ; ids de test par défaut, réels via `gradle.properties` (`admobAppId`, `admobBannerId`), kill switch `adsEnabled=false`.
- [x] Tips « café » / « ticket de tram » (consommables Play Billing), section « Support » dans Settings, badge cœur sur l'accueil, un supporter ne voit plus la bannière.
- [x] Bouton « Privacy choices » (Settings) quand UMP l'exige ; `MonetizationPolicy` testé (`MonetizationPolicyTest`).
- [ ] Config store : compte AdMob + message RGPD, produits `support_coffee` / `support_tram_ticket`, Data safety, politique de confidentialité, déclaration de pubs (checklist dans `docs/MONETIZATION.md`).
- [ ] Tester un vrai achat (testeurs de licence) et le refus de consentement ; la bannière met ~20 s à apparaître sur émulateur (aller-retour UMP).
- [ ] Vérification serveur des achats non faite (inutile tant que rien de précieux n'est débloqué).
- [ ] Ajouter éventuellement un rappel discret sur Results après un bon run (non fait, pour rester peu intrusif).

**Recommandation** : A (bannière accueil) + C (achat unique « Sans pub / Supporter », qui sert aussi de « buy me a coffee » conforme Play), avec un lien de don B1 uniquement si la politique Play le permet à la publication. D en phase multi-villes.

### Identité visuelle « Terminus » (2026-09-21)
- [x] Thème (palette, polices Bricolage Grotesque + DM Sans embarquées), pastilles de ligne, tuiles Sprint en plaques blanches (drag : liseré jaune, légère rotation), carte héros Accueil, ticket du défi validé, icône adaptative (+ monochrome).
- [x] Results (carte XP en plaque, badges à liseré jaune, boutons) et Progress (niveau en plaque, maîtrise en pastilles de ligne, graphe) restylés.
- [x] Settings (panneaux à bordure, interrupteurs jaunes, carte Support en ticket) et dialogues (difficulté en plaques, dev) restylés ; descriptions de difficulté réalignées sur l'économie.
- [ ] Reste au style intermédiaire : écran Trace (dev).
- [x] Une partie sans bonne réponse ne compte plus dans la série de jours et affiche « TRY AGAIN ».
- [ ] Fonctionnalités de la maquette non faites : carte de validation hebdomadaire (perforations), cigogne (emblème de niveau/badges), filtre de lignes, sons/haptique signature.
- [x] **Nom de l'app** : « Network Rush », titre Play « Network Rush: Tram Strasbourg » (29 car., limite 30). Pas d'homonyme trouvé sur le Play Store (recherche web, 2026-09-21) ; domaines `.app/.fr/.game/.io` libres, `.com` en vente. Reste à vérifier : recherche directe Play Store, TMview (classes 9/41).
- [x] **`applicationId`** = `com.agsoft.networkrush` (irréversible après publication ; le package Kotlin reste `com.triviamap`).
- [ ] Contraste à vérifier sur tuiles teintées (Classify) avec les couleurs de ligne claires (F, G).

## Checklist avant mise en production (2026-09-21)

**Bloquants**
- [x] **Vérifier le nom « Network Rush »** : Play Store, TMview (classes 9/41), réserver un domaine (`networkrush.app`/`.fr`). `applicationId` fixé : `com.agsoft.networkrush`.
- [x] **targetSdk** : passé de 35 à 36 (2026-09-21). Reste à valider visuellement sur un appareil/émulateur API 36 (edge-to-edge, écran large).
- [x] **Signature release** : ✅ 2026-09-21 : keystore d'upload créé (hors dépôt), `signingConfig` lu depuis `~/.gradle/gradle.properties` (`nrStoreFile/nrStorePassword/nrKeyAlias/nrKeyPassword`), `versionName` 1.0.0 / `versionCode` 1, APK et AAB signés vérifiés (`apksigner`), build R8 lancé sur émulateur sans crash. Reste à l'upload : activer Play App Signing, et garder 2 sauvegardes du keystore ; incrémenter `versionCode` à chaque envoi.
- [~] **Monétisation store** (ids AdMob réels fournis et placés dans `~/.gradle/gradle.properties`, **release uniquement** ; debug garde les ids de test) : compte AdMob + ids réels (`gradle.properties`, jamais commités), message RGPD UMP, produits `support_coffee` / `support_tram_ticket`, test d'achat (testeurs de licence) et du refus de consentement (`docs/MONETIZATION.md`).
- [~] **Conformité Play** : brouillons prêts dans `docs/legal/` (politique de confidentialité fr/en, conditions d'utilisation, attribution des données, réponses Data safety / pubs / classification, fiche fr/en). Reste : remplir les `[PLACEHOLDERS]` (éditeur, adresse, email, date), héberger la politique en HTTPS puis renseigner `res/values/legal.xml` (`privacy_policy_url`, `terms_url`), statut « trader » DSA, captures et visuels, relecture par un juriste.
- [x] **Données Etalab** : source confirmée (jeu « Stations de tram », data.strasbourg.eu, accès le 2026-09-21, mise à jour du jeu le 2026-06-17) ; mention datée dans Settings > Legal et `docs/legal/DATA_ATTRIBUTION.md`. À refaire (date) si le jeu est actualisé. Reste : l'ordre des stations par ligne et les coordonnées schématiques sont notre adaptation (déjà indiquée) ; vérifier l'exactitude des ordres (fourches A/D, E/F) vs la carte CTS.
- [x] **Données** : provenance et licence documentées (Etalab, 2026-09-21). Reste la validation des ordres de stations (fourches A/D, E/F, lignes G/H) vs la carte CTS.
- [x] **Test de migration Room 1→2→3** (`MigrationTestHelper`) : perte de scores = inacceptable en prod.
- [x] **Test sur appareil réel** : drag & drop (CLASSIFY, mode gaucher, 8-10 tuiles), perf, contraste tuiles teintées (lignes F/G).

**Fortement recommandés**
- [x] **i18n en/fr/de** (2026-09-21) : chaînes dans `res/values{,-fr,-de}/strings.xml`, textes des badges/niveaux/pourboires dans `presentation/common/DomainText.kt`, partage localisé, langue par app (Android 13+, `locales_config.xml`). Reste : relecture des traductions par un natif, politique de confidentialité et conditions en allemand, chaînes dev-only (Trace, variantes) laissées en anglais.
- [ ] **Crash reporting** respectueux de la vie privée (§2), au moins Play Console vitals.
- [ ] **Accessibilité minimale** : `contentDescription`, boutons monter/descendre (§4 P3), `fontScale`.
- [ ] **Process death** : l'état de partie est perdu (§1 P1) ; au minimum ne pas crasher au retour.
- [ ] **Test fermé Play** (12 testeurs / 14 jours si compte perso récent) avant la production.

**Peut attendre après la sortie**
- Trace Network, multi-villes, classement en ligne, sons, carte hebdo/cigogne, retuning de l'économie avec de vraies parties, Material 3, `kotlinx.serialization`, ktlint/detekt.

---

## 6. Ordre de traitement suggéré

1. Hygiène repo : `git rm --cached app/build`, supprimer fichiers morts, réécrire README (§1 P0, §2).
2. Fiabiliser le socle : chargement des données/erreurs, timer lifecycle, streak, requête best scores, ajout de tests (§1–§2).
3. Trancher la vision du mode Trace (topologie vs géographie) et refaire son scoring + feedback de fin (§3).
4. Rééquilibrer Sprint : skip, économie du temps, stage 5, CLASSIFY, feedback pédagogique (§4).
5. Extraire la logique de jeu (générateurs/règles) + `Random`/`Clock` injectables → daily challenge.
6. Accessibilité, i18n fr/en, CI, deps, release (proguard).
7. Abstraction multi-villes.

---

## Questions ouvertes

- Trace : jeu de mémoire **topologique** (ordre/connexions) ou **géographique** (forme du tracé) ?
- Public visé : Strasbourgeois qui connaissent déjà le réseau (donc besoin de difficulté) ou apprentissage pour nouveaux arrivants ?
- Le mode Sprint remplace-t-il Trace comme mode principal ?
- Langue(s) du jeu à la sortie ?
