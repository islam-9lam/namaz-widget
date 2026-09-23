package com.islam9lam.namazwidget

import org.jsoup.Jsoup
import java.net.URLEncoder
import java.time.YearMonth

object Sources {

    data class FetchResult(
        val days: List<PrayerDay>,
        val source: String
    )

    /*
     * Только подтверждённые прямые страницы ЦДУМ.
     * Никакого автоматического угадывания URL.
     */
    private val cdumUrls = mapOf(
        "Санкт-Петербург" to
            "https://cdum.ru/time-namaz/Spb.php",

        "Астрахань" to
            "https://cdum.ru/time-namaz/astrakhan/Astrakhan.php",

        "Киров" to
            "https://cdum.ru/time-namaz/%D0%9A%D0%B8%D1%80%D0%BE%D0%B2/Kirov.php",

        "Екатеринбург" to
            "https://cdum.ru/time-namaz/ekb/Ekb2015.php",

        "Ижевск" to
            "https://cdum.ru/time-namaz/izhevsk/Izhevsk2015.php",

        "Йошкар-Ола" to
            "https://cdum.ru/time-namaz/yoshkar-ola/Yoshkar-Ola2015.php",

        "Волгоград" to
            "https://cdum.ru/time-namaz/%D0%92%D0%BE%D0%BB%D0%B3%D0%BE%D0%B3%D1%80%D0%B0%D0%B4/Volgograd.php",

        "Курган" to
            "https://cdum.ru/time-namaz/Kurgan/Kurgan.php",

        "Оренбург" to
            "https://cdum.ru/time-namaz/orenburg/Orenburg2015.php",

        "Пенза" to
            "https://cdum.ru/time-namaz/%D0%9F%D0%B5%D0%BD%D0%B7%D0%B0/Penza2015.php",

        "Пермь" to
            "https://cdum.ru/time-namaz/%D0%9F%D0%B5%D1%80%D0%BC%D1%8C/Perm.php",

        "Ростов-на-Дону" to
            "https://cdum.ru/time-namaz/Rostov-na-Dony/Rostov.php",

        "Самара" to
            "https://cdum.ru/time-namaz/samara/Samara2015.php",

        "Ульяновск" to
            "https://cdum.ru/time-namaz/Ulyanovsk/Ulyanovsk2015.php",

        "Хабаровск" to
            "https://cdum.ru/time-namaz/khabarovsk/Habarovsk2015.php",

        "Чебоксары" to
            "https://cdum.ru/time-namaz/cheboksary/Cheboksary.php",

        "Челябинск" to
            "https://cdum.ru/time-namaz/Chelyabinsk/Chelyabinsk2015.php"
    )

    /*
     * Возвращает источник, который должен использоваться
     * для конкретного города.
     *
     * Важно: это та же логика приоритетов,
     * что используется в fetch().
     */
    fun sourceName(
        cityName: String
    ): String =
        sourceName(
            Cities.find(cityName)
        )

    fun sourceName(
        city: City
    ): String {

        return when {

            city.name == "Грозный" ->
                "Govzalla"

            city.ummaSlug != null ->
                "Umma.ru"

            cdumUrls.containsKey(city.name) ->
                "ЦДУМ России"

            city.name == "Уфа" ->
                "ДУМ Республики Башкортостан"

            else ->
                "Расчёт по координатам · MWL"
        }
    }

    private fun times(
        text: String
    ): List<String> =
        Regex("""\b([0-2]?\d:[0-5]\d)\b""")
            .findAll(text)
            .map {
                it.value.padStart(5, '0')
            }
            .toList()

    fun fetch(
        cityName: String,
        ym: YearMonth
    ): FetchResult =
        fetch(
            Cities.find(cityName),
            ym
        )

    fun fetch(
        city: City,
        ym: YearMonth
    ): FetchResult {

        // ---------------------------------
        // 1. ГРОЗНЫЙ — ТОЛЬКО GOVZALLA
        // ---------------------------------

        if (city.name == "Грозный") {

            return FetchResult(
                fetchGovzalla(
                    city,
                    ym
                ),
                "Govzalla"
            )
        }

        // ---------------------------------
        // 2. UMMA — ТОЛЬКО WHITELIST
        // ---------------------------------

        val slug =
            city.ummaSlug

        if (slug != null) {

            val result =
                fetchUmma(
                    city,
                    slug,
                    ym
                )

            if (result.isEmpty()) {
                throw IllegalStateException(
                    "Umma.ru не вернул расписание: ${city.name}"
                )
            }

            return FetchResult(
                result,
                "Umma.ru"
            )
        }

        // ---------------------------------
        // 3. ЦДУМ
        // ---------------------------------

        val cdumUrl =
            cdumUrls[city.name]

        if (cdumUrl != null) {

            return FetchResult(
                fetchCdum(
                    city,
                    cdumUrl,
                    ym
                ),
                "ЦДУМ России"
            )
        }

        // ---------------------------------
        // 4. УФА — ДУМ РЕСПУБЛИКИ БАШКОРТОСТАН
        // ---------------------------------

        if (city.name == "Уфа") {

            return FetchResult(
                fetchDumRb(
                    city,
                    ym
                ),
                "ДУМ Республики Башкортостан"
            )
        }

        // ---------------------------------
        // 5. РАСЧЁТ ПО КООРДИНАТАМ
        // ---------------------------------
        //
        // Используется ТОЛЬКО если выше для города
        // не найден официальный/выбранный источник.
        //
        // Метод:
        // Muslim World League (18° / 17°)
        // High latitude: Twilight Angle
        //
        // В координатном режиме:
        // asr       = Аср по Шафи'и
        // asrHanafi = Аср по Ханафи

        return FetchResult(
            CoordinatePrayerCalculator.calculate(
                city,
                ym
            ),
            "Расчёт по координатам · MWL"
        )
    }

    /*
     * UMMA
     */
    private fun fetchUmma(
        city: City,
        slug: String,
        ym: YearMonth
    ): List<PrayerDay> {

        val url =
            "https://umma.ru/raspisanie-namaza/$slug"

        val doc =
            Jsoup.connect(url)
                .userAgent(
                    "Mozilla/5.0 (Linux; Android 14) " +
                    "AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
                )
                .timeout(15000)
                .get()

        /*
         * Защита от ситуации:
         * запросили другой город, а получили Москву.
         */
        val pageText =
            doc.text()
                .lowercase()

        val cityName =
            city.name
                .lowercase()

        if (!pageText.contains(cityName)) {

            throw IllegalStateException(
                "Umma.ru вернул страницу другого города вместо ${city.name}"
            )
        }

        val out =
            mutableListOf<PrayerDay>()

        doc.select("tr").forEach { tr ->

            val cells =
                tr.select("td")
                    .map {
                        it.text().trim()
                    }

            val day =
                cells.firstOrNull()
                    ?.let {
                        Regex("""\d{1,2}""")
                            .find(it)
                            ?.value
                            ?.toIntOrNull()
                    }

            if (
                day != null &&
                day in 1..ym.lengthOfMonth()
            ) {

                val ts =
                    times(
                        cells.drop(1)
                            .joinToString(" ")
                    )

                if (ts.size >= 6) {

                    out +=
                        PrayerDay(
                            day,
                            ts[0],
                            ts[1],
                            ts[2],
                            ts[3],
                            ts[4],
                            ts[5]
                        )
                }
            }
        }

        return out
            .distinctBy {
                it.day
            }
            .sortedBy {
                it.day
            }
    }

    /*
     * ЦДУМ
     *
     * На странице находится весь год.
     * Поэтому недостаточно просто собрать строки 1..31:
     * иначе получили бы январь.
     *
     * Ищем конкретный заголовок месяца,
     * затем читаем таблицу сразу после него.
     */
    private fun fetchCdum(
        city: City,
        url: String,
        ym: YearMonth
    ): List<PrayerDay> {

        val doc =
            Jsoup.connect(url)
                .userAgent(
                    "Mozilla/5.0 (Linux; Android 14) " +
                    "AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
                )
                .timeout(20000)
                .get()

        // Проверяем, что получили нужный город.
        val title =
            doc.selectFirst("h1")
                ?.text()
                ?.trim()
                .orEmpty()

        if (
            title.isNotEmpty() &&
            !title.contains(
                city.name,
                ignoreCase = true
            )
        ) {

            throw IllegalStateException(
                "ЦДУМ вернул страницу '$title' вместо '${city.name}'"
            )
        }

        val monthNames =
            listOf(
                "Январь",
                "Февраль",
                "Март",
                "Апрель",
                "Май",
                "Июнь",
                "Июль",
                "Август",
                "Сентябрь",
                "Октябрь",
                "Ноябрь",
                "Декабрь"
            )

        val wantedMonth =
            monthNames[
                ym.monthValue - 1
            ]

        /*
         * На старых страницах ЦДУМ HTML не очень современный.
         * Поэтому надёжнее найти таблицу,
         * перед которой в документе расположен нужный месяц.
         */
        val tables =
            doc.select("table")

        var targetTable:
            org.jsoup.nodes.Element? = null

        for (table in tables) {

            val tableText =
                table.text()

            /*
             * Некоторые страницы содержат название месяца
             * внутри той же таблицы.
             */
            if (
                tableText.contains(
                    wantedMonth,
                    ignoreCase = true
                ) &&
                tableText.contains(
                    "Фаджр",
                    ignoreCase = true
                ) &&
                tableText.contains(
                    "Восход",
                    ignoreCase = true
                )
            ) {

                targetTable =
                    table

                break
            }

            /*
             * На других страницах месяц расположен
             * непосредственно перед таблицей.
             */
            var previous =
                table.previousElementSibling()

            var checks = 0

            while (
                previous != null &&
                checks < 4
            ) {

                if (
                    previous.text()
                        .contains(
                            wantedMonth,
                            ignoreCase = true
                        )
                ) {

                    targetTable =
                        table

                    break
                }

                previous =
                    previous.previousElementSibling()

                checks++
            }

            if (targetTable != null)
                break
        }

        if (targetTable == null) {

            /*
             * Запасной вариант для старой вёрстки:
             * ищем строки по всему документу,
             * но ограничиваемся областью между
             * заголовками текущего и следующего месяца.
             */
            val bodyText =
                doc.body()
                    .text()

            if (
                !bodyText.contains(
                    wantedMonth,
                    ignoreCase = true
                )
            ) {

                throw IllegalStateException(
                    "На странице ЦДУМ не найден месяц $wantedMonth"
                )
            }

            return parseCdumByMonthBlocks(
                doc,
                ym,
                wantedMonth,
                monthNames
            )
        }

        val result =
            parseCdumRows(
                targetTable!!,
                ym
            )

        if (result.size < 28) {

            throw IllegalStateException(
                "ЦДУМ: для ${city.name}, $wantedMonth получено только " +
                "${result.size} дней"
            )
        }

        return result
    }

    private fun parseCdumRows(
        root: org.jsoup.nodes.Element,
        ym: YearMonth
    ): List<PrayerDay> {

        val out =
            mutableListOf<PrayerDay>()

        root.select("tr")
            .forEach { tr ->

                val cells =
                    tr.select("td")
                        .map {
                            it.text()
                                .trim()
                        }

                if (cells.isEmpty())
                    return@forEach

                val day =
                    Regex("""^\s*(\d{1,2})\b""")
                        .find(
                            cells.first()
                        )
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()

                if (
                    day == null ||
                    day !in 1..ym.lengthOfMonth()
                ) {
                    return@forEach
                }

                val ts =
                    times(
                        cells.drop(1)
                            .joinToString(" ")
                    )

                if (ts.size >= 6) {

                    out +=
                        PrayerDay(
                            day,
                            ts[0],
                            ts[1],
                            ts[2],
                            ts[3],
                            ts[4],
                            ts[5]
                        )
                }
            }

        return out
            .distinctBy {
                it.day
            }
            .sortedBy {
                it.day
            }
    }

    private fun parseCdumByMonthBlocks(
        doc: org.jsoup.nodes.Document,
        ym: YearMonth,
        wantedMonth: String,
        monthNames: List<String>
    ): List<PrayerDay> {

        /*
         * Ищем элемент, содержащий ровно название месяца,
         * затем ближайшую следующую таблицу.
         */
        val candidates =
            doc.getAllElements()
                .filter {
                    it.ownText()
                        .trim()
                        .equals(
                            wantedMonth,
                            ignoreCase = true
                        )
                }

        for (candidate in candidates) {

            var node:
                org.jsoup.nodes.Element? =
                candidate

            var steps = 0

            while (
                node != null &&
                steps < 12
            ) {

                val table =
                    if (
                        node.tagName()
                            .equals(
                                "table",
                                ignoreCase = true
                            )
                    ) {
                        node
                    } else {
                        node.selectFirst("table")
                    }

                if (table != null) {

                    val parsed =
                        parseCdumRows(
                            table,
                            ym
                        )

                    if (
                        parsed.size >= 28
                    ) {
                        return parsed
                    }
                }

                node =
                    node.nextElementSibling()

                steps++
            }
        }

        throw IllegalStateException(
            "Не удалось разобрать таблицу ЦДУМ: $wantedMonth"
        )
    }

    /*
     * ДУМ Республики Башкортостан.
     *
     * Официальный API сайта dumrb.com:
     *
     * /Prayer/GetByCity
     * cityName
     * year
     * month
     *
     * Сейчас используем только для Уфы.
     */
    private fun fetchDumRb(
        city: City,
        ym: YearMonth
    ): List<PrayerDay> {

        val url =
            "https://api.dumrb.com/Prayer/GetByCity" +
            "?cityName=" +
            URLEncoder.encode(
                city.name,
                "UTF-8"
            ) +
            "&year=" +
            ym.year +
            "&month=" +
            ym.monthValue

        val body =
            Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(
                    "Mozilla/5.0 (Linux; Android 14) " +
                    "AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
                )
                .timeout(20000)
                .execute()
                .body()

        val root =
            org.json.JSONObject(body)

        val array =
            root.optJSONArray(
                "prayerTimes"
            )
                ?: throw IllegalStateException(
                    "ДУМ РБ не вернул prayerTimes для ${city.name}"
                )

        fun normalizeTime(
            value: String?
        ): String {

            if (value.isNullOrBlank())
                return ""

            val text =
                value.trim()

            /*
             * API может вернуть HH:mm,
             * HH:mm:ss либо время с точкой.
             */
            val colon =
                Regex(
                    """\b([0-2]?\d):([0-5]\d)(?::[0-5]\d)?\b"""
                )
                    .find(text)

            if (colon != null) {

                val h =
                    colon.groupValues[1]
                        .toInt()

                val m =
                    colon.groupValues[2]
                        .toInt()

                return String.format(
                    java.util.Locale.US,
                    "%02d:%02d",
                    h,
                    m
                )
            }

            val dot =
                Regex(
                    """\b([0-2]?\d)\.([0-5]\d)\b"""
                )
                    .find(text)

            if (dot != null) {

                val h =
                    dot.groupValues[1]
                        .toInt()

                val m =
                    dot.groupValues[2]
                        .toInt()

                return String.format(
                    java.util.Locale.US,
                    "%02d:%02d",
                    h,
                    m
                )
            }

            return ""
        }

        fun getTime(
            obj: org.json.JSONObject,
            vararg names: String
        ): String {

            for (name in names) {

                if (
                    obj.has(name) &&
                    !obj.isNull(name)
                ) {

                    val value =
                        normalizeTime(
                            obj.optString(name)
                        )

                    if (value.isNotEmpty())
                        return value
                }
            }

            return ""
        }

        fun findDay(
            obj: org.json.JSONObject,
            index: Int
        ): Int {

            /*
             * Сначала пробуем явные числовые поля.
             */
            val numericNames =
                listOf(
                    "day",
                    "dayOfMonth"
                )

            for (name in numericNames) {

                if (obj.has(name)) {

                    val d =
                        obj.optInt(
                            name,
                            -1
                        )

                    if (
                        d in 1..ym.lengthOfMonth()
                    ) {
                        return d
                    }
                }
            }

            /*
             * Если API хранит полную дату —
             * достаём YYYY-MM-DD.
             */
            val dateNames =
                listOf(
                    "date",
                    "prayerDate",
                    "dayDate"
                )

            for (name in dateNames) {

                val value =
                    obj.optString(
                        name,
                        ""
                    )

                val match =
                    Regex(
                        """(\d{4})-(\d{2})-(\d{2})"""
                    )
                        .find(value)

                if (match != null) {

                    val year =
                        match.groupValues[1]
                            .toInt()

                    val month =
                        match.groupValues[2]
                            .toInt()

                    val day =
                        match.groupValues[3]
                            .toInt()

                    if (
                        year == ym.year &&
                        month == ym.monthValue &&
                        day in 1..ym.lengthOfMonth()
                    ) {
                        return day
                    }
                }
            }

            /*
             * API отдаёт месяц последовательно.
             * Это последний резерв на случай,
             * если отдельного поля day нет.
             */
            return index + 1
        }

        val out =
            mutableListOf<PrayerDay>()

        for (
            i in 0 until array.length()
        ) {

            val obj =
                array.optJSONObject(i)
                    ?: continue

            val day =
                findDay(
                    obj,
                    i
                )

            if (
                day !in 1..ym.lengthOfMonth()
            ) {
                continue
            }

            val fajr =
                getTime(
                    obj,
                    "fajr",
                    "fadjr",
                    "fajer"
                )

            val sunrise =
                getTime(
                    obj,
                    "sunrise",
                    "shuruq",
                    "shuruk",
                    "sunRise"
                )

            val dhuhr =
                getTime(
                    obj,
                    "dhuhr",
                    "duhr",
                    "zuhr",
                    "zuhur",
                    "zuhra"
                )

            val asr =
                getTime(
                    obj,
                    "asr"
                )

            val maghrib =
                getTime(
                    obj,
                    "maghrib",
                    "magrib",
                    "magreb",
                    "magribTime"
                )

            val isha =
                getTime(
                    obj,
                    "isha",
                    "ishaA"
                )

            if (
                fajr.isEmpty() ||
                sunrise.isEmpty() ||
                dhuhr.isEmpty() ||
                asr.isEmpty() ||
                maghrib.isEmpty() ||
                isha.isEmpty()
            ) {

                throw IllegalStateException(
                    "ДУМ РБ вернул неполное расписание: " +
                    "${city.name}, день $day"
                )
            }

            out +=
                PrayerDay(
                    day,
                    fajr,
                    sunrise,
                    dhuhr,
                    asr,
                    maghrib,
                    isha
                )
        }

        val result =
            out
                .distinctBy {
                    it.day
                }
                .sortedBy {
                    it.day
                }

        if (result.isEmpty()) {

            throw IllegalStateException(
                "ДУМ РБ не вернул расписание: " +
                "${city.name}, ${ym.monthValue}.${ym.year}"
            )
        }

        return result
    }


    /*
     * GOVZALLA — только Грозный.
     */
    private fun fetchGovzalla(
        city: City,
        ym: YearMonth
    ): List<PrayerDay> {

        val url =
            "https://govzalla.com/ламазан-хенаш-время-молитв?city=" +
            URLEncoder.encode(
                city.name,
                "UTF-8"
            ) +
            "&month=" +
            ym.monthValue

        val doc =
            Jsoup.connect(url)
                .userAgent(
                    "Mozilla/5.0 (Linux; Android 14) " +
                    "AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
                )
                .timeout(15000)
                .get()

        val out =
            mutableListOf<PrayerDay>()

        doc.select("tr")
            .forEach { tr ->

                val cells =
                    tr.select("td")
                        .map {
                            it.text().trim()
                        }

                val day =
                    cells.firstOrNull()
                        ?.let {
                            Regex("""\d{1,2}""")
                                .find(it)
                                ?.value
                                ?.toIntOrNull()
                        }

                if (
                    day != null &&
                    day in 1..ym.lengthOfMonth()
                ) {

                    val ts =
                        times(
                            cells.drop(1)
                                .joinToString(" ")
                        )

                    if (ts.size >= 6) {

                        out +=
                            PrayerDay(
                                day,
                                ts[0],
                                ts[1],
                                ts[2],
                                ts[3],
                                ts[4],
                                ts[5]
                            )
                    }
                }
            }

        if (out.isEmpty()) {

            throw IllegalStateException(
                "Не удалось получить расписание Govzalla: ${city.name}"
            )
        }

        return out
            .distinctBy {
                it.day
            }
            .sortedBy {
                it.day
            }
    }
}
