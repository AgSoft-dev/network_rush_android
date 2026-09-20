# TriviaMap — Revue technique & gameplay

Revue du 2026-09-20 sur `master` (v0.1.0, POC). Statut : `[ ]` à faire · `[~]` en cours · `[x]` fait.
Priorités : **P0** bug bloquant / perte de données · **P1** important · **P2** confort · **P3** idée.
Références au format `fichier:ligne` quand pertinent (lignes indicatives, à re-vérifier).

> **Périmètre actuel (2026-09-20)** : on se concentre sur **Station Sprint**. **Trace Network** est parké : bouton visible uniquement en build debug (`BuildConfig.DEBUG`, libellé "TRACE NETWORK (DEV)"), ses items restent listés (§3) pour la prochaine étape.
> **Avancement §1** : P0 et P1 Sprint traités (compilation debug + release/R8 OK, **aucun test exécuté** ni sur appareil). Restent §1 : `Accessibilité`, `i18n`, et les items marqués ⏸.


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
- [ ] **P2 — Chaînes en dur** (aucune i18n) : `strings.xml` ne contient que `app_name`. Le jeu cible Strasbourg → français probable ; textes actuels en anglais. Extraire en ressources (fr + en).
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
  - ⏸ Reste : targetSdk 36 (lint `OldTargetApi`) à tester visuellement (edge-to-edge), migration Material 3, `kotlinx.serialization` à la place de Gson (le ⏸ précédent ci-dessous est obsolète).