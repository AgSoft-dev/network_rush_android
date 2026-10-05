package com.triviamap.presentation.gameplay

import com.triviamap.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.lerp
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.triviamap.domain.sprint.Side
import com.triviamap.util.ClassifyLayout
import com.triviamap.util.TileSizing
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import kotlinx.coroutines.withTimeoutOrNull
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.GameMode
import com.triviamap.domain.sprint.Direction
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.model.Station
import com.triviamap.domain.sprint.SprintRules
import com.triviamap.presentation.common.*

@Composable
fun SprintScreen(
    @Suppress("UNUSED_PARAMETER") difficulty: Difficulty,
    onFinished: (GameMode, Int) -> Unit,
    onBack: () -> Unit,
    vm: SprintViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    // Freeze the run whenever the screen leaves the foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> vm.pause()
                Lifecycle.Event.ON_START -> vm.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.phase) {
        val phase = state.phase
        if (phase is GamePhase.Finished) {
            onFinished(
                if (state.isDaily) GameMode.DAILY_SPRINT else GameMode.STATION_SPRINT,
                phase.breakdown.total
            )
        }
    }

    // Success / failure vibration (one per feedback)
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.feedbackTrigger) {
        if (state.feedbackTrigger > 0 && state.hapticsEnabled) {
            haptic.performHapticFeedback(
                if (state.isCorrectFeedback) HapticFeedbackType.Confirm else HapticFeedbackType.Reject
            )
        }
    }

    Scaffold(
        backgroundColor = Background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                SprintTopBar(
                    score = state.score,
                    stage = state.stage,
                    skipsLeft = state.skipsLeft,
                    isDaily = state.isDaily,
                    dailyIndex = state.dailyIndex,
                    elapsedMs = state.elapsedMs,
                    onBack = onBack,
                    onSkipShortcut = vm::skipQuestion
                )
                if (state.isDaily) {
                    LinearProgressIndicator(
                        progress = state.dailyIndex / SprintRules.DAILY_QUESTIONS.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Accent,
                        backgroundColor = Surface
                    )
                } else {
                    MetroTimerBar(
                        timeLeftMs = state.timeLeftMs,
                        difficulty = state.difficulty,
                        combo = state.combo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (state.phase is GamePhase.Drawing) {
                SubmitSection(onSubmit = vm::submit)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Background Map with cumulative session connections
            if (state.line != null && state.bounds != null) {
                SprintCanvas(
                    line = state.line!!,
                    bounds = state.bounds!!,
                    targetStation = null,
                    visitedStations = emptyList(),
                    sessionStations = state.sessionStations,
                    difficulty = state.difficulty,
                    onTap = {},
                    modifier = Modifier.fillMaxSize().alpha(0.12f)
                )
            }

            when (state.phase) {
                GamePhase.Loading -> {
                    if (state.loadFailed) {
                        Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.sprint_load_failed), color = OnSurface, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = vm::retryLoad, colors = ButtonDefaults.buttonColors(backgroundColor = Sun, contentColor = Ink)) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    } else {
                        CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)
                    }
                }
                GamePhase.Drawing, GamePhase.Validating -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(
                                    stringResource(R.string.sprint_stage, state.stage),
                                    color = Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    when (state.challengeType) {
                                        ChallengeType.REORDER -> stringResource(R.string.type_reorder)
                                        ChallengeType.CLASSIFY -> stringResource(R.string.type_classify)
                                        ChallengeType.SPEED_BURST -> stringResource(R.string.type_burst)
                                    },
                                    color = OnSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(
                                visible = state.combo >= 2,
                                enter = slideInHorizontally { it } + fadeIn(),
                                exit = slideOutHorizontally { it } + fadeOut(),
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                ComboBadge(state.combo)
                            }
                        }
                        
                        if (state.challengeType == ChallengeType.CLASSIFY) {
                            ClassifyHeader(state.line, state.line2, state.directions)
                        } else {
                            DirectionHints(state.directions)
                        }

                        if (state.challengeType == ChallengeType.SPEED_BURST) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = 1f - state.burstTimer,
                                modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).clip(CircleShape),
                                color = Accent,
                                backgroundColor = OnSurface.copy(alpha = 0.1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            TileList(
                                tiles = state.currentTiles,
                                challengeType = state.challengeType,
                                line1 = state.line,
                                line2 = state.line2,
                                stationSides = state.stationSides,
                                onReorder = vm::setTileOrder,
                                onSideChanged = vm::setStationSide,
                                hapticsEnabled = state.hapticsEnabled,
                                misplacedIds = state.misplacedIds,
                                isSuccessState = state.showFeedback && state.isCorrectFeedback
                            )
                        }
                    }
                }
                else -> {}
            }
            
            androidx.compose.animation.AnimatedVisibility(
                visible = state.showFeedback,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 1.2f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                FeedbackOverlay(
                    isCorrect = state.isCorrectFeedback,
                    timeGain = state.lastTimeGain,
                    timePenalty = state.lastTimePenalty,
                    showTime = !state.isDaily,
                    placedCount = state.placedCount,
                    tileCount = state.tileCount,
                    combo = state.combo
                )
            }

            if (state.phase is GamePhase.Validating && !state.showFeedback) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.times_up), color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SprintTopBar(score: Int, stage: Int, skipsLeft: Int, isDaily: Boolean, dailyIndex: Int, elapsedMs: Long, onBack: () -> Unit, onSkipShortcut: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface)
        }
        Column(modifier = Modifier.weight(1f)) {
            if (isDaily) {
                Text(stringResource(R.string.daily_question, dailyIndex + 1, SprintRules.DAILY_QUESTIONS), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnSurfaceMed)
                Text(formatDuration(elapsedMs), fontFamily = DisplayFont, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            } else {
                Text("SCORE (STAGE $stage)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnSurfaceMed)
                Text(score.toString(), fontFamily = DisplayFont, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = OnSurface)
            }
        }
        if (!isDaily) {
            TextButton(onClick = onSkipShortcut, enabled = skipsLeft > 0) {
                Icon(Icons.Default.SkipNext, null, tint = OnSurfaceMed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.skip, skipsLeft), color = OnSurfaceMed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetroTimerBar(
    timeLeftMs: Long,
    difficulty: Difficulty,
    combo: Int,
    modifier: Modifier = Modifier
) {
    val totalTimeMs = when(difficulty) {
        Difficulty.EASY -> 60_000L
        Difficulty.MEDIUM -> 45_000L
        else -> 30_000L
    }.toFloat()

    val progress by animateFloatAsState(targetValue = (timeLeftMs / totalTimeMs).coerceIn(0f, 1f), label = "timer")
    val isLowTime = timeLeftMs < 10000
    
    val baseColor = when {
        combo >= 10 -> Color(0xFFFFD700)
        combo >= 5 -> Color(0xFF00BFFF)
        else -> Accent
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (isLowTime) 1.06f else 1.0f,
        animationSpec = if (isLowTime) infiniteRepeatable(tween(500), RepeatMode.Reverse) else spring(),
        label = "pulse"
    )

    val color by animateColorAsState(if (isLowTime) Error else baseColor, label = "color")

    Box(modifier = modifier.graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            backgroundColor = Surface
        )
    }
}

/** "Line A → Illkirch": which way the player must order the stations. */
/**
 * Classify header: each line's badge with its direction right under it, "hub" in the middle.
 * Replaces the stacked direction rows + badge row to leave more height to the tiles.
 */
@Composable
private fun ClassifyHeader(line1: com.triviamap.domain.model.TramLine?, line2: com.triviamap.domain.model.TramLine?, directions: List<Direction>) {
    @Composable
    fun Column1(line: com.triviamap.domain.model.TramLine?, alignment: Alignment.Horizontal, modifier: Modifier) {
        val direction = directions.firstOrNull { it.lineId == line?.id }
        Column(horizontalAlignment = alignment, modifier = modifier) {
            HeaderLineBadge(line?.id ?: "", line?.color ?: 0xFF000000L)
            if (direction != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Accent, modifier = Modifier.size(12.dp))
                    Text(
                        stringResource(R.string.toward, direction.toward),
                        color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column1(line1, Alignment.Start, Modifier.weight(1f))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp).widthIn(max = 96.dp)
        ) {
            Text(stringResource(R.string.hub), color = OnSurfaceMed, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(
                stringResource(R.string.first_on_top), color = OnSurfaceMed, fontSize = 9.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 11.sp, maxLines = 2
            )
        }
        Column1(line2, Alignment.End, Modifier.weight(1f))
    }
}

@Composable
private fun DirectionHints(directions: List<Direction>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
        directions.forEach { d ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                LinePill(d.lineId, Color(d.color), size = 22.dp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ArrowDownward, null, tint = Accent, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.toward, d.toward),
                    color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            stringResource(R.string.first_on_top),
            color = OnSurfaceMed, fontSize = 10.sp
        )
    }
}

private const val LIST_BOTTOM_PADDING_DP = 8f
private const val HINT_HEIGHT_DP = 22f

@Composable
private fun TileList(
    tiles: List<Station>,
    challengeType: ChallengeType,
    line1: com.triviamap.domain.model.TramLine?,
    line2: com.triviamap.domain.model.TramLine?,
    stationSides: Map<String, Int>,
    onReorder: (List<Station>) -> Unit,
    onSideChanged: (String, Int) -> Unit,
    hapticsEnabled: Boolean,
    misplacedIds: Set<String>,
    isSuccessState: Boolean
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isClassify = challengeType == ChallengeType.CLASSIFY

    var draggedStationId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    // Horizontal offset of the dragged tile from the centre column (CLASSIFY)
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    // Order and sides are kept locally while dragging (synchronous with the drag offset)
    // and only committed to the ViewModel on release.
    var order by remember { mutableStateOf(tiles) }
    var sides by remember { mutableStateOf(stationSides) }

    val currentTiles by rememberUpdatedState(tiles)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnSideChanged by rememberUpdatedState(onSideChanged)
    val currentType by rememberUpdatedState(challengeType)
    val currentSuccess by rememberUpdatedState(isSuccessState)
    // True when the list overflows and scrolls: a drag must then be armed by a long press
    val currentHaptics by rememberUpdatedState(hapticsEnabled)

    fun buzz(type: HapticFeedbackType) { if (currentHaptics) haptic.performHapticFeedback(type) }

    fun resetDrag() { draggedStationId = null }

    // New challenge (or new order from the ViewModel): drop any gesture in flight and resync
    LaunchedEffect(tiles) {
        resetDrag()
        order = tiles
        sides = stationSides
    }
    LaunchedEffect(stationSides) { if (draggedStationId == null) sides = stationSides }
    // The success cascade must never start with a tile stuck in "dragging" state
    LaunchedEffect(isSuccessState) { if (isSuccessState) resetDrag() }

    val l1Color = Color(line1?.color ?: 0xFF000000L)
    val l2Color = Color(line2?.color ?: 0xFF000000L)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val listWidthPx = with(density) { maxWidth.toPx() }
        // CLASSIFY: tiles are 62 % wide and slide between three columns; otherwise full width
        val tileFraction = if (isClassify) 0.62f else 1f
        val tileWidthPx = listWidthPx * tileFraction
        // Keep outer columns clear of Android's edge-swipe back-gesture zone (see ClassifyLayout):
        // flush against the screen edge, a horizontal drag there gets stolen by the system.
        val edgeMarginPx = with(density) { ClassifyLayout.DEFAULT_EDGE_MARGIN_DP.dp.toPx() }
        val shiftPx = ClassifyLayout.shiftPx(listWidthPx, tileFraction, edgeMarginPx)
        // The gesture handler below is created once (pointerInput(Unit)): it must read the geometry
        // through these, never the locals, or it keeps the values of the first puzzle (e.g. a Reorder
        // with shiftPx = 0) and Classify columns can never be reached.
        val currentTileWidthPx by rememberUpdatedState(tileWidthPx)
        val currentShiftPx by rememberUpdatedState(shiftPx)
        // Tiles shrink (64 -> 48 dp) so the whole puzzle fits without scrolling; scroll is a last resort
        val sizing = TileSizing.layout(maxHeight.value - LIST_BOTTOM_PADDING_DP, order.size)
        val currentScrolls by rememberUpdatedState(sizing.scrolls)
        val compactText = sizing.tileDp < 58f

        fun sideForOffset(x: Float): Int = ClassifyLayout.sideForOffset(x, currentShiftPx)

        fun startDrag(stationId: String) {
            draggedStationId = stationId
            dragOffsetY = 0f
            dragOffsetX = (sides[stationId] ?: 0) * currentShiftPx
            buzz(HapticFeedbackType.LongPress)
        }

        fun dragBy(dx: Float, dy: Float) {
            val stationId = draggedStationId ?: return
            dragOffsetY += dy
            if (currentType == ChallengeType.CLASSIFY) {
                val newX = (dragOffsetX + dx).coerceIn(-currentShiftPx, currentShiftPx)
                if (sideForOffset(newX) != sideForOffset(dragOffsetX)) buzz(HapticFeedbackType.SegmentTick)
                dragOffsetX = newX
            }

            val fromIndex = order.indexOfFirst { it.id == stationId }
            if (fromIndex == -1) return
            val info = listState.layoutInfo
            val dragged = info.visibleItemsInfo.firstOrNull { it.key == stationId } ?: return
            // Layout not yet re-measured after our last swap: wait for it, offsets would be stale
            if (dragged.index != fromIndex) return

            val centerY = dragged.offset + dragOffsetY + dragged.size / 2
            val target = info.visibleItemsInfo.firstOrNull { item ->
                item.index != fromIndex && centerY.toInt() in item.offset..(item.offset + item.size)
            } ?: return

            buzz(HapticFeedbackType.TextHandleMove)
            order = order.toMutableList().apply { add(target.index, removeAt(fromIndex)) }
            dragOffsetY += (dragged.offset - target.offset)
        }

        fun endDrag() {
            val stationId = draggedStationId ?: return
            if (currentType == ChallengeType.CLASSIFY) {
                val side = sideForOffset(dragOffsetX)
                if (side != (sides[stationId] ?: 0)) {
                    sides = sides + (stationId to side)
                    currentOnSideChanged(stationId, side)
                }
            }
            if (order != currentTiles) currentOnReorder(order)
            // dragOffsetX is kept: the tile animates from where it was released
            resetDrag()
        }

        val previewSide = if (draggedStationId != null && isClassify) sideForOffset(dragOffsetX) else null

        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (!isClassify) return@drawBehind
                    val colColors = listOf(l1Color, Color.White, l2Color)
                    for (i in 0..2) {
                        val side = i - 1
                        val left = (size.width - tileWidthPx) / 2f + side * shiftPx
                        val active = previewSide == side
                        drawRoundRect(
                            color = colColors[i].copy(alpha = if (active) 0.22f else 0.05f),
                            topLeft = Offset(left, 0f),
                            size = Size(tileWidthPx, size.height),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                }
        )

        LazyColumn(
            state = listState,
            // A drag must never scroll the list underneath
            userScrollEnabled = sizing.scrolls && draggedStationId == null,
            verticalArrangement = Arrangement.spacedBy(sizing.gapDp.dp),
            // Room for the plate edge drawn under the last tile
            // Room for the long-press hint above the first tile when the list scrolls
            contentPadding = PaddingValues(top = if (sizing.scrolls) HINT_HEIGHT_DP.dp else 0.dp, bottom = LIST_BOTTOM_PADDING_DP.dp),
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                // Runs in the Initial pass, i.e. before the list's own scroll gesture:
                //  - the list fits (no scroll): touching a tile anywhere starts the drag immediately
                //  - the list scrolls: a long press on a tile starts the drag, a plain swipe scrolls
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val hit = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { down.position.y.toInt() in it.offset..(it.offset + it.size) }
                    val station = hit?.let { order.getOrNull(it.index) } ?: return@awaitEachGesture
                    if (currentSuccess) return@awaitEachGesture

                    val spanStart = (size.width - currentTileWidthPx) / 2f + (sides[station.id] ?: 0) * currentShiftPx
                    val spanEnd = spanStart + currentTileWidthPx
                    if (down.position.x < spanStart || down.position.x > spanEnd) return@awaitEachGesture
                    val armed = !currentScrolls || withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                .firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                            if (!change.pressed) return@withTimeoutOrNull false
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                return@withTimeoutOrNull false
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        true
                    } == null
                    if (!armed) return@awaitEachGesture

                    startDrag(station.id)
                    try {
                        down.consume()
                        while (true) {
                            val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                .firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) { change.consume(); endDrag(); break }
                            val delta = change.positionChange()
                            change.consume()
                            dragBy(delta.x, delta.y)
                        }
                    } finally {
                        // Cancelled (composition change, pointer lost): never leave a tile stuck
                        if (draggedStationId != null) resetDrag()
                    }
                }
            }
        ) {
            itemsIndexed(order, key = { _, s -> s.id }) { index, station ->
                val side = sides[station.id] ?: 0
                val isDragging = station.id == draggedStationId

                val cascadeOffset by animateDpAsState(
                    targetValue = if (isSuccessState) 800.dp else 0.dp,
                    animationSpec = tween(400, delayMillis = index * 60),
                    label = "cascade"
                )

                // Horizontal slide between columns, starting from where the tile was released
                val slideX = remember { Animatable(side * shiftPx) }
                var wasDragging by remember { mutableStateOf(false) }
                LaunchedEffect(isDragging, side, shiftPx) {
                    if (isDragging) {
                        wasDragging = true
                    } else {
                        if (wasDragging) { slideX.snapTo(dragOffsetX); wasDragging = false }
                        slideX.animateTo(side * shiftPx, spring(stiffness = Spring.StiffnessMedium))
                    }
                }

                val checked = misplacedIds.isNotEmpty()
                val isWrong = station.id in misplacedIds
                val targetColor = when (side) {
                    -1 -> lerp(Plate, l1Color, 0.3f)
                    1 -> lerp(Plate, l2Color, 0.3f)
                    else -> Plate
                }
                val tileColor by animateColorAsState(if (isDragging) Plate else targetColor, label = "color")

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Card(
                        elevation = if (isDragging) 12.dp else 0.dp,
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = tileColor,
                        border = when {
                            isDragging -> BorderStroke(3.dp, Sun)
                            checked -> BorderStroke(3.dp, if (isWrong) Error else Success)
                            side != 0 && !isDragging -> BorderStroke(1.5.dp, if (side == -1) l1Color else l2Color)
                            else -> null
                        },
                        modifier = Modifier
                            .fillMaxWidth(tileFraction)
                            .offset(x = cascadeOffset)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                rotationZ = if (isDragging) -2f else 0f
                                translationX = if (isDragging) dragOffsetX else slideX.value
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            // Signage plate "thickness"
                            .drawBehind {
                                if (!isDragging) drawRoundRect(
                                    PlateEdge, topLeft = Offset(0f, 4.dp.toPx()), size = size,
                                    cornerRadius = CornerRadius(18.dp.toPx())
                                )
                            }
                            // The dragged tile follows the finger: animating its placement would fight the drag offset
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                    ) {
                        // Purely decorative: the whole tile is the drag target
                        val handle = @Composable {
                            Icon(
                                Icons.Default.DragHandle, null,
                                tint = Ink.copy(alpha = if (isSuccessState) 0f else 0.45f)
                            )
                        }
                        Row(modifier = Modifier.height(sizing.tileDp.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = station.name, modifier = Modifier.weight(1f),
                                fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = if (compactText) 16.sp else 18.sp, color = Ink,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            handle()
                        }
                    }
                }
            }
        }

        if (sizing.scrolls && !isSuccessState) {
            Text(
                text = stringResource(R.string.drag_hint_hold),
                modifier = Modifier.align(Alignment.TopCenter).height(HINT_HEIGHT_DP.dp),
                color = OnSurfaceMed, fontSize = 12.sp, letterSpacing = 1.sp, maxLines = 1
            )
        }
    }
}

@Composable
private fun SubmitSection(onSubmit: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().navigationBarsSafePadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Sun, contentColor = Ink)
        ) {
            Text(stringResource(R.string.check_order), fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun FeedbackOverlay(isCorrect: Boolean, timeGain: Int, timePenalty: Int, showTime: Boolean, placedCount: Int, tileCount: Int, combo: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isCorrect) Success else Error,
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(if (isCorrect) stringResource(R.string.correct) else stringResource(R.string.wrong), color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            if (!isCorrect && tileCount > 0) {
                Text(stringResource(R.string.in_place, placedCount, tileCount), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
            }
            if (isCorrect && combo >= 3) {
                Text(stringResource(R.string.combo, combo), color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.ExtraBold)
            }
            if (showTime) {
                Text(if (isCorrect) "+${timeGain}s" else "-${timePenalty}s", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ComboBadge(combo: Int) {
    Surface(color = Sun, shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Whatshot, null, tint = Ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("x$combo", color = Ink, fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HeaderLineBadge(name: String, color: Long) {
    LinePill(name, Color(color), size = 36.dp)
}
