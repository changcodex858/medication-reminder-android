package com.example.medicationreminder.reminder

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medicationreminder.appGraph
import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.ui.theme.MedicationReminderTheme
import com.example.medicationreminder.ui.theme.BrandSageDeep

class ReminderActivity : ComponentActivity() {
    private val eventIds: List<Long> by lazy {
        intent.getLongArrayExtra(AlarmPlaybackService.EXTRA_EVENT_IDS)?.toList().orEmpty()
    }

    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModel.Factory(
            eventIds = eventIds,
            repository = appGraph.medicationRepository,
            preferencesRepository = appGraph.preferencesRepository,
            coordinator = appGraph.alarmCoordinator,
            applicationContext = applicationContext,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showAboveLockScreen()
        setContent {
            MedicationReminderTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(state.completed) {
                    if (state.completed) {
                        AlarmPlaybackService.refresh(this@ReminderActivity)
                        finish()
                    }
                }
                ReminderAlarmScreen(
                    state = state,
                    onTaken = viewModel::markTaken,
                    onSnooze = viewModel::snooze,
                    onSkipped = viewModel::markSkipped,
                )
            }
        }
    }

    private fun showAboveLockScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }
}

@Composable
private fun ReminderAlarmScreen(
    state: ReminderUiState,
    onTaken: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onSkipped: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            contentPadding = PaddingValues(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            "安心用药 · 语音提醒",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = CircleShape,
                        color = BrandSageDeep,
                        shadowElevation = 8.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Medication,
                                contentDescription = null,
                                modifier = Modifier.size(38.dp),
                                tint = Color.White,
                            )
                        }
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("该按时用药了", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "别着急，先核对药名、药量和注意事项",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (state.loading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                state.errorMessage?.let { message ->
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { liveRegion = LiveRegionMode.Assertive },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.32f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    message,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
                items(state.occurrences, key = { it.eventId }) { occurrence ->
                    ReminderCard(
                        item = occurrence,
                        isSubmitting = occurrence.eventId in state.resolvingIds,
                        onTaken = onTaken,
                        onSnooze = onSnooze,
                        onSkipped = onSkipped,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    item: DoseOccurrence,
    isSubmitting: Boolean,
    onTaken: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onSkipped: (Long) -> Unit,
) {
    var showSkipConfirmation by remember(item.eventId) { mutableStateOf(false) }
    var pendingAction by remember(item.eventId) { mutableStateOf<String?>(null) }
    val spokenDose = item.doseLabel.ifBlank { "请按医嘱核对药量" }

    LaunchedEffect(isSubmitting) {
        if (!isSubmitting) pendingAction = null
    }

    if (showSkipConfirmation) {
        AlertDialog(
            onDismissRequest = { showSkipConfirmation = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            },
            title = { Text("确定跳过这次用药吗？") },
            text = {
                Text(
                    "${item.medicationName}，$spokenDose，将被记录为“已跳过”。如果只是暂时不方便，建议选择“稍后提醒”。",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSkipConfirmation = false
                        pendingAction = "skip"
                        onSkipped(item.eventId)
                    },
                ) {
                    Text("确认跳过")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipConfirmation = false }) {
                    Text("返回")
                }
            },
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(24.dp),
            ),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Medication,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "现在需要确认",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Text(
                item.medicationName,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.doseLabel.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Medication,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = BrandSageDeep,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "本次药量",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                item.doseLabel,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }
            ReminderTakenButton(
                item = item,
                spokenDose = spokenDose,
                pending = pendingAction == "taken",
                enabled = !isSubmitting,
                onClick = {
                    pendingAction = "taken"
                    onTaken(item.eventId)
                },
            )
            if (item.route.isNotBlank()) {
                ReminderDetail(Icons.Default.Info, "服用方式", item.route)
            }
            if (item.instructions.isNotBlank()) {
                ReminderDetail(Icons.Default.Info, "重要提醒", item.instructions)
            }
            if (item.foodRestrictions.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(21.dp),
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "饮食注意",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                item.foodRestrictions,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    pendingAction = "snooze"
                    onSnooze(item.eventId)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .semantics {
                        contentDescription = "将${item.medicationName}，$spokenDose，设置为稍后提醒"
                    },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                enabled = !isSubmitting,
                shape = RoundedCornerShape(18.dp),
            ) {
                if (pendingAction == "snooze") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Snooze, null)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (pendingAction == "snooze") "正在设置…" else "稍后提醒",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(
                onClick = { showSkipConfirmation = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .semantics {
                        contentDescription = "跳过${item.medicationName}，$spokenDose，本次用药"
                    },
                enabled = !isSubmitting,
            ) {
                Text(
                    if (pendingAction == "skip") "正在更新…" else "跳过本次",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ReminderTakenButton(
    item: DoseOccurrence,
    spokenDose: String,
    pending: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .semantics {
                contentDescription = "将${item.medicationName}，$spokenDose，记录为已经服用"
            },
        contentPadding = PaddingValues(16.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
    ) {
        if (pending) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(Icons.Default.CheckCircle, null)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (pending) "正在记录…" else "已经服用",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ReminderDetail(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(
                value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
