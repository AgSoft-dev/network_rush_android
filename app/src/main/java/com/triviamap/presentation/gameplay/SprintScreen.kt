package com.triviamap.presentation.gameplay

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
import kotlinx.coroutines.withTimeoutOrNull
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.sprint.ChallengeType
import com.triviamap.domain.model.Station
import com.triviamap.presentation.common.*

@Composable
fun SprintScreen(
    @Suppress("UNUSED_PARAMETER") difficulty: Difficulty,
    onFinished: (Int, Float, Int, Int, Boolean, Int) -> Unit,
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
                state.score, 
                phase.breakdown.stationOrder, 
                state.level,
                state.maxCombo,
                state.isNewRecord,
                state.streak
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
                    onBack = onBack,
                    onSkipShortcut = vm::skipQuestion
                )
                MetroTimerBar(
                    timeLeftMs = state.timeLeftMs,
                    difficulty = state.difficulty,
                    combo = state.combo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
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
                            Text("Couldn't load the network data.", color = OnSurface, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = vm::retryLoad, colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = Color.White)) {
                                Text("RETRY")
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally, 
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(
                                    "Stage ${state.stage}",
                                    color = Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                
                                Text(
                                    when (state.challengeType) {
                                        ChallengeType.REORDER -> "Reorder the stations"
                                        ChallengeType.CLASSIFY -> "Sort & Reorder"
                                        ChallengeType.SPEED_BURST -> "RAPID FIRE!"
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
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            val icon = if (state.isForward) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                            Icon(icon, null, tint = Accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (state.isForward) "FOLLOW DIRECTION" else "REVERSE DIRECTION",
                                color = OnSurfaceMed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
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

                        if (state.challengeType == ChallengeType.CLASSIFY) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HeaderLineBadge(state.line?.id ?: "", state.line?.color ?: 0xFF000000L)
                                Text("HUB", color = OnSurfaceMed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                HeaderLineBadge(state.line2?.id ?: "", state.line2?.color ?: 0xFF000000L)
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            TileList(
                                tiles = state.currentTiles,
                                challengeType = state.challengeType,
                                line1 = state.line,
                                line2 = state.line2,
                                isForward = state.isForward,
                                stationSides = state.stationSides,
                                onReorder = vm::setTileOrder,
                                onSideChanged = vm::setStationSide,
                                leftHanded = state.leftHanded,
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
                    combo = state.combo
                )
            }

            if (state.phase is GamePhase.Validating && !state.showFeedback) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("TIME'S UP!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SprintTopBar(score: Int, stage: Int, onBack: () -> Unit, onSkipShortcut: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OnSurface)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("SCORE (STAGE $stage)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnSurfaceMed)
            Text(score.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = OnSurface)
        }
        TextButton(onClick = onSkipShortcut) {
            Icon(Icons.Default.SkipNext, null, tint = OnSurfaceMed, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("SKIP -4s", color = OnSurfaceMed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            backgroundColor = OnSurface.copy(alpha = 0.1f)
        )
    }
}

@Composable
private fun TileList(
    tiles: List<Station>,
    challengeType: ChallengeType,
    line1: com.triviamap.domain.model.TramLine?,
    line2: com.triviamap.domain.model.TramLine?,
    isForward: Boolean,
    stationSides: Map<String, Int>,
    onReorder: (List<Station>) -> Unit,
    onSideChanged: (String, Int) -> Unit,
    leftHanded: Boolean,
    isSuccessState: Boolean
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggedStationId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    // Order is kept locally while dragging (synchronous with the drag offset) and only
    // committed to the ViewModel on release: no StateFlow round-trip per swap.
    var order by remember { mutableStateOf(tiles) }

    val currentTiles by rememberUpdatedState(tiles)
    val currentOnReorder by rememberUpdatedState(onReorder)
    val currentOnSideChanged by rememberUpdatedState(onSideChanged)
    val currentSides by rememberUpdatedState(stationSides)
    val currentType by rememberUpdatedState(challengeType)
    val currentSuccess by rememberUpdatedState(isSuccessState)
    val currentLeftHanded by rememberUpdatedState(leftHanded)

    fun resetDrag() {
        draggedStationId = null
        dragOffsetX = 0f
        dragOffsetY = 0f
    }

    // New challenge (or new order from the ViewModel): drop any gesture in flight and resync
    LaunchedEffect(tiles) {
        resetDrag()
        order = tiles
    }
    // The success cascade must never start with a tile stuck in "dragging" state
    LaunchedEffect(isSuccessState) { if (isSuccessState) resetDrag() }

    fun startDrag(stationId: String) {
        draggedStationId = stationId
        dragOffsetY = 0f
        val side = currentSides[stationId] ?: 0
        dragOffsetX = with(density) { (side * 24f).dp.toPx() }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun dragBy(dx: Float, dy: Float) {
        val stationId = draggedStationId ?: return
        dragOffsetY += dy
        if (currentType == ChallengeType.CLASSIFY) dragOffsetX += dx

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

        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        order = order.toMutableList().apply { add(target.index, removeAt(fromIndex)) }
        dragOffsetY += (dragged.offset - target.offset)
    }

    fun endDrag() {
        val stationId = draggedStationId ?: return
        if (currentType == ChallengeType.CLASSIFY) {
            val thresholdPx = with(density) { 30.dp.toPx() }
            val side = when {
                dragOffsetX < -thresholdPx -> -1
                dragOffsetX > thresholdPx -> 1
                else -> 0
            }
            if (side != (currentSides[stationId] ?: 0)) currentOnSideChanged(stationId, side)
        }
        if (order != currentTiles) currentOnReorder(order)
        resetDrag()
    }

    val l1Color = Color(line1?.color ?: 0xFF000000L)
    val l2Color = Color(line2?.color ?: 0xFF000000L)

    LazyColumn(
        state = listState,
        // A drag must never scroll the list underneath
        userScrollEnabled = draggedStationId == null,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        modifier = Modifier.pointerInput(Unit) {
            // Runs in the Initial pass, i.e. before the list's own scroll gesture:
            //  - touch on the drag handle (right edge, left edge in left-handed mode): drag starts immediately
            //  - long press anywhere on a tile: drag starts
            //  - anything else falls through to normal scrolling
            val handleZonePx = 80.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val hit = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { down.position.y.toInt() in it.offset..(it.offset + it.size) }
                val station = hit?.let { order.getOrNull(it.index) } ?: return@awaitEachGesture
                if (currentSuccess) return@awaitEachGesture

                val onHandle = if (currentLeftHanded) down.position.x <= handleZonePx
                                else down.position.x >= size.width - handleZonePx
                val armed = onHandle || withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
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
            val side = stationSides[station.id] ?: 0
            val isDragging = station.id == draggedStationId

            val cascadeOffset by animateDpAsState(
                targetValue = if (isSuccessState) 800.dp else 0.dp,
                animationSpec = tween(400, delayMillis = index * 60),
                label = "cascade"
            )

            val targetColor = when (side) {
                -1 -> l1Color.copy(alpha = 0.6f)
                1 -> l2Color.copy(alpha = 0.6f)
                else -> Surface
            }
            val tileColor by animateColorAsState(if (isDragging) Primary.copy(alpha = 0.9f) else targetColor, label = "color")

            Card(
                elevation = if (isDragging) 8.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = tileColor,
                border = if (side != 0 && !isDragging) BorderStroke(1.5.dp, if (side == -1) l1Color else l2Color) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = cascadeOffset)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                        translationX = if (isDragging) dragOffsetX else (side * 24f).dp.toPx()
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    // The dragged tile follows the finger: animating its placement would fight the drag offset
                    .then(if (isDragging) Modifier else Modifier.animateItem())
            ) {
                val handle = @Composable {
                    Icon(
                        Icons.Default.DragHandle, null,
                        tint = OnSurfaceMed.copy(alpha = if (isSuccessState) 0f else 0.5f)
                    )
                }
                val direction = @Composable {
                    Icon(
                        imageVector = if (isForward) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null, tint = OnSurface.copy(alpha = 0.1f), modifier = Modifier.size(20.dp)
                    )
                }
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (leftHanded) handle() else direction()
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = station.name, modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold, color = OnSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (leftHanded) direction() else handle()
                }
            }
        }
    }
}

@Composable
private fun SubmitSection(onSubmit: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = Color.White)
        ) {
            Text("CHECK ORDER", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun FeedbackOverlay(isCorrect: Boolean, timeGain: Int, timePenalty: Int, combo: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isCorrect) Success else Error,
        elevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = Color.White, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(if (isCorrect) "CORRECT!" else "WRONG!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
            if (isCorrect && combo >= 3) {
                Text("x$combo COMBO!", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.ExtraBold)
            }
            Text(if (isCorrect) "+${timeGain}s" else "-${timePenalty}s", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ComboBadge(combo: Int) {
    Surface(color = Accent, shape = RoundedCornerShape(12.dp), elevation = 4.dp) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Whatshot, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("x$combo", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HeaderLineBadge(name: String, color: Long) {
    Surface(
        color = Color(color), shape = RoundedCornerShape(6.dp),
        modifier = Modifier.widthIn(min = 40.dp).height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
    }
}
