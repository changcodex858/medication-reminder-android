package com.example.medicationreminder.reminder

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal enum class SwipeDoseAction { TAKEN, SNOOZE }

internal fun swipeDoseAction(offset: Float, threshold: Float): SwipeDoseAction? = when {
    offset <= -threshold -> SwipeDoseAction.TAKEN
    offset >= threshold -> SwipeDoseAction.SNOOZE
    else -> null
}

@Composable
fun SwipeDoseControl(
    enabled: Boolean,
    medicationName: String,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
) {
    var drag by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { 65.dp.toPx() }
    val limit = with(LocalDensity.current) { 85.dp.toPx() }
    val feedback = LocalHapticFeedback.current
    val taken by rememberUpdatedState(onTaken)
    val snooze by rememberUpdatedState(onSnooze)
    val position by animateFloatAsState(drag, label = "swipePosition")
    val action = swipeDoseAction(drag, threshold)
    val accent = when (action) {
        SwipeDoseAction.TAKEN -> Color(0xFF28765E)
        SwipeDoseAction.SNOOZE -> Color(0xFF306BAB)
        null -> MaterialTheme.colorScheme.primary
    }
    LaunchedEffect(action) {
        if (action != null) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("按住圆球，上下滑动后松手", style = MaterialTheme.typography.labelLarge)
        Box(
            Modifier.fillMaxWidth().height(280.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(100.dp).height(260.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f), RoundedCornerShape(60.dp)))
            Text("↑  已经服用", Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
            Text("↓  稍后提醒", Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
            Box(
                modifier = Modifier.size(86.dp)
                    .graphicsLayer { translationY = position; shadowElevation = 12.dp.toPx(); shape = CircleShape; clip = false }
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.72f), accent)), CircleShape)
                    .border(3.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                    .semantics {
                        contentDescription = "$medicationName，按住上滑已经服用，下滑稍后提醒"
                        if (enabled) customActions = listOf(
                            CustomAccessibilityAction("已经服用") { taken(); true },
                            CustomAccessibilityAction("稍后提醒") { snooze(); true },
                        )
                    }
                    .pointerInput(enabled) {
                        if (enabled) detectDragGesturesAfterLongPress(
                            onDragStart = { holding = true; feedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                            onDragCancel = { drag = 0f; holding = false },
                            onDragEnd = {
                                val resolved = swipeDoseAction(drag, threshold)
                                drag = 0f
                                holding = false
                                when (resolved) {
                                    SwipeDoseAction.TAKEN -> taken()
                                    SwipeDoseAction.SNOOZE -> snooze()
                                    null -> Unit
                                }
                            },
                            onDrag = { change, amount -> change.consume(); drag = (drag + amount.y).coerceIn(-limit, limit) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (!enabled) CircularProgressIndicator(Modifier.size(28.dp), color = Color.White)
                else Icon(when (action) {
                    SwipeDoseAction.TAKEN -> Icons.Default.Check
                    SwipeDoseAction.SNOOZE -> Icons.Default.Snooze
                    null -> Icons.Default.TouchApp
                }, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        Text(when {
            !enabled -> "正在确认…"
            action == SwipeDoseAction.TAKEN -> "松手确认：已经服用"
            action == SwipeDoseAction.SNOOZE -> "松手确认：稍后提醒"
            holding -> "继续向上或向下滑动"
            else -> "服用后再上滑确认，也可以使用下方按钮"
        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
