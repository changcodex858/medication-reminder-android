package com.example.medicationreminder.reminder

import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.domain.ReminderVoiceStyle

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
    ): String = buildString {
        append(opening(style))
        items.forEach { item ->
            append(if (style == ReminderVoiceStyle.FIRM) "请服用" else "请记得服用")
            append(item.medicationName)
            append("。")
            if (item.doseLabel.isNotBlank()) append("这一次的药量是${item.doseLabel}。")
            if (item.route.isNotBlank()) append("服用方式是${item.route}。")
            if (item.instructions.isNotBlank()) append("重要提醒，${item.instructions}。")
            if (item.foodRestrictions.isNotBlank()) append("饮食方面请注意，${item.foodRestrictions}。")
        }
        append(closing(style))
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
