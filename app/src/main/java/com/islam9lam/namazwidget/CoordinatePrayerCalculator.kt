package com.islam9lam.namazwidget

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

object CoordinatePrayerCalculator {
 private val timeFormatter=DateTimeFormatter.ofPattern("HH:mm")

 fun calculate(city:City,month:YearMonth):List<PrayerDay>{
  val coordinates=Coordinates(city.lat,city.lon)
  val zone=ZoneId.of(city.timeZone)
  fun format(value:Date)=Instant.ofEpochMilli(value.time).atZone(zone).format(timeFormatter)
  return (1..month.lengthOfMonth()).map{day->
   val date=DateComponents(month.year,month.monthValue,day)
   val shafiParameters=CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.apply{
    madhab=Madhab.SHAFI
    highLatitudeRule=HighLatitudeRule.TWILIGHT_ANGLE
   }
   val hanafiParameters=CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.apply{
    madhab=Madhab.HANAFI
    highLatitudeRule=HighLatitudeRule.TWILIGHT_ANGLE
   }
   val shafi=PrayerTimes(coordinates,date,shafiParameters)
   val hanafi=PrayerTimes(coordinates,date,hanafiParameters)
   PrayerDay(day,format(shafi.fajr),format(shafi.sunrise),format(shafi.dhuhr),format(shafi.asr),format(shafi.maghrib),format(shafi.isha),format(hanafi.asr))
  }
 }
}
