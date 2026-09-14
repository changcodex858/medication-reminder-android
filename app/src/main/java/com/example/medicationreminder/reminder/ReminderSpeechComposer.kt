package com.example.medicationreminder.reminder

import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.domain.ReminderVoiceStyle
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

object ReminderSpeechComposer {
    fun testMessage(style: ReminderVoiceStyle): String = when (style) {
        ReminderVoiceStyle.GENTLE ->
            "你好，这是温柔陪伴语音。慢慢来，我们一起记得按时用药。"
        ReminderVoiceStyle.PLAYFUL ->
            "嗨呀，小小提醒来啦。该吃药咯，吃好以后记得告诉我哦。"
        ReminderVoiceStyle.FIRM ->
            "请注意，现在是用药时间。请核对药品和剂量，并及时处理提醒。"
        ReminderVoiceStyle.CLINICAL ->
            "用药提醒。请按照已录入的医嘱，核对药品名称、剂量与服用方式。"
        ReminderVoiceStyle.CUSTOM ->
            "你好，这是自定义语音试听。请确认语速、音调和音量是否舒适清楚。"
    }

    fun compose(
        items: List<DoseOccurrence>,
        style: ReminderVoiceStyle = ReminderVoiceStyle.default,
        remindedAt: Instant = Instant.now(),
        lastTakenAtByMedication: Map<Long, Instant> = emptyMap(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = buildString {
        append(opening(style))
        append(currentTimeAnnouncement(remindedAt, zoneId))
        items.forEach { item ->
            append(if (style == ReminderVoiceStyle.FIRM) "请服用" else "请记得服用")
            append(item.medicationName)
            append("。")
            if (item.doseLabel.isNotBlank()) append("这一次的药量是${item.doseLabel}。")
            append(intervalAnnouncement(lastTakenAtByMedication[item.medicationId], remindedAt))
            if (item.route.isNotBlank()) append("服用方式是${item.route}。")
            if (item.instructions.isNotBlank()) append("重要提醒，${item.instructions}。")
            if (item.foodRestrictions.isNotBlank()) append("饮食方面请注意，${item.foodRestrictions}。")
        }
        append(closing(style))
    }

    private fun currentTimeAnnouncement(remindedAt: Instant, zoneId: ZoneId): String {
        val time = remindedAt.atZone(zoneId).toLocalTime()
        val period = when (time.hour) {
            in 0..5 -> "凌晨"
            in 6..11 -> "上午"
            12 -> "中午"
            in 13..17 -> "下午"
            else -> "晚上"
        }
        val spokenHour = when {
            time.hour == 0 -> 12
            time.hour > 12 -> time.hour - 12
            else -> time.hour
        }
        val minute = if (time.minute == 0) "整" else "${time.minute}分"
        return "现在时间是$period${spokenHour}点$minute。"
    }

    private fun intervalAnnouncement(lastTakenAt: Instant?, remindedAt: Instant): String {
        if (lastTakenAt == null) return "应用中还没有找到这项药品的上次已服记录。"

        val elapsedSeconds = Duration.between(lastTakenAt, remindedAt).seconds.coerceAtLeast(0)
        if (elapsedSeconds < 60) {
            return "根据已确认的记录，距离上次服用还不到1分钟。"
        }

        val totalMinutes = elapsedSeconds / 60
        val days = totalMinutes / (24 * 60)
        val hours = totalMinutes % (24 * 60) / 60
        val minutes = totalMinutes % 60
        val duration = buildString {
            if (days > 0) append("${days}天")
            if (hours > 0) append("${hours}小时")
            if (minutes > 0) append("${minutes}分钟")
        }
        return "根据已确认的记录，距离上次服用已经过去$duration。"
    }

    private fun opening(style: ReminderVoiceStyle): String = when (style) {
        ReminderVoiceStyle.GENTLE -> "你好，到了用药时间。别着急，我们慢慢核对。"
        ReminderVoiceStyle.PLAYFUL -> "嗨呀，小小提醒来啦。该吃药咯。"
        ReminderVoiceStyle.FIRM -> "请注意，现在是规定的用药时间。"
        ReminderVoiceStyle.CLINICAL -> "用药提醒。请按照已录入的医嘱核对本次用药。"
        ReminderVoiceStyle.CUSTOM -> "你好，现在是用药时间。"
    }

    private fun closing(style: ReminderVoiceStyle): String = when (style) {
        ReminderVoiceStyle.GENTLE ->
            "服用以后，请轻轻点一下我已经服用。如果现在不方便，也可以选择稍后提醒。"
        ReminderVoiceStyle.PLAYFUL ->
            "吃好以后，记得点一下我已经服用哦。现在不方便的话，就让我稍后再来提醒你。"
        ReminderVoiceStyle.FIRM ->
            "完成后请确认已经服用；如暂时无法服用，请选择稍后提醒，不要直接忽略。"
        ReminderVoiceStyle.CLINICAL ->
            "服用后请确认记录；如需延后，请选择稍后提醒。记录不替代医生或药师建议。"
        ReminderVoiceStyle.CUSTOM ->
            "服用以后，请确认已经服用；现在不方便时，可以选择稍后提醒。"
    }
}
