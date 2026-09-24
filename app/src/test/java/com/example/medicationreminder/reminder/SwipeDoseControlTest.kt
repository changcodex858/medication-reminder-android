package com.example.medicationreminder.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeDoseControlTest {
    @Test fun `short drags and returning to center do not resolve a dose`() {
        listOf(0f, -64f, 64f).forEach { assertNull(swipeDoseAction(it, 65f)) }
    }

    @Test fun `up means taken and down means snooze at the release threshold`() {
        assertEquals(SwipeDoseAction.TAKEN, swipeDoseAction(-65f, 65f))
        assertEquals(SwipeDoseAction.SNOOZE, swipeDoseAction(65f, 65f))
        assertEquals(SwipeDoseAction.TAKEN, swipeDoseAction(-85f, 65f))
    }
}
