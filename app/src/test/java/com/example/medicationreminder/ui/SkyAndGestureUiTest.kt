package com.example.medicationreminder.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.example.medicationreminder.domain.Zodiac
import com.example.medicationreminder.reminder.SwipeDoseControl
import com.example.medicationreminder.ui.theme.MedicationReminderTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyAndGestureUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `long hold and release up or down invokes only the matching action`() {
        var taken = 0
        var snoozed = 0
        compose.setContent {
            MedicationReminderTheme {
                SwipeDoseControl(true, "测试药品", { taken++ }, { snoozed++ })
            }
        }
        val knob = compose.onNodeWithContentDescription("测试药品，按住上滑已经服用，下滑稍后提醒")
        knob.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, -160f))
            up()
        }
        compose.waitForIdle()
        assertEquals(1, taken)
        assertEquals(0, snoozed)
        knob.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, 160f))
            up()
        }
        compose.waitForIdle()
        assertEquals(1, taken)
        assertEquals(1, snoozed)
        knob.performTouchInput { down(center); advanceEventTime(700); moveBy(Offset(0f, -160f)); cancel() }
        compose.waitForIdle()
        assertEquals(1, taken)
    }

    @Test fun `day and night covers render and zodiac selection updates`() {
        var dark by mutableStateOf(false)
        var cover by mutableStateOf(Zodiac.RABBIT)
        compose.setContent {
            MedicationReminderTheme(dark) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text("安心用药", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("用药计划 · 今天也要好好照顾自己", color = MaterialTheme.colorScheme.onBackground)
                    ElevatedCard(elevation = CardDefaults.elevatedCardElevation(12.dp)) {
                        ZodiacCardCover(cover) { cover = it }
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("示例用药计划", style = MaterialTheme.typography.headlineSmall)
                            Text("每次药量 · 按已录入计划")
                            Text("08:00   /   18:00", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Text("锁屏手势", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Surface(shape = MaterialTheme.shapes.large) {
                        SwipeDoseControl(true, "示例", {}, {})
                    }
                }
            }
        }
        capture("day")
        compose.onNodeWithText("更换卡面 ›").performClick()
        compose.onNodeWithText("龙", useUnmergedTree = true).performClick()
        compose.onNodeWithText("龙 · 陪伴").assertExists()
        compose.runOnIdle { dark = true }
        capture("night")
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnIdle { view.draw(Canvas(bitmap)) }
        val file = File("build/reports/visual/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
