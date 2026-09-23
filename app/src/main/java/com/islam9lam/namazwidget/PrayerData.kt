package com.islam9lam.namazwidget

data class PrayerDay(
    val day: Int,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,

    // Для официальных источников:
    // asr = время Асра, которое дал сам источник.
    //
    // Для расчёта по координатам:
    // asr = Аср по Шафи'и
    // asrHanafi = Аср по Ханафи
    val asr: String,
    val maghrib: String,
    val isha: String,
    val asrHanafi: String? = null
)

data class City(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val timeZone: String,
    val ummaSlug: String? = null
)

object Cities {

    val list = listOf(

        // ---------------- RUSSIA ----------------

        // Подтверждённый город Umma
        City(
            "Москва",
            "Россия",
            55.7558,
            37.6173,
            "Europe/Moscow",
            "moscow"
        ),

        // Подтверждённый город Umma
        City(
            "Казань",
            "Россия",
            55.7961,
            49.1064,
            "Europe/Moscow",
            "kazan"
        ),

        // Грозный НЕ использует Umma.
        // Для него отдельный источник Govzalla.
        City(
            "Грозный",
            "Россия",
            43.3180,
            45.6982,
            "Europe/Moscow",
            null
        ),

        City(
            "Санкт-Петербург",
            "Россия",
            59.9343,
            30.3351,
            "Europe/Moscow",
            null
        ),

        City(
            "Уфа",
            "Россия",
            54.7388,
            55.9721,
            "Asia/Yekaterinburg",
            null
        ),

        City(
            "Самара",
            "Россия",
            53.1959,
            50.1002,
            "Europe/Samara",
            null
        ),


        City(
            "Екатеринбург",
            "Россия",
            56.8389,
            60.6057,
            "Asia/Yekaterinburg",
            null
        ),


        City(
            "Ростов-на-Дону",
            "Россия",
            47.2357,
            39.7015,
            "Europe/Moscow",
            null
        ),




        // ---------------- ADDITIONAL CDUM CITIES ----------------

        City(
            "Астрахань",
            "Россия",
            46.3479,
            48.0336,
            "Europe/Astrakhan",
            null
        ),

        City(
            "Киров",
            "Россия",
            58.6035,
            49.6679,
            "Europe/Kirov",
            null
        ),

        City(
            "Ижевск",
            "Россия",
            56.8527,
            53.2115,
            "Europe/Samara",
            null
        ),

        City(
            "Йошкар-Ола",
            "Россия",
            56.6344,
            47.8998,
            "Europe/Moscow",
            null
        ),

        City(
            "Волгоград",
            "Россия",
            48.7080,
            44.5133,
            "Europe/Volgograd",
            null
        ),

        City(
            "Курган",
            "Россия",
            55.4443,
            65.3161,
            "Asia/Yekaterinburg",
            null
        ),

        City(
            "Оренбург",
            "Россия",
            51.7682,
            55.0969,
            "Asia/Yekaterinburg",
            null
        ),

        City(
            "Пенза",
            "Россия",
            53.1959,
            45.0183,
            "Europe/Moscow",
            null
        ),

        City(
            "Пермь",
            "Россия",
            58.0105,
            56.2502,
            "Asia/Yekaterinburg",
            null
        ),

        City(
            "Ульяновск",
            "Россия",
            54.3142,
            48.4031,
            "Europe/Ulyanovsk",
            null
        ),

        City(
            "Хабаровск",
            "Россия",
            48.4802,
            135.0719,
            "Asia/Vladivostok",
            null
        ),

        City(
            "Чебоксары",
            "Россия",
            56.1439,
            47.2489,
            "Europe/Moscow",
            null
        ),

        City(
            "Челябинск",
            "Россия",
            55.1644,
            61.4368,
            "Asia/Yekaterinburg",
            null
        ),


        // ---------------- KAZAKHSTAN ----------------

        City(
            "Астана",
            "Казахстан",
            51.1694,
            71.4491,
            "Asia/Almaty",
            "astana"
        ),

        City(
            "Алматы",
            "Казахстан",
            43.2389,
            76.8897,
            "Asia/Almaty",
            "almaty"
        ),

        City(
            "Актау",
            "Казахстан",
            43.6532,
            51.1975,
            "Asia/Aqtau",
            "aktau"
        ),

        City(
            "Атырау",
            "Казахстан",
            47.0945,
            51.9238,
            "Asia/Atyrau",
            "atyrau"
        ),

        City(
            "Караганда",
            "Казахстан",
            49.8064,
            73.0855,
            "Asia/Almaty",
            "karaganda"
        ),

        City(
            "Шымкент",
            "Казахстан",
            42.3417,
            69.5901,
            "Asia/Almaty",
            "shymkent"
        ),

        City(
            "Костанай",
            "Казахстан",
            53.2144,
            63.6246,
            "Asia/Qostanay",
            "kostanay"
        ),

        City(
            "Уральск",
            "Казахстан",
            51.2278,
            51.3865,
            "Asia/Oral",
            "uralsk"
        ),







    )

    fun find(name: String): City =
        list.firstOrNull {
            it.name == name
        } ?: throw IllegalArgumentException(
            "Неизвестный встроенный город: $name"
        )

    fun countries(): List<String> =
        list.map {
            it.country
        }.distinct()

    fun forCountry(country: String): List<City> =
        list.filter {
            it.country == country
        }
}
