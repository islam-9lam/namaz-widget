package com.islam9lam.namazwidget

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.work.*
import java.time.*
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null
    private lateinit var root: LinearLayout
    private var enableAodAfterPermission = false
    private var showingMainScreen = true

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted && enableAodAfterPermission) {
                enableAodAfterPermission = false
                PrayerAod.enable(this)
                showMainScreen()
            } else if (enableAodAfterPermission) {
                enableAodAfterPermission = false
                Store.setAodEnabled(this, false)
                showMainScreen()
            }
        }

    private val bg = Color.rgb(248, 248, 250)
    private val dark = Color.rgb(35, 35, 40)
    private val muted = Color.rgb(105, 108, 108)
    private val sage = Color.rgb(72, 96, 83)
    private val sageLight = Color.rgb(231, 237, 233)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        PrayerSyncScheduler.schedule(applicationContext)

        showMainScreen()

        if (!Store.sourcesRestored(this)) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance(this).enqueueUniqueWork(
                "restore_original_sources",
                ExistingWorkPolicy.KEEP,
                request
            )
            WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.id)
                .observe(this) { info ->
                    if (info?.state == WorkInfo.State.SUCCEEDED && ::root.isInitialized) {
                        showMainScreen()
                    }
                }
        }

        if (Store.isAodEnabled(this) &&
            (android.os.Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        ) {
            PrayerAod.enable(this)
        }
    }

    override fun onResume() {
        super.onResume()

        if (::root.isInitialized) {
            showMainScreen()
        }
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    private fun text(
        value: String,
        size: Float,
        bold: Boolean = false,
        color: Int = dark
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)

        if (bold) {
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun space(h: Int) =
        Space(this).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    1,
                    dp(h)
                )
        }

    private fun rounded(
        color: Int,
        radius: Int
    ) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun showMainScreen() {

        showingMainScreen = true

        timer?.cancel()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(20),
                dp(38),
                dp(20),
                dp(10)
            )
            setBackgroundColor(bg)
        }

        val city = Store.city(this)
        val today = CityClock.date(this)
        val ym = YearMonth.from(today)

        val todayData =
            Store.load(
                this,
                "$city-$ym"
            ).firstOrNull {
                it.day == today.dayOfMonth
            }

        // ---------- HEADER ----------

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        val cityBlock =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

        cityBlock.addView(
            text(
                city,
                26f,
                true
            )
        )

        val locale = Locale("ru", "RU")

        val dateFormatter =
            DateTimeFormatter.ofPattern(
                "EEEE, d MMMM",
                locale
            )

        cityBlock.addView(
            text(
                today.format(dateFormatter)
                    .replaceFirstChar {
                        if (it.isLowerCase())
                            it.titlecase(locale)
                        else
                            it.toString()
                    },
                13f,
                false,
                muted
            )
        )

        header.addView(
            cityBlock,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val cityButton =
            Button(this).apply {
                text = "Город"
                textSize = 12f
                isAllCaps = false

                setOnClickListener {
                    showCityScreen()
                }
            }

        header.addView(cityButton)

        root.addView(header)

        root.addView(space(16))

        // ---------- NO DATA ----------

        if (todayData == null) {

            val empty =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(
                        dp(20),
                        dp(30),
                        dp(20),
                        dp(30)
                    )
                    background =
                        rounded(
                            Color.WHITE,
                            24
                        )
                }

            empty.addView(
                text(
                    "Нет расписания на сегодня",
                    19f,
                    true
                )
            )

            empty.addView(space(8))

            empty.addView(
                text(
                    "Подключитесь к интернету и обновите расписание.",
                    14f,
                    false,
                    muted
                )
            )

            empty.addView(space(18))

            val update =
                Button(this).apply {

                    text = "Обновить"
                    isAllCaps = false

                    setOnClickListener {

                        text = "Загрузка…"
                        isEnabled = false

                        val request =
                            OneTimeWorkRequestBuilder<SyncWorker>()
                                .build()

                        WorkManager
                            .getInstance(this@MainActivity)
                            .enqueue(request)

                        WorkManager
                            .getInstance(this@MainActivity)
                            .getWorkInfoByIdLiveData(
                                request.id
                            )
                            .observe(
                                this@MainActivity
                            ) { info ->

                                if (
                                    info != null &&
                                    info.state.isFinished
                                ) {
                                    showMainScreen()
                                }
                            }
                    }
                }

            empty.addView(update)

            root.addView(
                empty,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )

            addBottomNavigation()
            setContentView(root)
            return
        }

        // Официальные источники содержат один Аср.
        //
        // Для городов, рассчитанных по координатам:
        // asr       = Аср по Шафи'и
        // asrHanafi = Аср по Ханафи
        //
        // Ханафитский Аср пока отображается только
        // как дополнительное время. Логику следующего
        // намаза и виджета изменим отдельной настройкой.

        val prayers =
            buildList {
                add(
                    PrayerTime(
                        "Фаджр",
                        todayData.fajr
                    )
                )

                add(
                    PrayerTime(
                        "Восход",
                        todayData.sunrise
                    )
                )

                add(
                    PrayerTime(
                        "Зухр",
                        todayData.dhuhr
                    )
                )

                if (todayData.asrHanafi != null) {

                    add(
                        PrayerTime(
                            "Аср (Шафи‘и)",
                            todayData.asr
                        )
                    )

                    add(
                        PrayerTime(
                            "Аср (Ханафи)",
                            todayData.asrHanafi
                        )
                    )

                } else {

                    add(
                        PrayerTime(
                            "Аср",
                            todayData.asr
                        )
                    )
                }

                add(
                    PrayerTime(
                        "Магриб",
                        todayData.maghrib
                    )
                )

                add(
                    PrayerTime(
                        "Иша",
                        todayData.isha
                    )
                )
            }

        val state =
            PrayerStateCalculator.calculate(this)

        val nextName =
            state?.nextPrayer?.name ?: "—"

        val nextTime =
            state?.nextPrayer?.time ?: "—"

        val nextDateTime =
            state?.nextDateTime

        // ---------- NEXT PRAYER ----------

        val card =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(16),
                    dp(20),
                    dp(16)
                )

                background =
                    rounded(
                        sage,
                        24
                    )

                elevation =
                    dp(2).toFloat()
            }

        card.addView(
            text(
                "Следующий намаз",
                12f,
                false,
                Color.rgb(220, 230, 224)
            )
        )

        val nextRow =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        nextRow.addView(
            text(
                nextName,
                28f,
                true,
                Color.WHITE
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        nextRow.addView(
            text(
                nextTime,
                28f,
                true,
                Color.WHITE
            )
        )

        card.addView(nextRow)

        val countdown =
            text(
                "",
                15f,
                true,
                Color.rgb(235, 240, 237)
            )

        card.addView(countdown)

        root.addView(card)

        root.addView(space(15))

        // ---------- TODAY GRID ----------

        val todayHeader =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        todayHeader.addView(
            text(
                "Сегодня",
                18f,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val currentPrayer =
            state?.currentPrayer

        if (currentPrayer != null) {
            todayHeader.addView(
                text(
                    "Сейчас: ${currentPrayer.name} ${currentPrayer.time}",
                    12f,
                    true,
                    sage
                )
            )
        }

        root.addView(todayHeader)

        root.addView(space(8))

        val schedule =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                background =
                    rounded(
                        Color.WHITE,
                        22
                    )

                setPadding(
                    dp(10),
                    dp(5),
                    dp(10),
                    dp(5)
                )
            }

        prayers.forEachIndexed { index, prayer ->

            schedule.addView(
                prayerRow(
                    prayer,
                    prayer.name == nextName,
                    currentPrayer?.name == prayer.name ||
                        (
                            currentPrayer?.name == "Аср" &&
                            prayer.name == "Аср (Шафи‘и)"
                        )
                ),
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )

            if (index < prayers.lastIndex) {

                val divider =
                    View(this).apply {

                        setBackgroundColor(
                            Color.rgb(
                                235,
                                236,
                                238
                            )
                        )
                    }

                schedule.addView(
                    divider,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).apply {
                        leftMargin = dp(12)
                        rightMargin = dp(12)
                    }
                )
            }
        }

        root.addView(
            schedule,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // ---------- ASR METHOD ----------
        //
        // Показываем переключатель только для расписаний,
        // рассчитанных по координатам.
        // У официальных источников asrHanafi == null.

        if (todayData.asrHanafi != null) {

            root.addView(space(8))

            val asrMethodRow =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(12),
                        dp(5),
                        dp(12),
                        dp(5)
                    )

                    background =
                        rounded(
                            Color.rgb(
                                245,
                                247,
                                246
                            ),
                            16
                        )
                }

            asrMethodRow.addView(
                text(
                    "Расчёт Асра",
                    12f,
                    true
                ),
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val currentAsrMethod =
                Store.asrMethod(this)

            val shafiButton =
                text(
                    "Шафи‘и",
                    12f,
                    currentAsrMethod == "shafi",
                    if (currentAsrMethod == "shafi")
                        sage
                    else
                        muted
                )

            shafiButton.setPadding(
                dp(10),
                dp(7),
                dp(10),
                dp(7)
            )

            shafiButton.setOnClickListener {

                if (Store.asrMethod(this) != "shafi") {

                    Store.setAsrMethod(
                        this,
                        "shafi"
                    )

                    PrayerWidget.refreshAll(
                        applicationContext
                    )

                    showMainScreen()
                }
            }

            asrMethodRow.addView(
                shafiButton
            )

            val separator =
                text(
                    " | ",
                    12f,
                    false,
                    muted
                )

            asrMethodRow.addView(
                separator
            )

            val hanafiButton =
                text(
                    "Ханафи",
                    12f,
                    currentAsrMethod == "hanafi",
                    if (currentAsrMethod == "hanafi")
                        sage
                    else
                        muted
                )

            hanafiButton.setPadding(
                dp(10),
                dp(7),
                dp(10),
                dp(7)
            )

            hanafiButton.setOnClickListener {

                if (Store.asrMethod(this) != "hanafi") {

                    Store.setAsrMethod(
                        this,
                        "hanafi"
                    )

                    PrayerWidget.refreshAll(
                        applicationContext
                    )

                    showMainScreen()
                }
            }

            asrMethodRow.addView(
                hanafiButton
            )

            root.addView(asrMethodRow)
        }

        // ---------- STATUS ----------

        root.addView(space(10))

        val source =
            Store.source(this).ifBlank {
                Sources.sourceName(
                    CityClock.city(this)
                )
            }

        val status =
            text(
                "Источник: $source · расписание сохранено офлайн",
                11f,
                false,
                muted
            )

        status.gravity =
            Gravity.CENTER

        root.addView(status)

        root.addView(space(8))

        addAodToggle()

        // Занимает свободное место и прижимает навигацию вниз.
        root.addView(
            Space(this),
            LinearLayout.LayoutParams(
                1,
                0,
                1f
            )
        )

        addBottomNavigation()

        setContentView(root)

        if (nextDateTime != null) {
            startCountdown(
                nextDateTime,
                countdown
            )
        }
    }

    private fun prayerRow(
        prayer: PrayerTime,
        isNext: Boolean,
        isCurrent: Boolean
    ): View {

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(14),
                    dp(3),
                    dp(14),
                    dp(3)
                )

                if (isCurrent) {
                    background =
                        rounded(
                            sageLight,
                            15
                        )
                }
            }

        val nameColor =
            if (isCurrent || isNext)
                sage
            else
                dark

        row.addView(
            text(
                prayer.name,
                16f,
                isCurrent || isNext,
                nameColor
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        if (isCurrent) {

            row.addView(
                text(
                    "Сейчас",
                    11f,
                    true,
                    sage
                )
            )

            row.addView(
                Space(this).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            dp(12),
                            1
                        )
                }
            )
        }

        row.addView(
            text(
                prayer.time,
                17f,
                isCurrent || isNext,
                nameColor
            )
        )

        return row
    }

    private fun addAodToggle() {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(8), dp(4))
            background = rounded(Color.WHITE, 16)
        }

        row.addView(
            text("Always on Display", 13f, true),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        val toggle = Switch(this).apply {
            isChecked = Store.isAodEnabled(this@MainActivity)
        }

        toggle.setOnCheckedChangeListener { _, enabled ->
            if (!enabled) {
                enableAodAfterPermission = false
                PrayerAod.disable(this)
                return@setOnCheckedChangeListener
            }

            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                enableAodAfterPermission = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                PrayerAod.enable(this)
            }
        }

        row.addView(toggle)
        root.addView(row)
    }

    private fun addBottomNavigation(selected: String = "Сегодня") {

        val nav =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(2),
                    dp(6),
                    dp(2),
                    0
                )
            }

        nav.addView(
            navItem(
                "Сегодня",
                "●",
                selected == "Сегодня",
                action = { if (selected != "Сегодня") showMainScreen() }
            ),
            LinearLayout.LayoutParams(
                0,
                dp(58),
                1f
            )
        )

        nav.addView(
            navItem(
                "Месяц",
                "≡",
                selected == "Месяц",
                action = { if (selected != "Месяц") showMonthScreen() }
            ),
            LinearLayout.LayoutParams(0, dp(58), 1f)
        )

        nav.addView(
            navItem(
                "Кыбла",
                "↑",
                false,
                action = {
                    startActivity(
                        Intent(
                            this,
                            QiblaActivity::class.java
                        )
                    )
                }
            ),
            LinearLayout.LayoutParams(
                0,
                dp(58),
                1f
            )
        )

        root.addView(nav)
    }

    private fun showMonthScreen() {
        showingMainScreen = false
        timer?.cancel()

        val city = CityClock.city(this)
        val today = CityClock.date(this)
        val month = YearMonth.from(today)
        val days = Store.load(this, "${city.name}-$month")
        val locale = Locale("ru", "RU")

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(38), dp(20), dp(10))
            setBackgroundColor(bg)
        }

        root.addView(text("Расписание", 26f, true))
        root.addView(
            text(
                "${city.name} · ${month.month.getDisplayName(java.time.format.TextStyle.FULL, locale)} ${month.year}",
                13f,
                false,
                muted
            )
        )
        root.addView(space(12))

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        days.forEach { day ->
            val date = month.atDay(day.day)
            val holiday = islamicHoliday(date)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(9), dp(14), dp(9))
                background = rounded(if (date == today) sageLight else Color.WHITE, 16)
            }

            val heading = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            heading.addView(
                text(
                    "${day.day}, ${date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale)}",
                    14f,
                    true,
                    if (date == today) sage else dark
                ),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            if (holiday != null) heading.addView(text(holiday, 11f, true, sage))
            row.addView(heading)

            val asr = Store.selectedAsr(this, day)
            row.addView(
                text(
                    "Фаджр ${day.fajr}   Восход ${day.sunrise}   Зухр ${day.dhuhr}\nАср $asr   Магриб ${day.maghrib}   Иша ${day.isha}",
                    12f,
                    false,
                    muted
                )
            )
            list.addView(row)
            list.addView(space(6))
        }

        if (days.isEmpty()) {
            list.addView(text("Нет сохранённого расписания на этот месяц.", 15f, false, muted))
        }

        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        addBottomNavigation("Месяц")
        setContentView(root)
    }

    private fun islamicHoliday(date: LocalDate): String? {
        val hijri = HijrahDate.from(date)
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        return when (month to day) {
            1 to 1 -> "Новый год"
            1 to 10 -> "Ашура"
            3 to 12 -> "Мавлид"
            7 to 27 -> "Исра и Мирадж"
            8 to 15 -> "Ночь Бараат"
            9 to 1 -> "Рамадан"
            9 to 27 -> "Ночь Предопределения"
            10 to 1 -> "Ураза-байрам"
            12 to 10 -> "Курбан-байрам"
            else -> null
        }
    }

    private fun navItem(
        title: String,
        icon: String,
        selected: Boolean,
        action: (() -> Unit)? = null
    ): View {

        return LinearLayout(this).apply {

            orientation =
                LinearLayout.VERTICAL

            gravity =
                Gravity.CENTER

            isClickable =
                action != null

            isFocusable =
                action != null

            if (selected) {
                background =
                    rounded(
                        sageLight,
                        18
                    )
            }

            addView(
                text(
                    icon,
                    18f,
                    true,
                    if (selected) sage else muted
                )
            )

            addView(
                text(
                    title,
                    11f,
                    selected,
                    if (selected) sage else muted
                )
            )

            setOnClickListener {
                action?.invoke()
            }
        }
    }

    private fun startCountdown(
        target: LocalDateTime,
        view: TextView
    ) {

        timer?.cancel()

        var millis =
            Duration.between(
                CityClock.localDateTime(this),
                target
            ).toMillis()

        if (millis < 0)
            millis = 0

        timer =
            object :
                CountDownTimer(
                    millis,
                    1000
                ) {

                override fun onTick(
                    left: Long
                ) {

                    val total =
                        left / 1000

                    val hours =
                        total / 3600

                    val minutes =
                        (total % 3600) / 60

                    val seconds =
                        total % 60

                    view.text =
                        "через %02d:%02d:%02d".format(
                            hours,
                            minutes,
                            seconds
                        )
                }

                override fun onFinish() {
                    showMainScreen()
                }
            }.start()
    }

    private fun showCityScreen() {

        showingMainScreen = false

        timer?.cancel()

        val box =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(24),
                    dp(42),
                    dp(24),
                    dp(24)
                )

                setBackgroundColor(bg)
            }

        box.addView(
            text(
                "Выберите город",
                27f,
                true
            )
        )

        box.addView(space(25))

        // ====================================================
        // Поиск любого города
        // ====================================================

        box.addView(
            text(
                "Найти любой город",
                16f,
                true
            )
        )

        box.addView(space(6))

        box.addView(
            text(
                "Если города нет в списке ниже, найдите его по названию.",
                13f,
                false,
                muted
            )
        )

        box.addView(space(10))

        val citySearchInput =
            EditText(this).apply {
                hint = "Например: Ханой или Washington"
                isSingleLine = true
            }

        box.addView(citySearchInput)

        box.addView(space(8))

        val citySearchButton =
            Button(this).apply {
                text = "Найти"
                isAllCaps = false
            }

        box.addView(citySearchButton)

        box.addView(space(8))

        val citySearchStatus =
            text(
                "",
                13f,
                false,
                muted
            )

        box.addView(citySearchStatus)

        citySearchButton.setOnClickListener {

            val query =
                citySearchInput.text
                    .toString()
                    .trim()

            if (query.length < 2) {
                citySearchStatus.text =
                    "Введите название города."
                return@setOnClickListener
            }

            citySearchButton.isEnabled = false
            citySearchStatus.text = "Ищем город…"

            Thread {

                try {

                    val results =
                        CitySearch.search(query)

                    runOnUiThread {

                        citySearchButton.isEnabled = true

                        if (results.isEmpty()) {

                            citySearchStatus.text =
                                "Ничего не найдено. Попробуйте уточнить название."

                            return@runOnUiThread
                        }

                        citySearchStatus.text =
                            "Найдено вариантов: ${results.size}"

                        val labels =
                            results.map {
                                it.displayName()
                            }.toTypedArray()

                        androidx.appcompat.app.AlertDialog
                            .Builder(this)
                            .setTitle("Выберите город")
                            .setItems(labels) { _, which ->

                                val found =
                                    results[which]

                                // Если это один из наших проверенных
                                // встроенных городов, используем именно
                                // его — вместе с официальным источником.
                                val builtIn =
                                    Cities.list.firstOrNull {
                                        it.name.equals(
                                            found.name,
                                            ignoreCase = true
                                        ) &&
                                        it.country.equals(
                                            found.country,
                                            ignoreCase = true
                                        )
                                    }

                                if (builtIn != null) {

                                    Store.setCity(
                                        this,
                                        builtIn.name
                                    )

                                    citySearchStatus.text =
                                        "Выбран: ${builtIn.name}. " +
                                        "Источник: ${Sources.sourceName(builtIn.name)}"

                                } else {

                                    val custom =
                                        found.toCity()

                                    Store.setCustomCity(
                                        this,
                                        custom
                                    )

                                    citySearchStatus.text =
                                        "Выбран: ${found.displayName()}. " +
                                        "Расчёт по координатам · MWL"
                                }

                                citySearchButton.isEnabled =
                                    false

                                citySearchInput.isEnabled =
                                    false

                                val request =
                                    OneTimeWorkRequestBuilder<SyncWorker>()
                                        .build()

                                WorkManager
                                    .getInstance(this)
                                    .enqueue(request)

                                WorkManager
                                    .getInstance(this)
                                    .getWorkInfoByIdLiveData(
                                        request.id
                                    )
                                    .observe(this) { info ->

                                        if (
                                            info != null &&
                                            info.state.isFinished
                                        ) {

                                            if (
                                                info.state ==
                                                WorkInfo.State.SUCCEEDED
                                            ) {
                                                showMainScreen()
                                            } else {
                                                citySearchStatus.text =
                                                    Store.syncStatus(this)

                                                citySearchButton.isEnabled =
                                                    true

                                                citySearchInput.isEnabled =
                                                    true
                                            }
                                        }
                                    }
                            }
                            .setNegativeButton(
                                "Отмена",
                                null
                            )
                            .show()
                    }

                } catch (e: Exception) {

                    runOnUiThread {

                        citySearchButton.isEnabled = true

                        citySearchStatus.text =
                            "Не удалось выполнить поиск. Проверьте интернет."
                    }
                }

            }.start()
        }

        box.addView(space(28))

        box.addView(
            text(
                "Проверенные города",
                18f,
                true
            )
        )

        box.addView(space(14))

        box.addView(
            text(
                "Страна",
                14f,
                true
            )
        )

        box.addView(space(6))

        val countries =
            Cities.countries()

        val currentCity =
            Store.customCity(this)
                ?: Cities.find(
                    Store.city(this)
                )

        val countrySpinner =
            Spinner(this)

        countrySpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                countries
            )

        countrySpinner.setSelection(
            countries.indexOf(
                currentCity.country
            ).coerceAtLeast(0)
        )

        box.addView(countrySpinner)

        box.addView(space(20))

        box.addView(
            text(
                "Город",
                14f,
                true
            )
        )

        box.addView(space(6))

        val citySpinner =
            Spinner(this)

        box.addView(citySpinner)

        box.addView(space(20))

        val sourceInfo =
            text(
                "",
                13f,
                false,
                muted
            )

        box.addView(sourceInfo)

        box.addView(space(20))

        val save =
            Button(this).apply {
                text =
                    "Сохранить и обновить"

                isAllCaps =
                    false
            }

        box.addView(save)

        box.addView(space(12))

        val status =
            text(
                "",
                14f
            )

        box.addView(status)

        fun fillCities(
            country: String,
            selectCity: String? = null
        ) {

            val cities =
                Cities.forCountry(country)

            val names =
                cities.map {
                    it.name
                }

            citySpinner.adapter =
                ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )

            val index =
                names.indexOf(
                    selectCity
                ).takeIf {
                    it >= 0
                } ?: 0

            citySpinner.setSelection(
                index
            )

            val selected =
                cities[index]

            sourceInfo.text =
                "Источник: ${Sources.sourceName(selected.name)}"
        }

        fillCities(
            currentCity.country,
            currentCity.name
        )

        countrySpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    fillCities(
                        countries[position]
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }

        save.setOnClickListener {

            val country =
                countries[
                    countrySpinner.selectedItemPosition
                ]

            val cities =
                Cities.forCountry(country)

            val selected =
                cities[
                    citySpinner.selectedItemPosition
                ]

            Store.setCity(
                this,
                selected.name
            )

            status.text =
                "Загрузка расписания…"

            save.isEnabled =
                false

            val request =
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .build()

            WorkManager
                .getInstance(this)
                .enqueue(request)

            WorkManager
                .getInstance(this)
                .getWorkInfoByIdLiveData(
                    request.id
                )
                .observe(this) { info ->

                    if (
                        info != null &&
                        info.state.isFinished
                    ) {

                        if (
                            info.state ==
                            WorkInfo.State.SUCCEEDED
                        ) {
                            showMainScreen()
                        } else {
                            status.text =
                                Store.syncStatus(this)

                            save.isEnabled =
                                true
                        }
                    }
                }
        }

        setContentView(box)
    }

    override fun onBackPressed() {
        if (showingMainScreen) super.onBackPressed() else showMainScreen()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    data class PrayerTime(
        val name: String,
        val time: String
    )
}
