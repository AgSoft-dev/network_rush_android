package com.triviamap.presentation.gameplay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.triviamap.domain.model.Difficulty
import com.triviamap.domain.model.Station
import com.triviamap.presentation.common.*
import kotlinx.coroutines.delay

@Composable
fun SprintScreen(
    @Suppress("UNUSED_PARAMETER") difficulty: Difficulty,
    onFinished: (Int) -> Unit,
    onBack: () -> Unit,
    vm: SprintViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(state.phase) {
        if (state.phase is GamePhase.Finished) {
            onFinished(state.score)
        }
    }

    Scaffold(
        backgroundColor = Background,
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                SprintTopBar(
                    score = state.score,
                    level = state.level,
                    feedbackTrigger = state.feedbackTrigger,
                    isCorrect = state.isCorrectFeedback,
                    onBack = onBack
                )
                MetroTimerBar(
                    timeLeftMs = state.timeLeftMs,
                    difficulty = state.difficulty,
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
            when (state.phase) {
                GamePhase.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Primary)
                }
                GamePhase.Drawing, GamePhase.Validating -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Level ${state.level}",
                            color = Primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        
                        Text(
                            if (state.challengeType == ChallengeType.REORDER) "Reorder the stations" else "Sort & Reorder",
                            color = OnSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
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
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (state.challengeType == ChallengeType.CLASSIFY) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HeaderLineBadge(state.line?.name ?: "", state.line?.color ?: 0xFF000000)
                                Text("HUB", color = OnSurfaceMed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                HeaderLineBadge(state.line2?.name ?: "", state.line2?.color ?: 0xFF000000)
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            // Watermark direction arrow
                            Icon(
                                if (state.isForward) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = OnSurface.copy(alpha = 0.03f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(48.dp)
                            )

                            TileList(
                                tiles = state.currentTiles,
                                challengeType = state.challengeType,
                                line1 = state.line,
                                line2 = state.line2,
                                isForward = state.isForward,
                                stationSides = state.stationSides,
                                onMove = vm::moveTile,
                                onSideChanged = vm::setStationSide
                            )
                        }
                    }
                }
                else -> {}
            }
            
            FeedbackOverlay(
                show = state.showFeedback,
                isCorrect = state.isCorrectFeedback,
                timeGain = state.lastTimeGain
            )

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
private fun HeaderLineBadge(name: String, color: Long) {
    Surface(
        color = Color(color),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.widthIn(min = 60.dp).height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = name, 
                color = Color.White, 
                fontWeight = FontWeight.Black, 
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetroTimerBar(
    timeLeftMs: Long,
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    val totalTimeMs = when(difficulty) {
        Difficulty.EASY -> 60_000L
        Difficulty.MEDIUM -> 45_000L
        Difficulty.HARD -> 30_000L
    }.toFloat()

    val progress by animateFloatAsState(
        targetValue = (timeLeftMs / totalTimeMs).coerceIn(0f, 1f),
        animationSpec = tween(100, easing = LinearEasing),
        label = "timer_progress"
    )
    
    val isLowTime = timeLeftMs < 5000
    val color by animateColorAsState(
        if (isLowTime) Error else Accent,
        animationSpec = if (isLowTime) infiniteRepeatable(tween(400), RepeatMode.Reverse) else tween(400),
        label = "timer_color"
    )

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val strokeWidth = 6.dp.toPx()
            val stationRadius = 5.dp.toPx()
            
            drawLine(
                color = OnSurfaceMed.copy(alpha = 0.1f),
                start = Offset(stationRadius, centerY),
                end = Offset(width - stationRadius, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            
            val activeWidth = (width - 2 * stationRadius) * progress
            if (progress > 0) {
                drawLine(
                    color = color,
                    start = Offset(stationRadius, centerY),
                    end = Offset(stationRadius + activeWidth, centerY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            
            val stationCount = 5
            for (i in 0..stationCount) {
                val x = stationRadius + ((width - 2 * stationRadius) / stationCount) * i
                val isReached = x <= (stationRadius + activeWidth) + 1f
                
                drawCircle(
                    color = if (isReached) color else OnSurfaceMed.copy(alpha = 0.2f),
                    radius = stationRadius,
                    center = Offset(x, centerY)
                )
                
                drawCircle(
                    color = if (isReached) Color.White else Background,
                    radius = stationRadius * 0.4f,
                    center = Offset(x, centerY)
                )
            }
        }
    }
}

@Composable
private fun FeedbackOverlay(show: Boolean, isCorrect: Boolean, timeGain: Int) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        exit = fadeOut() + scaleOut(targetScale = 1.5f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                elevation = 12.dp,
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val scale by animateFloatAsState(
                        targetValue = if (show) 1.1f else 1f,
                        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse)
                    )
                    
                    Text(
                        if (isCorrect) "PERFECT!" else "WRONG!",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                    
                    if (isCorrect) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("+$timeGain SECONDS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SprintTopBar(
    score: Int, 
    level: Int, 
    feedbackTrigger: Int,
    isCorrect: Boolean,
    onBack: () -> Unit
) {
    var scoreScale by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(feedbackTrigger) {
        if (feedbackTrigger > 0 && isCorrect) {
            scoreScale = 1.3f
            delay(100)
            scoreScale = 1f
        }
    }
    val animatedScoreScale by animateFloatAsState(
        targetValue = scoreScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)
    )

    TopAppBar(
        backgroundColor = Background,
        elevation = 0.dp,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = OnSurface)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.graphicsLayer(scaleX = animatedScoreScale, scaleY = animatedScoreScale)) {
                    Text("Sprint - Lvl $level", color = OnSurface, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("SCORE: $score", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileList(
    tiles: List<Station>,
    challengeType: ChallengeType,
    line1: com.triviamap.domain.model.TramLine?,
    line2: com.triviamap.domain.model.TramLine?,
    isForward: Boolean,
    stationSides: Map<String, Int>,
    onMove: (Int, Int) -> Unit,
    onSideChanged: (String, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    var draggedStationId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    
    // Stable references to prevent gesture cancellation on question transitions
    val currentTiles by rememberUpdatedState(tiles)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnSideChanged by rememberUpdatedState(onSideChanged)
    val currentSides by rememberUpdatedState(stationSides)
    val currentChallengeType by rememberUpdatedState(challengeType)

    val l1Color = Color(line1?.color ?: 0xFF000000)
    val l2Color = Color(line2?.color ?: 0xFF000000)

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp), 
        contentPadding = PaddingValues(bottom = 60.dp),
        modifier = Modifier.pointerInput(Unit) { // Stable key is critical
            detectDragGestures(
                onDragStart = { offset ->
                    val item = listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { item ->
                            offset.y.toInt() in item.offset..(item.offset + item.size)
                        }
                    
                    item?.let { 
                        val station = currentTiles.getOrNull(it.index)
                        if (station != null) {
                            draggedStationId = station.id
                            dragOffsetY = 0f
                            // Initialize horizontal offset based on current snap to prevent initial jump
                            val currentSide = currentSides[station.id] ?: 0
                            dragOffsetX = with(density) { (currentSide * 24f).dp.toPx() }
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val stationId = draggedStationId ?: return@detectDragGestures
                    dragOffsetY += dragAmount.y
                    
                    // Always read the latest challengeType from the updated state
                    if (currentChallengeType == ChallengeType.CLASSIFY) {
                        dragOffsetX += dragAmount.x
                    }

                    // Find index reliably from ID to fix "wrong indexing" bug
                    val fromIndex = currentTiles.indexOfFirst { it.id == stationId }
                    if (fromIndex == -1) return@detectDragGestures

                    val info = listState.layoutInfo
                    // Use stable key to find item info instead of stale indices
                    val draggedItemInfo = info.visibleItemsInfo.firstOrNull { it.key == stationId } 
                        ?: return@detectDragGestures

                    // Vertical Reordering
                    val draggedCenterY = draggedItemInfo.offset + dragOffsetY + draggedItemInfo.size / 2
                    val target = info.visibleItemsInfo.firstOrNull { item ->
                        item.index != fromIndex &&
                                draggedCenterY.toInt() in item.offset..(item.offset + item.size)
                    }

                    if (target != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentOnMove(fromIndex, target.index)
                        
                        // Vertical reconciliation to prevent jittering
                        dragOffsetY += (draggedItemInfo.offset - target.offset)
                    }
                },
                onDragEnd = {
                    val stationId = draggedStationId
                    if (stationId != null && currentChallengeType == ChallengeType.CLASSIFY) {
                        // Use density-independent threshold
                        val thresholdPx = with(density) { 30.dp.toPx() }
                        val side = when {
                            dragOffsetX < -thresholdPx -> -1
                            dragOffsetX > thresholdPx -> 1
                            else -> 0
                        }
                        if (side != (currentSides[stationId] ?: 0)) {
                            currentOnSideChanged(stationId, side)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                    draggedStationId = null
                    dragOffsetY = 0f
                    dragOffsetX = 0f
                },
                onDragCancel = {
                    draggedStationId = null
                    dragOffsetY = 0f
                    dragOffsetX = 0f
                }
            )
        }
    ) {
        itemsIndexed(tiles, key = { _, station -> station.id }) { index, station ->
            val isDragging = station.id == draggedStationId
            val side = stationSides[station.id] ?: 0
            
            val animatedScale by animateFloatAsState(if (isDragging) 1.04f else 1f)
            
            // Background logic: Half/Half Gradient for center/hubs
            val halfHalfBrush = Brush.horizontalGradient(listOf(l1Color.copy(alpha = 0.5f), l2Color.copy(alpha = 0.5f)))
            
            val targetColor = when (side) {
                -1 -> l1Color.copy(alpha = 0.6f)
                1 -> l2Color.copy(alpha = 0.6f)
                else -> Color.Transparent
            }
            val animatedBackgroundColor by animateColorAsState(if (isDragging) Primary.copy(alpha = 0.9f) else targetColor)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                        translationX = if (isDragging) dragOffsetX else (side * 24f).dp.toPx()
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .let { if (!isDragging) it.animateItemPlacement() else it },
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                elevation = if (isDragging) 8.dp else 2.dp,
                border = if (side != 0 && !isDragging) androidx.compose.foundation.BorderStroke(1.5.dp, if (side == -1) l1Color else l2Color) else null
            ) {
                Box(
                    modifier = Modifier
                        .background(if (side == 0 && !isDragging) halfHalfBrush else SolidColor(animatedBackgroundColor))
                        .padding(horizontal = 12.dp, vertical = 10.dp), // Thinner tiles
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 40.dp)
                            .size(24.dp)
                            .alpha(0.05f),
                        tint = if (isDragging) Color.White else OnSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Small square line badge for CLASSIFY
                        if (challengeType == ChallengeType.CLASSIFY) {
                            Surface(
                                color = l1Color,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(22.dp).alpha(if (side == -1) 1f else 0.3f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(line1?.name?.take(1) ?: "", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (isDragging) Color.White.copy(alpha = 0.2f) else Background, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontWeight = FontWeight.Black,
                                color = if (isDragging) Color.White else OnSurfaceMed,
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(Modifier.width(10.dp))
                        
                        Text(
                            text = station.name,
                            modifier = Modifier.weight(1f),
                            color = if (isDragging || side != 0) Color.White else OnSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = if (challengeType == ChallengeType.CLASSIFY) {
                                when(side) {
                                    -1 -> TextAlign.Start
                                    1 -> TextAlign.End
                                    else -> TextAlign.Center
                                }
                            } else TextAlign.Start
                        )

                        if (challengeType == ChallengeType.CLASSIFY) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                color = l2Color,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.size(22.dp).alpha(if (side == 1) 1f else 0.3f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(line2?.name?.take(1) ?: "", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = null,
                                tint = if (isDragging || side != 0) Color.White.copy(alpha = 0.6f) else OnSurfaceMed.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitSection(onSubmit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Background,
        elevation = 16.dp
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Primary, contentColor = Color.White)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("SUBMIT ORDER", fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 15.sp)
        }
    }
}

private fun formatTimeLeft(ms: Long): String {
    val seconds = (ms / 1000).toInt()
    val deciseconds = (ms % 1000 / 100).toInt()
    return "$seconds.$deciseconds"
}
