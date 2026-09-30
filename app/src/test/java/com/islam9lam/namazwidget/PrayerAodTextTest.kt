package com.islam9lam.namazwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerAodTextTest {
    @Test
    fun compactDurationFitsNowBarWithoutDuplicatingPrayerTime() {
        assertEquals("0м", PrayerAod.compactDurationText(0))
        assertEquals("42м", PrayerAod.compactDurationText(42))
        assertEquals("1ч", PrayerAod.compactDurationText(60))
        assertEquals("1ч 15м", PrayerAod.compactDurationText(75))
    }
}
