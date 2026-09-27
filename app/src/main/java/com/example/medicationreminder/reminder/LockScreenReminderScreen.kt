package com.example.medicationreminder.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** A dedicated alarm surface over the keyguard. The gesture never scrolls out of view. */
@Composable
internal fun LockScreenReminderScreen(
    state: ReminderUiState,
    isTest: Boolean,
    onTaken: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onSkipped: (Long) -> Unit,
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var details by remember { mutableStateOf(false) }
    var confirmSkip by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { time = LocalDateTime.now(); delay(1_000) }
    }
    val selected = state.occurrences.firstOrNull { it.eventId == selectedId } ?: state.occurrences.firstOrNull()
    val busy = state.resolvingIds.isNotEmpty()
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().background(Color(0x22020C1E))) {
        val compact = maxHeight < 650.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = Color(0xFFB9D4EE))
                Spacer(Modifier.width(8.dp))
                Text(if (isTest) "安心用药 · 锁屏测试" else "安心用药 · 用药提醒",
                    color = Color(0xFFCEE2F5), style = MaterialTheme.typography.labelLarge)
            }
            Text(time.format(DateTimeFormatter.ofPattern("HH:mm")),
                color = Color.White, fontSize = if (compact) 40.sp else 60.sp, lineHeight = if (compact) 48.sp else 70.sp)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.loading) CircularProgressIndicator(Modifier.padding(24.dp))
                selected?.let { item ->
                    if (!compact) {
                        Surface(Modifier.size(60.dp), shape = CircleShape, color = Color(0xFF274F71)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Medication, null, tint = Color.White) }
                        }
                    }
                    Text(item.medicationName, style = MaterialTheme.typography.headlineMedium,
                        color = Color.White, textAlign = TextAlign.Center)
                    Text(if (isTest) "不记录真实用药" else "本次 ${item.doseLabel}",
                        style = MaterialTheme.typography.titleLarge, color = Color(0xFFBDDFF9), textAlign = TextAlign.Center)
                    if (item.foodRestrictions.isNotBlank()) Text("饮食注意：${item.foodRestrictions}",
                        color = Color(0xFFFFDDB5), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    TextButton(onClick = { details = true }) { Text("查看用药说明与其他操作", color = Color(0xFFD3E6FA)) }
                    if (state.occurrences.size > 1) {
                        val index = state.occurrences.indexOf(item)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(enabled = !busy, onClick = {
                                selectedId = state.occurrences[(index - 1 + state.occurrences.size) % state.occurrences.size].eventId
                            }) { Text("上一项") }
                            Text("${index + 1} / ${state.occurrences.size}", color = Color.White)
                            TextButton(enabled = !busy, onClick = {
                                selectedId = state.occurrences[(index + 1) % state.occurrences.size].eventId
                            }) { Text("下一项") }
                        }
                    }
                }
                state.errorMessage?.let {
                    Text(it, color = Color(0xFFFFBCB5), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
            }
            selected?.let { item ->
                // Keying prevents an in-progress drag from confirming a newly selected medicine.
                key(item.eventId) {
                    SwipeDoseControl(!busy, item.medicationName,
                        { onTaken(item.eventId) }, { onSnooze(item.eventId) }, lockScreenMode = true, compact = compact)
                }
            }
        }
    }
    selected?.let { item ->
        if (details) AlertDialog(
            onDismissRequest = { details = false },
            title = { Text(item.medicationName) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isTest) Text("锁屏滑动演练，不记录真实用药。")
                    else Text("本次药量：${item.doseLabel}")
                    if (item.route.isNotBlank()) Text("服用方式：${item.route}")
                    if (item.instructions.isNotBlank()) Text("重要提醒：${item.instructions}")
                    if (item.foodRestrictions.isNotBlank()) Text("饮食注意：${item.foodRestrictions}")
                    Button(enabled = !busy, onClick = { details = false; onTaken(item.eventId) }) { Text("已经服用") }
                    OutlinedButton(enabled = !busy, onClick = { details = false; onSnooze(item.eventId) }) { Text("稍后提醒") }
                    TextButton(enabled = !busy, onClick = { details = false; confirmSkip = true }) { Text(if (isTest) "结束测试" else "跳过本次") }
                }
            },
            confirmButton = { TextButton(onClick = { details = false }) { Text("返回滑动界面") } },
        )
        if (confirmSkip) AlertDialog(
            onDismissRequest = { confirmSkip = false },
            title = { Text(if (isTest) "结束测试？" else "跳过本次用药？") },
            text = { Text(if (isTest) "不会改变真实用药记录。" else "${item.medicationName}，${item.doseLabel}。如果暂时不方便，请选择稍后提醒。") },
            confirmButton = { TextButton(enabled = !busy, onClick = { confirmSkip = false; onSkipped(item.eventId) }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { confirmSkip = false }) { Text("取消") } },
        )
    }
}
