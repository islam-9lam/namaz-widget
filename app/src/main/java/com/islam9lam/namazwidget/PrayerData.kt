package com.islam9lam.namazwidget

data class PrayerDay(
 val day:Int,
 val fajr:String,
 val sunrise:String,
 val dhuhr:String,
 val asr:String,
 val maghrib:String,
 val isha:String,
 val asrHanafi:String?=null
)

data class City(
 val name:String,
 val country:String,
 val lat:Double,
 val lon:Double,
 val timeZone:String
)

data class CitySearchResult(
 val name:String,
 val country:String,
 val admin1:String?,
 val lat:Double,
 val lon:Double,
 val timeZone:String
){
 fun displayName()=listOfNotNull(name,admin1?.takeIf{it.isNotBlank()},country.takeIf{it.isNotBlank()}).distinct().joinToString(", ")
 fun toCity()=City(name,country,lat,lon,timeZone)
}

object Cities {
 val list=listOf(
  City("Москва","Россия",55.7558,37.6173,"Europe/Moscow"),
  City("Грозный","Россия",43.3180,45.6982,"Europe/Moscow"),
  City("Санкт-Петербург","Россия",59.9343,30.3351,"Europe/Moscow"),
  City("Казань","Россия",55.7961,49.1064,"Europe/Moscow"),
  City("Уфа","Россия",54.7388,55.9721,"Asia/Yekaterinburg"),
  City("Махачкала","Россия",42.9849,47.5047,"Europe/Moscow"),
  City("Екатеринбург","Россия",56.8389,60.6057,"Asia/Yekaterinburg"),
  City("Самара","Россия",53.1959,50.1002,"Europe/Samara"),
  City("Краснодар","Россия",45.0355,38.9753,"Europe/Moscow"),
  City("Ростов-на-Дону","Россия",47.2357,39.7015,"Europe/Moscow"),
  City("Сочи","Россия",43.5855,39.7231,"Europe/Moscow"),
  City("Ставрополь","Россия",45.0428,41.9734,"Europe/Moscow"),
  City("Астрахань","Россия",46.3479,48.0336,"Europe/Astrakhan"),
  City("Ижевск","Россия",56.8527,53.2115,"Europe/Samara"),
  City("Оренбург","Россия",51.7682,55.0969,"Asia/Yekaterinburg"),
  City("Пермь","Россия",58.0105,56.2502,"Asia/Yekaterinburg"),
  City("Челябинск","Россия",55.1644,61.4368,"Asia/Yekaterinburg"),
  City("Астана","Казахстан",51.1694,71.4491,"Asia/Almaty"),
  City("Алматы","Казахстан",43.2389,76.8897,"Asia/Almaty"),
  City("Шымкент","Казахстан",42.3417,69.5901,"Asia/Almaty")
 )
 fun find(name:String)=list.firstOrNull{it.name==name}
}
