package com.example.medicationreminder.reminder

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
@Config(sdk = [35], qualifiers = "w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockScreenLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `compact lock screen keeps the gesture visible without scrolling medication details`() {
        var taken = 0
        compose.setContent {
            MedicationReminderTheme(darkTheme = true) {
                LockScreenReminderScreen(
                    ReminderUiState(loading = false, occurrences = listOf(ReminderActivity.demoOccurrence())),
                    true, { taken++ }, {}, {},
                )
            }
        }
        val knob = compose.onNodeWithContentDescription("锁屏滑动演练，按住上滑已经服用，下滑稍后提醒")
        knob.assertIsDisplayed()
        compose.onNodeWithText("锁屏滑动演练").assertIsDisplayed()
        compose.onNodeWithText("↑  已经服用").assertIsDisplayed()
        compose.onNodeWithText("↓  稍后提醒").assertIsDisplayed()
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnIdle { view.draw(Canvas(bitmap)) }
        val file = File("build/reports/visual/lock-screen.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        knob.performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, -150f)); up() }
        compose.waitForIdle()
        assertEquals(1, taken)
    }
}
