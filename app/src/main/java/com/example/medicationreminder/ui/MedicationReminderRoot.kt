package com.example.medicationreminder.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.domain.DoseStatus
import com.example.medicationreminder.domain.MedicationDraft
import com.example.medicationreminder.domain.MedicationPlan
import com.example.medicationreminder.domain.ReminderVoiceStyle
import com.example.medicationreminder.domain.UserPreferences
import com.example.medicationreminder.reminder.AlarmPlaybackService
import com.example.medicationreminder.ui.theme.BrandSage
import com.example.medicationreminder.ui.theme.BrandSageDeep
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class MainTab(val label: String) {
    TODAY("今天"),
    MEDICATIONS("用药计划"),
    SETTINGS("设置"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationReminderRoot(viewModel: AppViewModel) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val today by viewModel.todayOccurrences.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSavingMedication by viewModel.isSavingMedication.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableStateOf(MainTab.TODAY) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedOccurrenceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var resolvingOccurrenceId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (editorOpen) {
        val plan = editingId?.let { id -> plans.firstOrNull { it.id == id } }
        MedicationEditorScreen(
            plan = plan,
            isSaving = isSavingMedication,
            snackbarHostState = snackbarHostState,
            onCancel = {
                editorOpen = false
                editingId = null
            },
            onSave = { draft ->
                viewModel.saveMedication(draft) {
                    editorOpen = false
                    editingId = null
                    if (!health.voiceReminderReady) tab = MainTab.SETTINGS
                }
            },
        )
        return
    }

    selectedOccurrenceId?.let { eventId ->
        DoseDetailScreen(
            item = today.firstOrNull { it.eventId == eventId },
            snoozeMinutes = preferences.defaultSnoozeMinutes,
            isSubmitting = resolvingOccurrenceId != null,
            snackbarHostState = snackbarHostState,
            onBack = {
                if (resolvingOccurrenceId == null) selectedOccurrenceId = null
            },
            onTaken = {
                if (resolvingOccurrenceId == null) {
                    resolvingOccurrenceId = eventId
                    viewModel.markOccurrenceTaken(eventId) { success ->
                        resolvingOccurrenceId = null
                        if (success) selectedOccurrenceId = null
                    }
                }
            },
            onSnooze = {
                if (resolvingOccurrenceId == null) {
                    resolvingOccurrenceId = eventId
                    viewModel.snoozeOccurrence(eventId) { success ->
                        resolvingOccurrenceId = null
                        if (success) selectedOccurrenceId = null
                    }
                }
            },
            onSkipped = {
                if (resolvingOccurrenceId == null) {
                    resolvingOccurrenceId = eventId
                    viewModel.skipOccurrence(eventId) { success ->
                        resolvingOccurrenceId = null
                        if (success) selectedOccurrenceId = null
                    }
                }
            },
        )
        return
    }

    Scaffold(
        modifier = Modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val largeText = LocalDensity.current.fontScale >= 1.35f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = MaterialTheme.shapes.small,
                            color = BrandSageDeep,
                            shadowElevation = 3.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            if (!largeText) {
                                Text(
                                    "安心用药",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                tab.label,
                                style = if (largeText) {
                                    MaterialTheme.typography.titleMedium
                                } else {
                                    MaterialTheme.typography.titleLarge
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(24.dp),
                color = BrandSageDeep,
                shadowElevation = 12.dp,
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    MainTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        MainTab.TODAY -> Icons.Default.Home
                                        MainTab.MEDICATIONS -> Icons.Default.Medication
                                        MainTab.SETTINGS -> Icons.Default.SettingsIcon
                                    },
                                    contentDescription = null,
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color.White.copy(alpha = 0.84f),
                                unselectedTextColor = Color.White.copy(alpha = 0.84f),
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab == MainTab.MEDICATIONS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedOccurrenceId = null
                        editingId = null
                        editorOpen = true
                    },
                    modifier = Modifier.heightIn(min = 56.dp),
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("添加药品") },
                    shape = RoundedCornerShape(18.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                )
            }
        },
    ) { padding ->
        when (tab) {
            MainTab.TODAY -> TodayScreen(
                items = today,
                health = health,
                modifier = Modifier.padding(padding),
                onOpenSettings = { tab = MainTab.SETTINGS },
                onOpenDose = { eventId ->
                    if (resolvingOccurrenceId == null) selectedOccurrenceId = eventId
                },
                onTaken = { eventId ->
                    if (resolvingOccurrenceId == null) {
                        resolvingOccurrenceId = eventId
                        viewModel.markOccurrenceTaken(eventId) {
                            resolvingOccurrenceId = null
                        }
                    }
                },
                resolvingOccurrenceId = resolvingOccurrenceId,
            )
            MainTab.MEDICATIONS -> MedicationListScreen(
                plans = plans.filter { it.enabled },
                zodiacCovers = preferences.zodiacCovers,
                onZodiacChanged = viewModel::setZodiacCover,
                modifier = Modifier.padding(padding),
                onEdit = {
                    selectedOccurrenceId = null
                    editingId = it
                    editorOpen = true
                },
                onArchive = viewModel::archiveMedication,
            )
            MainTab.SETTINGS -> SettingsScreen(
                health = health,
                preferences = preferences,
                modifier = Modifier.padding(padding),
                onRefresh = viewModel::refreshHealth,
                onTest = viewModel::scheduleTestReminder,
                onPreview = viewModel::previewVoice,
                onSnoozeChanged = viewModel::setSnoozeMinutes,
                onRepeatChanged = viewModel::setRepeatSeconds,
                onVoiceStyleChanged = viewModel::setVoiceStyle,
                onSpeechRateChanged = viewModel::setSpeechRate,
                onSpeechPitchChanged = viewModel::setSpeechPitch,
                onSpeechVolumeChanged = viewModel::setSpeechVolume,
            )
        }
    }
}

@Composable
private fun TodayScreen(
    items: List<DoseOccurrence>,
    health: SystemHealth,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit,
    onOpenDose: (Long) -> Unit,
    onTaken: (Long) -> Unit,
    resolvingOccurrenceId: Long?,
) {
    val completed = items.count { it.status == DoseStatus.TAKEN }
    val ringingItems = items.filter { it.status == DoseStatus.RINGING }
    val otherItems = items.filterNot { it.status == DoseStatus.RINGING }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            TodayOverviewCard(items = items, completed = completed)
        }

        if (ringingItems.isNotEmpty()) {
            item { SectionTitle("正在提醒", "请先核对药名、药量和注意事项") }
            items(ringingItems, key = { it.eventId }) { occurrence ->
                TodayDoseCard(
                    item = occurrence,
                    isSubmitting = resolvingOccurrenceId == occurrence.eventId,
                    actionsEnabled = resolvingOccurrenceId == null,
                    onClick = { onOpenDose(occurrence.eventId) },
                    onTaken = { onTaken(occurrence.eventId) },
                )
            }
        }

        item {
            ReliabilityBanner(
                health = health,
                onOpenSettings = onOpenSettings,
            )
        }

        if (items.isEmpty()) {
            item {
                EmptyState(
                    title = "今天还没有用药任务",
                    detail = "去“用药计划”添加药品和提醒时间吧。",
                )
            }
        } else if (otherItems.isNotEmpty()) {
            item {
                SectionTitle(
                    if (ringingItems.isEmpty()) "今天的用药" else "今天的其他用药",
                    "按时间核对药名与药量",
                )
            }
            items(otherItems, key = { it.eventId }) { occurrence ->
                TodayDoseCard(
                    item = occurrence,
                    isSubmitting = resolvingOccurrenceId == occurrence.eventId,
                    actionsEnabled = resolvingOccurrenceId == null,
                    onClick = { onOpenDose(occurrence.eventId) },
                    onTaken = { onTaken(occurrence.eventId) },
                )
            }
        }
    }
}

@Composable
private fun TodayOverviewCard(
    items: List<DoseOccurrence>,
    completed: Int,
) {
    val active = items.firstOrNull { it.status == DoseStatus.RINGING }
        ?: items.firstOrNull { it.status == DoseStatus.SNOOZED }
        ?: items.firstOrNull { it.status == DoseStatus.PENDING }
    val progress = if (items.isEmpty()) 0f else completed.toFloat() / items.size
    val nextText = when (active?.status) {
        DoseStatus.RINGING -> "正在提醒"
        DoseStatus.SNOOZED -> active.snoozedUntil?.let(::formatNextReminder) ?: "稍后提醒"
        DoseStatus.PENDING -> active.scheduledAt.let(::formatNextReminder)
        else -> if (items.isEmpty()) "暂无安排" else "今日已完成"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = BrandSageDeep,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(BrandSageDeep, BrandSage),
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            LocalDate.now().format(DAY_FORMAT),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.90f),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "今天也要安心用药",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                        )
                    }
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White,
                            )
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stacked = maxWidth < 290.dp || LocalDensity.current.fontScale >= 1.45f
                    if (stacked) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            TodayOverviewMetric(
                                label = "今日进度",
                                value = if (items.isEmpty()) "—" else "$completed / ${items.size}",
                                prominent = true,
                            )
                            TodayOverviewMetric(label = "下一项", value = nextText)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) {
                                TodayOverviewMetric(
                                    label = "今日进度",
                                    value = if (items.isEmpty()) "—" else "$completed / ${items.size}",
                                    prominent = true,
                                )
                            }
                            Box(modifier = Modifier.weight(1.25f)) {
                                TodayOverviewMetric(label = "下一项", value = nextText)
                            }
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = Color(0xFFFFC2AE),
                    trackColor = Color.White.copy(alpha = 0.18f),
                )
            }
        }
    }
}

@Composable
private fun TodayOverviewMetric(label: String, value: String, prominent: Boolean = false) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.90f),
        )
        Text(
            value,
            style = if (prominent) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = Color.White,
        )
    }
}

@Composable
private fun ReliabilityBanner(health: SystemHealth, onOpenSettings: () -> Unit) {
    val ready = health.voiceReminderReady
    val accent = if (ready) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    ElevatedCard(
        onClick = onOpenSettings,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                MaterialTheme.shapes.large,
            ),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (ready) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (ready) Icons.Default.VerifiedUser else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = accent,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (ready) "设备检查通过" else "提醒设置待完善",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    if (ready) {
                        health.nextReminderAt?.let { "下一次 ${formatNextReminder(it)}" }
                            ?: "通知、闹钟与后台权限状态正常"
                    } else {
                        "检查通知、精确闹钟、音量与后台权限"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = CircleShape,
                color = if (ready) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
            ) {
                Text(
                    if (ready) "正常" else "检查",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = accent,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayDoseCard(
    item: DoseOccurrence,
    isSubmitting: Boolean,
    actionsEnabled: Boolean,
    onClick: () -> Unit,
    onTaken: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val time = item.scheduledAt.atZone(zone).toLocalTime().format(TIME_FORMAT)
    val status = doseStatusStyle(item.status)
    val spokenDose = item.doseLabel.ifBlank { "请按医嘱核对药量" }
    ElevatedCard(
        onClick = onClick,
        enabled = actionsEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(
                    width = if (item.status == DoseStatus.RINGING) 1.5.dp else 1.dp,
                    color = if (item.status == DoseStatus.RINGING) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
                MaterialTheme.shapes.large,
            ),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (item.status == DoseStatus.RINGING) 6.dp else 1.dp,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(19.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackHeader = maxWidth < 285.dp || LocalDensity.current.fontScale >= 1.45f
                if (stackHeader) {
                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        DoseTimeLabel(time)
                        DoseStatusPill(status)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DoseTimeLabel(time)
                        Spacer(Modifier.weight(1f))
                        DoseStatusPill(status)
                    }
                }
            }
            Text(item.medicationName, style = MaterialTheme.typography.headlineSmall)
            if (item.doseLabel.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PremiumIconBadge(
                            icon = Icons.Default.Medication,
                            tint = MaterialTheme.colorScheme.secondary,
                            container = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "本次用量",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(item.doseLabel, style = MaterialTheme.typography.titleMedium)
                        }
                        if (item.route.isNotBlank()) {
                            Text(
                                item.route,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
            if (item.instructions.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        item.instructions,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (item.foodRestrictions.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier.padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "饮食注意",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                item.foodRestrictions,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (item.status == DoseStatus.RINGING) {
                Button(
                    onClick = onTaken,
                    enabled = actionsEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .semantics {
                            contentDescription = "将${item.medicationName} $spokenDose 记录为已经服用"
                        },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (isSubmitting) "正在记录…" else "已经服用")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "详情",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DoseTimeLabel(time: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            time,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DoseStatusPill(status: StatusStyle) {
    Surface(shape = CircleShape, color = status.container) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                status.icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = status.content,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                status.label,
                color = status.content,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun doseStatusStyle(status: DoseStatus): StatusStyle = when (status) {
    DoseStatus.PENDING -> StatusStyle(
        "待服",
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
        Icons.Default.AccessTime,
    )
    DoseStatus.RINGING -> StatusStyle(
        "正在提醒",
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
        Icons.Default.NotificationsActive,
    )
    DoseStatus.SNOOZED -> StatusStyle(
        "稍后提醒",
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.onTertiaryContainer,
        Icons.Default.Snooze,
    )
    DoseStatus.TAKEN -> StatusStyle(
        "已服用",
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        Icons.Default.CheckCircle,
    )
    DoseStatus.SKIPPED -> StatusStyle(
        "已跳过",
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
        Icons.Default.SkipNext,
    )
    DoseStatus.MISSED -> StatusStyle(
        "漏服/未确认",
        MaterialTheme.colorScheme.errorContainer,
        MaterialTheme.colorScheme.onErrorContainer,
        Icons.Default.WarningAmber,
    )
}

internal fun DoseStatus.canResolveFromToday(): Boolean = when (this) {
    DoseStatus.RINGING,
    DoseStatus.SNOOZED,
    DoseStatus.MISSED,
    -> true
    DoseStatus.PENDING,
    DoseStatus.TAKEN,
    DoseStatus.SKIPPED,
    -> false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoseDetailScreen(
    item: DoseOccurrence?,
    snoozeMinutes: Int,
    isSubmitting: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkipped: () -> Unit,
) {
    var confirmSkip by remember(item?.eventId) { mutableStateOf(false) }

    BackHandler {
        if (!isSubmitting) onBack()
    }

    if (confirmSkip && item != null) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) confirmSkip = false },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null) },
            title = { Text("确认跳过这次用药吗？") },
            text = {
                Text("将把 ${item.medicationName} 的本次提醒记录为“已跳过”，不会影响之后的用药提醒。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmSkip = false
                        onSkipped()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("确认跳过")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmSkip = false },
                    enabled = !isSubmitting,
                ) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val largeText = LocalDensity.current.fontScale >= 1.35f
                    Column {
                        Text("提醒详情", fontWeight = FontWeight.Bold)
                        if (!largeText) {
                            Text(
                                "核对药品后再操作",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSubmitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回今天")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (item == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("这条记录已不在今天列表中", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "日期可能已经变化，请返回今天重新查看。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(onClick = onBack) { Text("返回今天") }
                }
            }
        } else {
            DoseDetailContent(
                item = item,
                snoozeMinutes = snoozeMinutes,
                isSubmitting = isSubmitting,
                modifier = Modifier.padding(padding),
                onTaken = onTaken,
                onSnooze = onSnooze,
                onSkipped = { confirmSkip = true },
            )
        }
    }
}

@Composable
private fun DoseDetailContent(
    item: DoseOccurrence,
    snoozeMinutes: Int,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
    onTaken: () -> Unit,
    onSnooze: () -> Unit,
    onSkipped: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val scheduled = item.scheduledAt.atZone(zone)
    val status = doseStatusStyle(item.status)
    val canResolve = item.status.canResolveFromToday()
    val spokenDose = item.doseLabel.ifBlank { "请按医嘱核对药量" }
    val stateMessage = when (item.status) {
        DoseStatus.PENDING -> "这次提醒还没有开始。为避免提前误记，操作会在提醒开始后开放。"
        DoseStatus.RINGING -> "提醒正在进行，请核对药名和药量后选择本次操作。"
        DoseStatus.SNOOZED -> item.snoozedUntil?.let {
            "已安排在 ${it.atZone(zone).format(NEXT_REMINDER_FORMAT)} 再次提醒。"
        } ?: "已经设置稍后提醒。"
        DoseStatus.MISSED -> "这次用药尚未确认，可以补记已服、稍后再提醒或确认跳过。"
        DoseStatus.TAKEN -> item.actedAt?.let {
            "已在 ${it.atZone(zone).format(NEXT_REMINDER_FORMAT)} 记录为已经服用。"
        } ?: "这次用药已经记录为已服。"
        DoseStatus.SKIPPED -> item.actedAt?.let {
            "已在 ${it.atZone(zone).format(NEXT_REMINDER_FORMAT)} 记录为跳过本次。"
        } ?: "这次用药已经记录为跳过。"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            DoseDetailHero(
                item = item,
                status = status,
                scheduled = scheduled,
                stateMessage = stateMessage,
            )
        }

        item { SectionTitle("本次用药信息", "请与处方或药袋再次核对") }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        MaterialTheme.shapes.large,
                    ),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(17.dp),
                ) {
                    DoseDetailRow(
                        icon = Icons.Default.Medication,
                        label = "本次药量",
                        value = item.doseLabel.ifBlank { "请按医嘱核对" },
                    )
                    DoseDetailRow(
                        icon = Icons.Default.HealthAndSafety,
                        label = "服用方式",
                        value = item.route.ifBlank { "请按医嘱服用" },
                    )
                    DoseDetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "原定时间",
                        value = scheduled.format(NEXT_REMINDER_FORMAT),
                    )
                }
            }
        }

        if (item.instructions.isNotBlank()) {
            item {
                DoseDetailNotice(
                    icon = Icons.Default.Info,
                    title = "重要提醒",
                    detail = item.instructions,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        if (item.foodRestrictions.isNotBlank()) {
            item {
                DoseDetailNotice(
                    icon = Icons.Default.Restaurant,
                    title = "饮食注意",
                    detail = item.foodRestrictions,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        MaterialTheme.shapes.large,
                    ),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("本次怎么处理？", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    if (!canResolve) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier.padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    stateMessage,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (isSubmitting) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("正在记录，请稍候…", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = onTaken,
                        enabled = canResolve && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .semantics {
                                contentDescription = "将${item.medicationName}，$spokenDose，记录为已经服用"
                            },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text("已经服用", fontSize = 17.sp)
                    }
                    OutlinedButton(
                        onClick = onSnooze,
                        enabled = canResolve && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .semantics {
                                contentDescription = "将${item.medicationName}，$spokenDose，设置为稍后提醒$snoozeMinutes 分钟"
                            },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.Snooze, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text("稍后提醒（$snoozeMinutes 分钟）", fontSize = 17.sp)
                    }
                    TextButton(
                        onClick = onSkipped,
                        enabled = canResolve && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .semantics {
                                contentDescription = "跳过${item.medicationName}，$spokenDose，本次用药"
                            },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text("跳过本次", fontSize = 17.sp)
                    }
                    Text(
                        "“稍后提醒”会从现在起重新计时，不会改变之后的用药计划。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DoseDetailHero(
    item: DoseOccurrence,
    status: StatusStyle,
    scheduled: java.time.ZonedDateTime,
    stateMessage: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = BrandSageDeep,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(BrandSageDeep, BrandSage),
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PremiumIconBadge(
                        icon = Icons.Default.Medication,
                        tint = Color.White,
                        container = Color.White.copy(alpha = 0.14f),
                        size = 48.dp,
                    )
                    Spacer(Modifier.weight(1f))
                    DoseStatusPill(status)
                }
                Text(
                    scheduled.toLocalTime().format(TIME_FORMAT),
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                )
                Text(
                    scheduled.toLocalDate().format(DAY_FORMAT),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.90f),
                )
                Text(
                    item.medicationName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                if (item.doseLabel.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White.copy(alpha = 0.13f),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "本次 ${item.doseLabel}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                            if (item.route.isNotBlank()) {
                                Text(
                                    item.route,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.90f),
                                )
                            }
                        }
                    }
                }
                Text(
                    stateMessage,
                    color = Color.White.copy(alpha = 0.90f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DoseDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DoseDetailNotice(
    icon: ImageVector,
    title: String,
    detail: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = contentColor)
                Spacer(Modifier.height(3.dp))
                Text(detail, color = contentColor, fontSize = 17.sp)
            }
        }
    }
}

private data class StatusStyle(
    val label: String,
    val container: Color,
    val content: Color,
    val icon: ImageVector,
)

@Composable
private fun MedicationListScreen(
    plans: List<MedicationPlan>,
    zodiacCovers: Map<Long, String>,
    onZodiacChanged: (Long, com.example.medicationreminder.domain.Zodiac) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (Long) -> Unit,
    onArchive: (Long) -> Unit,
) {
    var pendingArchive by remember { mutableStateOf<MedicationPlan?>(null) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 72.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            MedicationPlanOverview(plans.size)
        }
        if (plans.isEmpty()) {
            item {
                EmptyState("还没有用药计划", "点击右下角“添加药品”开始设置吧。")
            }
        }
        items(plans, key = { it.id }) { plan ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        MaterialTheme.shapes.large,
                    ),
                onClick = { onEdit(plan.id) },
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                ZodiacCardCover(
                    zodiac = com.example.medicationreminder.domain.Zodiac.fromId(zodiacCovers[plan.id]),
                    onSelected = { onZodiacChanged(plan.id, it) },
                )
                Column(
                    modifier = Modifier.padding(19.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        PremiumIconBadge(
                            icon = Icons.Default.Medication,
                            tint = MaterialTheme.colorScheme.primary,
                            container = MaterialTheme.colorScheme.primaryContainer,
                            size = 46.dp,
                        )
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.name, style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "点击查看并编辑计划",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                            ) {
                                Text(
                                    "使用中",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                    if (plan.doseLabel.isNotBlank()) {
                        PlanDetail(Icons.Default.Medication, "每次药量", plan.doseLabel)
                    }
                    PlanDetail(
                        Icons.Default.CalendarMonth,
                        "重复日期",
                        formatWeekdays(plan.schedules.firstOrNull()?.weekdays.orEmpty()),
                    )
                    Row(verticalAlignment = Alignment.Top) {
                        PremiumIconBadge(
                            icon = Icons.Default.AccessTime,
                            tint = MaterialTheme.colorScheme.primary,
                            container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            size = 36.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "提醒时间",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(5.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                            plan.schedules.forEach { schedule ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                ) {
                                    Text(
                                        schedule.time.format(TIME_FORMAT),
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                            }
                            }
                        }
                    }
                    if (plan.instructions.isNotBlank()) {
                        PlanDetail(Icons.Default.Campaign, "重要提醒", plan.instructions)
                    }
                    if (plan.foodRestrictions.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    "饮食注意：${plan.foodRestrictions}",
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { pendingArchive = plan }) {
                            Text("停用计划", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "编辑详情",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    pendingArchive?.let { plan ->
        AlertDialog(
            onDismissRequest = { pendingArchive = null },
            title = { Text("停用 ${plan.name}？") },
            text = { Text("未来提醒会取消，但既往记录会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    onArchive(plan.id)
                    pendingArchive = null
                }) { Text("停用") }
            },
            dismissButton = {
                TextButton(onClick = { pendingArchive = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun MedicationPlanOverview(planCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumIconBadge(
                icon = Icons.Default.Medication,
                tint = Color.White,
                container = BrandSageDeep,
                size = 56.dp,
            )
            Spacer(Modifier.width(15.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "正在使用",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    if (planCount == 0) "还没有用药计划" else "$planCount 种药品",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    if (planCount == 0) "添加第一项计划，按时获得提醒"
                    else "十二生肖，陪你照顾每一天 · 点击卡面更换伙伴",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlanDetail(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        PremiumIconBadge(
            icon = icon,
            tint = MaterialTheme.colorScheme.secondary,
            container = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
            size = 36.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationEditorScreen(
    plan: MedicationPlan?,
    isSaving: Boolean,
    snackbarHostState: SnackbarHostState,
    onCancel: () -> Unit,
    onSave: (MedicationDraft) -> Unit,
) {
    var name by rememberSaveable(plan?.id) { mutableStateOf(plan?.name.orEmpty()) }
    var doseAmount by rememberSaveable(plan?.id) { mutableStateOf(plan?.doseAmount.orEmpty()) }
    var doseUnit by rememberSaveable(plan?.id) { mutableStateOf(plan?.doseUnit ?: "片") }
    var route by rememberSaveable(plan?.id) { mutableStateOf(plan?.route ?: "口服") }
    var instructions by rememberSaveable(plan?.id) { mutableStateOf(plan?.instructions.orEmpty()) }
    var restrictions by rememberSaveable(plan?.id) { mutableStateOf(plan?.foodRestrictions.orEmpty()) }
    var startDateText by rememberSaveable(plan?.id) {
        mutableStateOf((plan?.startDate ?: LocalDate.now()).toString())
    }
    var endDateText by rememberSaveable(plan?.id) { mutableStateOf(plan?.endDate?.toString().orEmpty()) }
    var weekdaysMask by rememberSaveable(plan?.id) {
        mutableIntStateOf(
            (plan?.schedules?.firstOrNull()?.weekdays ?: DayOfWeek.entries.toSet())
                .fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }
        )
    }
    var times by rememberSaveable(plan?.id, stateSaver = LOCAL_TIME_LIST_SAVER) {
        mutableStateOf(plan?.schedules?.map { it.time } ?: listOf(LocalTime.of(8, 0)))
    }
    var validationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val submit: () -> Unit = {
        if (!isSaving) {
            val startDate = runCatching { LocalDate.parse(startDateText.trim()) }.getOrNull()
            val endDate = endDateText.trim().takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val weekdays = DayOfWeek.entries.filterTo(linkedSetOf()) { day ->
                weekdaysMask and (1 shl (day.value - 1)) != 0
            }
            validationError = when {
                name.isBlank() -> "请输入药品名称"
                doseAmount.isBlank() -> "请输入每次药量"
                startDate == null -> "开始日期格式应为 YYYY-MM-DD"
                endDateText.isNotBlank() && endDate == null -> "结束日期格式应为 YYYY-MM-DD"
                endDate != null && endDate.isBefore(startDate) -> "结束日期不能早于开始日期"
                times.isEmpty() -> "至少添加一个提醒时间"
                weekdays.isEmpty() -> "至少选择一个星期"
                else -> null
            }
            if (validationError == null && startDate != null) {
                onSave(
                    MedicationDraft(
                        id = plan?.id,
                        name = name,
                        doseAmount = doseAmount,
                        doseUnit = doseUnit,
                        route = route,
                        instructions = instructions,
                        foodRestrictions = restrictions,
                        startDate = startDate,
                        endDate = endDate,
                        times = times,
                        weekdays = weekdays,
                    )
                )
            }
        }
    }

    BackHandler {
        if (!isSaving) onCancel()
    }

    Scaffold(
        modifier = Modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val largeText = LocalDensity.current.fontScale >= 1.35f
                    Column {
                        Text(
                            if (plan == null) "添加用药计划" else "编辑用药计划",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (!largeText) {
                            Text(
                                "按处方准确填写",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "取消并返回")
                    }
                },
                actions = {
                    TextButton(onClick = submit, enabled = !isSaving) {
                        Text(if (isSaving) "保存中…" else "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = BrandSageDeep,
                shadowElevation = 5.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PremiumIconBadge(
                        icon = Icons.Default.HealthAndSafety,
                        tint = Color.White,
                        container = Color.White.copy(alpha = 0.14f),
                        size = 48.dp,
                    )
                    Spacer(Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "准确的信息带来可靠的提醒",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            "请按照医生、药师或药袋上的信息填写",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f),
                        )
                    }
                }
            }
            validationError?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            EditorSectionCard(
                icon = Icons.Default.Medication,
                title = "药品信息",
                detail = "药名、药量和服用方式",
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("药品名称 *") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = doseAmount,
                    onValueChange = { doseAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("每次药量 *") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = doseUnit,
                    onValueChange = { doseUnit = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("药量单位，例如：片、粒、毫升") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = route,
                    onValueChange = { route = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("服用方式，例如：口服、外用") },
                    singleLine = true,
                )
            }

            EditorSectionCard(
                icon = Icons.Default.AccessTime,
                title = "提醒安排",
                detail = "设置每天提醒的时间和日期",
            ) {
                Text("提醒时间", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    times.sorted().forEach { time ->
                        InputChip(
                            selected = true,
                            onClick = { times = times - time },
                            label = { Text("${time.format(TIME_FORMAT)}  ×") },
                        )
                    }
                    AssistChip(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val selected = LocalTime.of(hour, minute)
                                    if (selected !in times) times = times + selected
                                },
                                8,
                                0,
                                true,
                            ).show()
                        },
                        label = { Text("＋ 添加时间") },
                    )
                }
                Text("每周重复", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val bit = 1 shl (day.value - 1)
                        FilterChip(
                            selected = weekdaysMask and bit != 0,
                            onClick = { weekdaysMask = weekdaysMask xor bit },
                            label = { Text(DAY_LABELS.getValue(day)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = startDateText,
                    onValueChange = { startDateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("开始日期") },
                    supportingText = { Text("YYYY-MM-DD") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = endDateText,
                    onValueChange = { endDateText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("结束日期（可空）") },
                    supportingText = { Text("YYYY-MM-DD") },
                    singleLine = true,
                )
            }

            EditorSectionCard(
                icon = Icons.Default.Info,
                title = "医嘱与注意事项",
                detail = "这些内容会一起语音播报",
            ) {
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("其他重要提醒") },
                    minLines = 2,
                )
                OutlinedTextField(
                    value = restrictions,
                    onValueChange = { restrictions = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("饮食与忌口提醒") },
                    minLines = 2,
                )
            }

            Button(
                onClick = submit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    if (isSaving) "正在保存…"
                    else if (plan == null) "保存用药计划"
                    else "保存修改"
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun EditorSectionCard(
    icon: ImageVector,
    title: String,
    detail: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                MaterialTheme.shapes.large,
            ),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PremiumIconBadge(
                    icon = icon,
                    tint = MaterialTheme.colorScheme.primary,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    size = 42.dp,
                )
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingsScreen(
    health: SystemHealth,
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onTest: () -> Unit,
    onPreview: () -> Unit,
    onSnoozeChanged: (Int) -> Unit,
    onRepeatChanged: (Int) -> Unit,
    onVoiceStyleChanged: (ReminderVoiceStyle) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onSpeechPitchChanged: (Float) -> Unit,
    onSpeechVolumeChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    val readyCount = listOf(
        health.notificationsAllowed,
        health.alarmChannelAllowed,
        health.exactAlarmsAllowed,
        health.fullScreenAllowed,
        health.alarmVolumeAudible,
        !health.backgroundRestricted,
    ).count { it }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onRefresh() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ReminderHealthHero(health = health, readyCount = readyCount)
        }

        item { SectionTitle("语音陪伴", "选择让你听起来最舒服的提醒方式") }
        item {
            VoiceStyleGrid(
                selected = preferences.voiceStyle,
                onSelected = onVoiceStyleChanged,
            )
        }
        if (preferences.voiceStyle == ReminderVoiceStyle.CUSTOM) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text("当前正在使用高级自定义参数", modifier = Modifier.padding(12.dp))
                }
            }
        }
        item {
            Button(
                onClick = onPreview,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("立即试听当前风格")
            }
        }
        item {
            OutlinedButton(
                onClick = { context.openSystemSettings(Intent(TTS_SETTINGS_ACTION)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.RecordVoiceOver, null)
                Spacer(Modifier.width(8.dp))
                Text("选择手机中的中文音色")
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "语气、语速、音调和音量会随选择变化；实际基础音色由手机安装的中文语音引擎提供。",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionTitle("提醒节奏", "可按听感继续微调；调整后会显示为自定义") }
        item {
            PreferenceChoices(
                title = "默认稍后提醒",
                values = listOf(5, 10, 15, 30),
                selected = preferences.defaultSnoozeMinutes,
                label = { "$it 分钟" },
                onSelected = onSnoozeChanged,
            )
        }
        item {
            PreferenceChoices(
                title = "未处理时重复播报",
                values = listOf(30, 45, 60, 120),
                selected = preferences.repeatIntervalSeconds,
                label = { "$it 秒" },
                onSelected = onRepeatChanged,
            )
        }
        item {
            PreferenceChoices(
                title = "语速微调",
                values = listOf(0.75f, 0.82f, 0.9f, 1.0f, 1.2f),
                selected = preferences.speechRate,
                label = { value ->
                    when (value) {
                        0.75f -> "慢"
                        0.82f -> "柔和"
                        0.9f -> "较慢"
                        1.0f -> "正常"
                        1.2f -> "较快"
                        else -> "自定义"
                    }
                },
                onSelected = onSpeechRateChanged,
            )
        }
        item {
            PreferenceChoices(
                title = "音调微调",
                values = listOf(0.96f, 1.0f, 1.04f, 1.12f, 1.2f),
                selected = preferences.speechPitch,
                label = { value ->
                    when (value) {
                        0.96f -> "沉稳"
                        1.0f -> "自然"
                        1.04f -> "柔和"
                        1.12f -> "轻快"
                        1.2f -> "活泼"
                        else -> "自定义"
                    }
                },
                onSelected = onSpeechPitchChanged,
            )
        }
        item {
            PreferenceChoices(
                title = "播报音量",
                values = listOf(0.6f, 0.74f, 0.78f, 0.9f, 1.0f),
                selected = preferences.speechVolume,
                label = { value ->
                    when (value) {
                        0.6f -> "轻柔"
                        0.74f -> "温和"
                        0.78f -> "舒适"
                        0.9f -> "清楚"
                        1.0f -> "响亮"
                        else -> "自定义"
                    }
                },
                onSelected = onSpeechVolumeChanged,
            )
        }

        item { SectionTitle("后台提醒可靠性", "语音要在退出应用后响起，请完成以下检查") }
        item {
            HealthRow(
                title = "通知权限",
                ready = health.notificationsAllowed,
                detail = "用于显示用药提醒和操作按钮",
                actionLabel = if (health.notificationsAllowed) null else "授权",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.openSystemSettings(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                },
            )
        }
        item {
            HealthRow(
                title = "精确闹钟",
                ready = health.exactAlarmsAllowed,
                detail = "后台准时播放语音所必需；未开启时仅有可能延迟的铃声通知",
                actionLabel = if (health.exactAlarmsAllowed) null else "设置",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.openSystemSettings(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData("package:${context.packageName}".toUri())
                        )
                    }
                },
            )
        }
        item {
            HealthRow(
                title = "提醒通知通道",
                ready = health.alarmChannelAllowed,
                detail = "通道被关闭时，提醒界面和操作按钮可能不可见",
                actionLabel = if (health.alarmChannelAllowed) null else "设置",
                onAction = {
                    context.openSystemSettings(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, AlarmPlaybackService.CHANNEL_ID)
                    )
                },
            )
        }
        item {
            HealthRow(
                title = "锁屏全屏提醒",
                ready = health.fullScreenAllowed,
                detail = "不可用时仍会显示高优先级通知并播放语音",
                actionLabel = if (health.fullScreenAllowed || Build.VERSION.SDK_INT < 34) null else "设置",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        context.openSystemSettings(
                            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                                .setData("package:${context.packageName}".toUri())
                        )
                    }
                },
            )
        }
        item {
            HealthRow(
                title = "闹钟音量",
                ready = health.alarmVolumeAudible,
                detail = "中文语音和兜底铃声使用系统闹钟音量",
                actionLabel = if (health.alarmVolumeAudible) null else "调整",
                onAction = { context.openSystemSettings(Intent(Settings.ACTION_SOUND_SETTINGS)) },
            )
        }
        item {
            HealthRow(
                title = "后台运行限制",
                ready = !health.backgroundRestricted,
                detail = "系统“受限”模式会阻止闹钟和后台播放",
                actionLabel = if (health.backgroundRestricted) "设置" else null,
                onAction = {
                    context.openSystemSettings(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData("package:${context.packageName}".toUri())
                    )
                },
            )
        }
        item {
            Button(
                onClick = {
                    if (health.exactAlarmsAllowed) {
                        onTest()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.openSystemSettings(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData("package:${context.packageName}".toUri())
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            ) {
                Icon(if (health.exactAlarmsAllowed) Icons.Default.AccessTime else Icons.Default.WarningAmber, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (health.exactAlarmsAllowed) "30 秒后台真实提醒测试"
                    else "先开启精确闹钟权限"
                )
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    context.openSystemSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.BatteryAlert, null)
                Spacer(Modifier.width(8.dp))
                Text("检查省电与后台运行设置")
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("退出后的提醒说明", fontWeight = FontWeight.Bold)
                    Text("• 设备检查通过且应用未被系统强行停止时，返回桌面、划掉最近任务或进程被回收后，已设置的闹钟仍会保留。")
                    Text("• 在系统设置中点“强行停止”会取消闹钟，任何应用都无法绕过；重新打开本应用即可恢复。")
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    "本应用是提醒工具，不是医疗器械。首次使用请完成后台真实测试；验证前请保留备用提醒方式。",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ReminderHealthHero(health: SystemHealth, readyCount: Int) {
    val progress = readyCount / 6f
    val allChecksReady = readyCount == 6
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (allChecksReady) {
                    "设备检查通过，六项检查全部正常"
                } else {
                    "设备检查，六项中有$readyCount 项正常"
                }
            },
        shape = MaterialTheme.shapes.large,
        color = BrandSageDeep,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(BrandSageDeep, BrandSage),
                    )
                )
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                val stacked = maxWidth < 315.dp || LocalDensity.current.fontScale >= 1.5f
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HealthProgressBadge(progress = progress, readyCount = readyCount)
                        HealthHeroText(
                            health = health,
                            allChecksReady = allChecksReady,
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HealthProgressBadge(progress = progress, readyCount = readyCount)
                        Spacer(Modifier.width(18.dp))
                        HealthHeroText(
                            health = health,
                            allChecksReady = allChecksReady,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthProgressBadge(progress: Float, readyCount: Int) {
    Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFFC2AE),
            trackColor = Color.White.copy(alpha = 0.18f),
            strokeWidth = 7.dp,
        )
        Text(
            "$readyCount/6",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
    }
}

@Composable
private fun HealthHeroText(
    health: SystemHealth,
    allChecksReady: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (allChecksReady) "设备条件已经准备好" else "还有设备设置需要完成",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Text(
            if (allChecksReady) "请再完成一次后台测试，确认中文语音可用"
            else "完成下方检查，提升后台提醒可靠性",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.90f),
        )
        health.nextReminderAt?.let {
            Text(
                "下一次 ${formatNextReminder(it)}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun VoiceStyleGrid(
    selected: ReminderVoiceStyle,
    onSelected: (ReminderVoiceStyle) -> Unit,
) {
    val styles = listOf(
        ReminderVoiceStyle.GENTLE,
        ReminderVoiceStyle.PLAYFUL,
        ReminderVoiceStyle.FIRM,
        ReminderVoiceStyle.CLINICAL,
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val oneColumn = maxWidth < 380.dp || LocalDensity.current.fontScale > 1.2f
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (oneColumn) {
                styles.forEach { style ->
                    VoiceStyleCard(
                        style = style,
                        selected = selected == style,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSelected,
                    )
                }
            } else {
                styles.chunked(2).forEach { rowStyles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowStyles.forEach { style ->
                            VoiceStyleCard(
                                style = style,
                                selected = selected == style,
                                modifier = Modifier.weight(1f),
                                onClick = onSelected,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceStyleCard(
    style: ReminderVoiceStyle,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (ReminderVoiceStyle) -> Unit,
) {
    val (title, detail, icon) = when (style) {
        ReminderVoiceStyle.GENTLE -> Triple("温柔陪伴", "慢一些，轻声安抚", Icons.Default.Favorite)
        ReminderVoiceStyle.PLAYFUL -> Triple("可爱童声", "轻快俏皮，像小朋友", Icons.Default.ChildCare)
        ReminderVoiceStyle.FIRM -> Triple("严肃清晰", "直接醒目，不责备", Icons.Default.Campaign)
        ReminderVoiceStyle.CLINICAL -> Triple("专业医护", "克制清楚，按项播报", Icons.Default.LocalHospital)
        ReminderVoiceStyle.CUSTOM -> Triple("自定义", "使用手动参数", Icons.Default.RecordVoiceOver)
    }
    Card(
        onClick = { onClick(style) },
        modifier = modifier
            .heightIn(min = 98.dp)
            .semantics {
                role = Role.RadioButton
                stateDescription = if (selected) "已选择" else "未选择"
            },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                BrandSageDeep
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) BrandSage else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumIconBadge(
                icon = icon,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                container = if (selected) {
                    Color.White.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                size = 46.dp,
            )
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        Color.White.copy(alpha = 0.90f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                )
            } else {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                ) {}
            }
        }
    }
}

@Composable
private fun HealthRow(
    title: String,
    ready: Boolean,
    detail: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (ready) "正常" else "需要处理" }
            .border(
                BorderStroke(
                    1.dp,
                    if (ready) MaterialTheme.colorScheme.outlineVariant
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
                ),
                MaterialTheme.shapes.large,
            ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (ready) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
            },
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (ready) 0.dp else 1.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.45f
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    PremiumIconBadge(
                        icon = if (ready) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        tint = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        container = if (ready) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                        },
                        size = 40.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!stacked) {
                        Spacer(Modifier.width(8.dp))
                        HealthStatePill(ready)
                    }
                }
                if (stacked) HealthStatePill(ready)
                actionLabel?.let {
                    OutlinedButton(
                        onClick = onAction,
                        modifier = Modifier.heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStatePill(ready: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (ready) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Text(
            if (ready) "正常" else "需处理",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (ready) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SectionTitle(title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PremiumIconBadge(
    icon: ImageVector,
    tint: Color,
    container: Color,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = CircleShape,
        color = container,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(if (size <= 38.dp) 19.dp else 24.dp),
                tint = tint,
            )
        }
    }
}

@Composable
private fun <T> PreferenceChoices(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                values.forEach { value ->
                    FilterChip(
                        selected = value == selected,
                        onClick = { onSelected(value) },
                        label = { Text(label(value)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PremiumIconBadge(
                    icon = Icons.Default.Medication,
                    tint = MaterialTheme.colorScheme.primary,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    size = 58.dp,
                )
                Spacer(Modifier.height(15.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(5.dp))
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun formatWeekdays(days: Set<DayOfWeek>): String = when {
    days.isEmpty() || days.size == 7 -> "每天"
    days == setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    ) -> "工作日"
    else -> DayOfWeek.entries.filter { it in days }.joinToString("、") { DAY_LABELS.getValue(it) }
}

private fun formatNextReminder(instant: Instant): String {
    val zoned = instant.atZone(ZoneId.systemDefault())
    return if (zoned.toLocalDate() == LocalDate.now()) {
        "今天 ${zoned.toLocalTime().format(TIME_FORMAT)}"
    } else {
        zoned.format(NEXT_REMINDER_FORMAT)
    }
}

private val DAY_LABELS = mapOf(
    DayOfWeek.MONDAY to "一",
    DayOfWeek.TUESDAY to "二",
    DayOfWeek.WEDNESDAY to "三",
    DayOfWeek.THURSDAY to "四",
    DayOfWeek.FRIDAY to "五",
    DayOfWeek.SATURDAY to "六",
    DayOfWeek.SUNDAY to "日",
)

private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
private val NEXT_REMINDER_FORMAT = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.SIMPLIFIED_CHINESE)
private val LOCAL_TIME_LIST_SAVER = listSaver<List<LocalTime>, String>(
    save = { times -> times.map { it.toString() } },
    restore = { saved -> saved.map { LocalTime.parse(it) } },
)
private const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"

private fun Context.openSystemSettings(intent: Intent) {
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData("package:$packageName".toUri())
    val target = if (intent.resolveActivity(packageManager) != null) intent else fallback
    runCatching { startActivity(target) }
}
