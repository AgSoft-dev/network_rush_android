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

- [ ] **P1 — Aucun test** (`app/src/test` et `androidTest` inexistants). Le code le plus critique est pur et testable : `ScoringEngine`, `GeometryEngine`, `GeoJsonParser`, génération des défis Sprint, streak. → tests JUnit + Turbine (Flows) ; commencer par : LCS/ordre, `getStage`, générateurs (invariants : `correctOrder` cohérent, tailles, unicité), streak sur plusieurs dates (injecter un `Clock`).
- [ ] **P1 — Logique de jeu dans les ViewModels** (`SprintViewModel` ~490 lignes) : génération de défis, règles de score, timers, badges, persistance, tout dans une classe. → extraire `ChallengeGenerator`, `SprintRules` (score/temps/pénalités) purs + injection d'un `Random` seedable et d'un `Clock`. Prérequis du daily challenge (§4) et des tests.
- [ ] **P1 — Code mort du moteur de tracé** : `simplifyPolyline` (RDP), `discreteFrechetDistance` (récursif → **StackOverflow** sur ≥ ~5000 points, doit être itératif), `TramLine.geometry` scoring, `pathAccuracyScore` toujours à `0f`… Soit on ré-intègre la précision (§3), soit on supprime pour ne pas laisser croire que c'est actif. Le `ScoreBreakdown` de Trace ignore `playerPaths` (param `playerPaths` inutilisé dans `computeGlobal`).
- [ ] **P1 — README désynchronisé** : décrit poids 30/35/25/10, snap 150 m, Fréchet 800 m, `LineSelectionScreen`, `StatsViewModel`, pipeline GeoJSON lon/lat + Etalab… Le code : coordonnées **schématiques** (x∈[353,937], y∈[270,740], pas de lon/lat), snap `30.0` unités, poids 50/40/10 · 60/30/10 · 60/20/20, données curées à la main. Réécrire ; source/licence des données à clarifier (provenance des coordonnées "diagramme" ?).
- [ ] **P2 — `Repositories.kt` dupliqué** (`data/repository` et `domain/repository` portent le même nom de fichier) + plusieurs classes par fichier (`UseCases.kt`, `Models.kt`). Renommer par classe.
- [ ] **P2 — Use cases anémiques** (`GetLineUseCase`, `GetLineResultsUseCase` : simples délégations, `GetLineUseCase` non utilisé). SprintViewModel injecte **directement** les repositories en plus des use cases → couche domain incohérente. Choisir : soit repos partout, soit use cases partout.
- [ ] **P2 — `TramLineRepositoryImpl`** : `MutableStateFlow(emptyList())` comme "loading state" est ambigu (vide = pas chargé ou pas de données ?). Modéliser `Loading/Loaded/Error`.
- [ ] **P2 — `GeoBounds.from` : padding 10% / 25% "pour l'UI" codé dans la util géométrique** → mélange couche présentation/util ; `toNormalized` divise par zéro si une ligne est parfaitement horizontale/verticale (bounds nulles).
- [ ] **P2 — Ratio d'aspect déformé** : la projection normalise x et y indépendamment sur `size.width/height` du canvas → le réseau est **étiré** selon l'écran (distances et angles non conservés). Utiliser un scale uniforme (fit-center) ; impacte aussi le rayon de snap (30 unités ≠ même distance en px selon l'axe).
- [ ] **P2 — Dépendances datées** : Kotlin 1.9.23, Compose BOM 2024.05, AGP 8.13.2 (décalage avec Kotlin/Gradle), `compileSdk/targetSdk 34` (Play exige 35 depuis 2025-08), Gson (maintenance mode), Material 2 (roadmap M3 abandonnée en pratique). Planifier bump : Kotlin 2.x + plugin Compose compiler, BOM récent, targetSdk 35/36, `kotlinx.serialization`.
- [ ] **P2 — Pas de CI / lint** : ajouter GitHub Actions (`./gradlew lintDebug testDebugUnitTest assembleDebug`), ktlint/detekt.
- [ ] **P2 — Logging / analytics / crash reporting** : rien. Au minimum Crashlytics (ou équivalent respectueux de la vie privée) avant beta.
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

- [ ] **P0 — Pas de vraie condition de défaite ni de courbe de difficulté finie** : la seule fin est le timer à 0 ; skip gratuit (cf. §1) + gains de temps 8–12 s pour ≤ 8 tuiles → un joueur régulier peut jouer indéfiniment. Ajouter : skip coûteux (−temps, −combo, quota limité), et/ou décroissance du gain de temps avec le niveau (au-delà du stage 5, "else" = stage 5 permanent, plus aucune montée).
- [ ] **P0 — Stage 5 infini à difficulté constante** : `getStage` plafonne à 5 dès le niveau 21 ; count 7–8 constant, gain de temps `coerceAtLeast(4000)`. Ajouter une montée continue (moins de temps, plus de tuiles, distracteurs, lignes moins connues) pour qu'une partie ait un plafond de compétence.
- [ ] **P1 — Les gains/pénalités de temps créent une boucle "positive"** : gain (≥ 4 s + combo jusqu'à +5 s + 5 s classify) vs pénalité (5–9 s) ; à partir de ~5 réponses justes le joueur est net positif quelle que soit la difficulté (plafond `maxTimeMs` de 30–60 s seulement). Simuler l'économie (script) : temps moyen de résolution par type × gains, cible ~ 60–90 s de survie pour un joueur moyen.
- [ ] **P1 — CLASSIFY : l'"ordre correct" est mal défini** (cf. §1) : trier des stations de deux lignes différentes par `minOf(idx1, idx2)` n'a pas de sens géographique/pédagogique (l'index 3 de la ligne A n'est pas "avant" l'index 5 de la ligne D). Le joueur ne peut pas déduire l'ordre → frustration/aléatoire. Séparer : (1) classer gauche/droite/hub, (2) ordonner **dans chaque colonne**.
- [ ] **P1 — CLASSIFY : ambiguïté hub** : une station "hub" (commune aux deux lignes) est correcte uniquement au centre ; mais l'UI ne montre que trois offsets de 24 dp (translationX) : très faible affordance (les tuiles bougent à peine), seuil 30 dp au relâché. Zones de dépôt visibles nécessaires (colonnes colorées).
- [ ] **P1 — Direction "FOLLOW / REVERSE" mal expliquée** : une flèche ↓/↑ minuscule ; l'ordre "reverse" est la source principale d'échecs involontaires. Afficher les **terminus** ("de Graffenstaden vers Parc des Sports") au lieu d'une flèche.
- [x] **P1 — Stage 1 filtre "stations à correspondance + terminus"** : n'a pas de sens si `Station.lines` ne contient qu'**une** ligne (le parser met `listOf(lineId)` "stations are per line") → `s.lines.size > 1` est **toujours faux**, le filtre ne garde que les 2 terminus → `availableStations.size <= count` → tuiles = 2 stations, jamais 3–4. À vérifier / corriger (dériver `lines` en croisant les IDs partagés entre lignes).
  - ✅ le parser dérive maintenant `Station.lines` des ids partagés entre lignes (les hubs sont détectés) ; repli sur la ligne complète si < 3 stations. À vérifier en jeu : les tuiles du stage 1 = hubs + terminus, pas forcément contigus.
- [ ] **P1 — Répétition des défis** : `allLines.random()` sans mémoire → mêmes lignes/segments de suite, aucune progression de "connaissance". Ajouter un tirage pondéré par erreurs passées (répétition espacée), et éviter les doublons consécutifs.
- [ ] **P1 — Pas de feedback pédagogique** : sur une mauvaise réponse → "WRONG! −5s" (et le libellé `-5s` est **codé en dur** alors que la pénalité est 5–9 s), pas de correction affichée. Montrer la bonne réponse (ou au moins quelles tuiles étaient mal placées) : c'est ce qui fait apprendre le réseau.
- [ ] **P1 — Un seul submit à valider = grille tout-ou-rien** : 8 tuiles, une erreur → 0 point + pénalité. Ajouter un retour partiel (nombre de tuiles bien placées façon Wordle) pour un flux plus doux.
- [ ] **P2 — SPEED_BURST** : `burstTimer` de 6 s fixe avec 3 tuiles, et le timeout **retire 3 s** mais ne compte pas comme échec de `totalSubmissions`/`combo` reset (il reset le combo, ok) ; le mode n'apparaît qu'au stage 5 avec 20 % de chance → jamais vu par la majorité. Le rendre plus visible (stage 2–3) ou en faire un mode à part.
- [ ] **P2 — Score** : `basePoints * speedFactor * comboFactor` : le `speedFactor` plafonne à 1.5 pour < 10 s et vaut 1.0 après 20 s, donc rapidité peu récompensée ; combo à ×2 max atteint à 10 → plafond bas. `isNewRecord = score > previousHigh && previousHigh > 0` → **le premier score n'est jamais un record**, bien que ce soit compréhensible, l'UX du "premier run" est vide.
- [ ] **P2 — Difficulté = simple durée de timer** (60/45/30 s) : ne change ni le contenu ni les règles. Ajouter des variantes (masquer partiellement les noms, lignes moins connues, sens inverse forcé).
- [ ] **P2 — Feedback tactile/sonore** : haptique présent au drag, mais pas de vibration succès/échec, aucun son. Le jeu vit de ce feedback (combos).
- [ ] **P2 — Le fond `SprintCanvas` à 12 % d'opacité** dessine la ligne en arrière-plan : décoratif, mais peut **donner la réponse** (la géométrie de la ligne et les stations sont visibles derrière). Vérifier que l'ordre spatial ne fuit pas.
- [ ] **P2 — Badges** : seulement `hub_expert` et `night_rider` (22h–4h, condition `hour >= 22 || hour <= 4`) attribués silencieusement, jamais affichés dans le code lu → écran badges absent.
- [ ] **P3 — Modes additionnels** (déjà en roadmap) : *Daily challenge* (graine par jour → requiert `Random` seedable, cf. §2), *"Quelle ligne dessert ces stations ?"*, *"Quelle est la prochaine station ?"*, *Trouve le segment manquant*, *Terminus/correspondances*.

---

## 5. Produit / contenu / croissance

- [ ] **P1 — Qualité et provenance des données** : coordonnées `x/y` "diagramme" (non géographiques) → documenter comment elles ont été produites, source, licence (le README cite Etalab mais le fichier source a changé). Valider : noms de stations officiels CTS, ordre des stations par ligne (fourches de ligne A/D, branches E/F), stations partagées (38 noms sur plusieurs lignes), lignes **G** et **H**, tram-train, extensions récentes (mises à jour de réseau). Script de validation en test unitaire (IDs uniques, ordre, pas de doublons, nombre de stations attendu).
- [ ] **P1 — Cible & promesse** : le jeu est ultra-local (Strasbourg). Définir le public (habitants/étudiants ? touristes ?) et la valeur (apprendre le réseau ? défis entre amis ?). Sans multi-villes, plafond d'audience faible → prioriser l'abstraction ville (`CityDataSource`, README v1.0) tôt, car le modèle actuel (`strasbourg_stations.json` hardcodé dans le repo) est un couplage fort.
- [ ] **P2 — Boucle de rétention** : streak + badges existent en base mais peu visibles ; ajouter un daily challenge partageable (résultat texte/emoji façon Wordle) — meilleur levier organique pour un jeu de niche.
- [ ] **P2 — Progression** : XP/niveaux de "connaissance du réseau" (par ligne, % de stations maîtrisées) affichés dans Stats au lieu d'un simple top score.
- [ ] **P2 — Stats** : afficher évolution, taux d'erreur par ligne/station, stations les plus ratées (donnée disponible si on journalise les réponses).
- [ ] **P3 — Multi / social** : ghost replays, classement (Firebase) → nécessite anti-triche (score calculé côté client aujourd'hui).
- [ ] **P3 — Monétisation** : non abordée ; pas d'impact technique avant la beta.
- [ ] **P3 — Store** : icône (`ic_launcher.xml` unique, pas d'adaptive icon ni monochrome), captures, politique de confidentialité si analytics.

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
