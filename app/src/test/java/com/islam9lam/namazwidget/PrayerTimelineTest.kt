package com.islam9lam.namazwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class PrayerTimelineTest {
 private val yesterday=PrayerDay(22,"05:10","06:30","12:20","15:40","18:05","19:25")
 private val today=PrayerDay(23,"05:08","06:28","12:20","15:42","18:08","19:28")
 private val tomorrow=PrayerDay(24,"05:06","06:26","12:20","15:44","18:10","19:30")

 @Test fun beforeFajrUsesPreviousIsha(){
  val state=PrayerTimeline.calculate(LocalDateTime.parse("2026-09-23T04:30"),yesterday,today,tomorrow)!!
  assertEquals("Иша",state.current.name)
  assertEquals("Фаджр",state.next.name)
  assertEquals(LocalDateTime.parse("2026-09-23T05:08"),state.next.time)
 }

 @Test fun daytimeMovesToNextPrayer(){
  val state=PrayerTimeline.calculate(LocalDateTime.parse("2026-09-23T16:00"),yesterday,today,tomorrow)!!
  assertEquals("Аср",state.current.name)
  assertEquals("Магриб",state.next.name)
  assertEquals(LocalDateTime.parse("2026-09-23T18:08"),state.next.time)
 }

 @Test fun afterIshaUsesTomorrowFajr(){
  val state=PrayerTimeline.calculate(LocalDateTime.parse("2026-09-23T22:00"),yesterday,today,tomorrow)!!
  assertEquals("Иша",state.current.name)
  assertEquals(LocalDateTime.parse("2026-09-24T05:06"),state.next.time)
 }

 @Test fun hanafiAsrIsUsedWhenSelected(){
  val hanafiToday=today.copy(asrHanafi="16:45")
  val state=PrayerTimeline.calculate(LocalDateTime.parse("2026-09-23T16:00"),yesterday,hanafiToday,tomorrow,true)!!
  assertEquals("Зухр",state.current.name)
  assertEquals("Аср",state.next.name)
  assertEquals(LocalDateTime.parse("2026-09-23T16:45"),state.next.time)
 }

 @Test fun coordinateCalculatorProvidesBothAsrMethods(){
  val city=City("Москва","Россия",55.7558,37.6173,"Europe/Moscow")
  val days=CoordinatePrayerCalculator.calculate(city,YearMonth.of(2026,9))
  assertEquals(30,days.size)
  val day=days[22]
  assertTrue(LocalTime.parse(day.asrHanafi!!).isAfter(LocalTime.parse(day.asr)))
 }
}
