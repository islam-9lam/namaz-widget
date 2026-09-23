package com.islam9lam.namazwidget

import java.time.YearMonth

object Sources {
 /** Stable offline calculation restored from the original 0.1.0 APK. */
 fun fetch(city:City,month:YearMonth)=CoordinatePrayerCalculator.calculate(city,month)
}
