package com.example.medicationreminder.ui

import com.example.medicationreminder.domain.DoseStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseActionAvailabilityTest {
    @Test
    fun `active unresolved states expose the three detail actions`() {
        assertTrue(DoseStatus.RINGING.canResolveFromToday())
        assertTrue(DoseStatus.SNOOZED.canResolveFromToday())
        assertTrue(DoseStatus.MISSED.canResolveFromToday())
    }

    @Test
    fun `future and terminal states keep detail actions read only`() {
        assertFalse(DoseStatus.PENDING.canResolveFromToday())
        assertFalse(DoseStatus.TAKEN.canResolveFromToday())
        assertFalse(DoseStatus.SKIPPED.canResolveFromToday())
    }
}
