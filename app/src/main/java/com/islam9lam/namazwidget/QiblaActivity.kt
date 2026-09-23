package com.islam9lam.namazwidget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.roundToInt

class QiblaActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager

    private var rotationSensor: Sensor? = null

    private lateinit var arrow: TextView
    private lateinit var headingText: TextView
    private lateinit var statusText: TextView
    private lateinit var bearingText: TextView

    private var qiblaBearing: Int? = null
    private var currentRotation = 0f

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted) {
                findLocation()
            } else {
                qiblaBearing = null
                headingText.text = "Нужен доступ к местоположению"
                bearingText.text = ""
                statusText.text =
                    "Без текущих координат направление Кыблы определить нельзя."
            }
        }

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun label(
        value: String,
        size: Float,
        bold: Boolean = false
    ) = TextView(this).apply {

        text = value
        textSize = size

        setTextColor(
            Color.rgb(35, 35, 40)
        )

        if (bold) {
            setTypeface(
                typeface,
                Typeface.BOLD
            )
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        sensorManager =
            getSystemService(Context.SENSOR_SERVICE)
                    as SensorManager

        locationManager =
            getSystemService(Context.LOCATION_SERVICE)
                    as LocationManager

        rotationSensor =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ROTATION_VECTOR
            )

        buildScreen()

        requestLocationIfNeeded()
    }

    private fun buildScreen() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_HORIZONTAL

                setPadding(
                    dp(24),
                    dp(42),
                    dp(24),
                    dp(30)
                )

                setBackgroundColor(
                    Color.rgb(
                        248,
                        248,
                        250
                    )
                )
            }

        val top =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL
            }

        val back =
            Button(this).apply {

                text = "←"
                textSize = 20f
                isAllCaps = false

                setOnClickListener {
                    finish()
                }
            }

        top.addView(back)

        top.addView(
            label(
                "Кыбла",
                27f,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = dp(12)
            }
        )

        root.addView(
            top,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(space(32))

        bearingText =
            label(
                "Определяем ваше местоположение…",
                15f
            )

        bearingText.gravity =
            Gravity.CENTER

        root.addView(bearingText)

        root.addView(space(28))

        val compass =
            FrameLayout(this).apply {

                background =
                    android.graphics.drawable.GradientDrawable().apply {

                        shape =
                            android.graphics.drawable.GradientDrawable.OVAL

                        setColor(Color.WHITE)

                        setStroke(
                            dp(1),
                            Color.rgb(
                                220,
                                224,
                                222
                            )
                        )
                    }

                elevation =
                    dp(3).toFloat()
            }

        arrow =
            TextView(this).apply {

                text = "↑"
                textSize = 105f

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.rgb(
                        72,
                        96,
                        83
                    )
                )
            }

        compass.addView(
            arrow,
            FrameLayout.LayoutParams(
                dp(240),
                dp(240),
                Gravity.CENTER
            )
        )

        val north =
            TextView(this).apply {

                text = "С"
                textSize = 15f

                gravity =
                    Gravity.CENTER

                setTypeface(
                    typeface,
                    Typeface.BOLD
                )

                setTextColor(
                    Color.rgb(
                        100,
                        100,
                        105
                    )
                )
            }

        compass.addView(
            north,
            FrameLayout.LayoutParams(
                dp(40),
                dp(40),
                Gravity.TOP or
                    Gravity.CENTER_HORIZONTAL
            )
        )

        root.addView(
            compass,
            LinearLayout.LayoutParams(
                dp(270),
                dp(270)
            )
        )

        root.addView(space(28))

        headingText =
            label(
                "Подготовка компаса…",
                18f,
                true
            )

        headingText.gravity =
            Gravity.CENTER

        root.addView(headingText)

        root.addView(space(9))

        statusText =
            label(
                "",
                14f
            )

        statusText.gravity =
            Gravity.CENTER

        root.addView(statusText)

        if (rotationSensor == null) {

            headingText.text =
                "Датчик компаса недоступен"

            statusText.text =
                "На этом устройстве не удалось получить данные ориентации."
        }

        setContentView(root)
    }

    private fun space(h: Int) =
        Space(this).apply {

            layoutParams =
                LinearLayout.LayoutParams(
                    1,
                    dp(h)
                )
        }

    private fun requestLocationIfNeeded() {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fine || coarse) {

            findLocation()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun findLocation() {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            return
        }

        bearingText.text =
            "Определяем ваше местоположение…"

        try {

            val providers =
                locationManager.getProviders(true)

            val lastLocations =
                providers.mapNotNull { provider ->

                    try {
                        locationManager
                            .getLastKnownLocation(provider)
                    } catch (_: Exception) {
                        null
                    }
                }

            val best =
                lastLocations.maxByOrNull {
                    it.time
                }

            if (best != null) {

                useLocation(best)

            } else {

                requestFreshLocation()
            }

        } catch (_: Exception) {

            requestFreshLocation()
        }
    }

    private fun requestFreshLocation() {

        val fine =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarse =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fine && !coarse) {
            return
        }

        val provider =
            when {

                locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                ) ->
                    LocationManager.GPS_PROVIDER

                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                ) ->
                    LocationManager.NETWORK_PROVIDER

                else -> null
            }

        if (provider == null) {

            qiblaBearing = null

            headingText.text =
                "Включите геолокацию"

            bearingText.text = ""

            statusText.text =
                "Службы определения местоположения выключены."

            return
        }

        try {

            locationManager.getCurrentLocation(
                provider,
                null,
                mainExecutor
            ) { location ->

                if (location != null) {

                    useLocation(location)

                } else {

                    qiblaBearing = null

                    headingText.text =
                        "Местоположение не найдено"

                    bearingText.text = ""

                    statusText.text =
                        "Не удалось получить текущие координаты."
                }
            }

        } catch (_: SecurityException) {

            qiblaBearing = null

            headingText.text =
                "Нет доступа к местоположению"
        }
    }

    private fun useLocation(
        location: Location
    ) {

        qiblaBearing =
            Qibla.bearing(
                location.latitude,
                location.longitude
            )

        bearingText.text =
            "Направление Кыблы: ${qiblaBearing}°"

        headingText.text =
            "Кыбла определена"

        statusText.text =
            "Поворачивайте телефон — стрелка указывает направление."
    }

    override fun onResume() {
        super.onResume()

        rotationSensor?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()

        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(
        event: SensorEvent
    ) {

        if (
            event.sensor.type !=
            Sensor.TYPE_ROTATION_VECTOR
        ) return

        val bearing =
            qiblaBearing ?: return

        val rawMatrix =
            FloatArray(9)

        SensorManager
            .getRotationMatrixFromVector(
                rawMatrix,
                event.values
            )

        val rotation =
            display?.rotation
                ?: Surface.ROTATION_0

        val adjustedMatrix =
            FloatArray(9)

        when (rotation) {

            Surface.ROTATION_90 ->
                SensorManager.remapCoordinateSystem(
                    rawMatrix,
                    SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X,
                    adjustedMatrix
                )

            Surface.ROTATION_180 ->
                SensorManager.remapCoordinateSystem(
                    rawMatrix,
                    SensorManager.AXIS_MINUS_X,
                    SensorManager.AXIS_MINUS_Y,
                    adjustedMatrix
                )

            Surface.ROTATION_270 ->
                SensorManager.remapCoordinateSystem(
                    rawMatrix,
                    SensorManager.AXIS_MINUS_Y,
                    SensorManager.AXIS_X,
                    adjustedMatrix
                )

            else ->
                System.arraycopy(
                    rawMatrix,
                    0,
                    adjustedMatrix,
                    0,
                    9
                )
        }

        val orientation =
            FloatArray(3)

        SensorManager.getOrientation(
            adjustedMatrix,
            orientation
        )

        var azimuth =
            Math.toDegrees(
                orientation[0].toDouble()
            ).toFloat()

        azimuth =
            (azimuth + 360f) % 360f

        val target =
            (bearing - azimuth + 360f) % 360f

        var delta =
            target -
                (currentRotation % 360f)

        if (delta > 180f)
            delta -= 360f

        if (delta < -180f)
            delta += 360f

        // Небольшое сглаживание движения стрелки.
        currentRotation +=
            delta * 0.18f

        arrow.rotation =
            currentRotation

        val difference =
            abs(
                ((bearing - azimuth + 540f) % 360f) - 180f
            )

        headingText.text =
            if (difference <= 4f) {
                "Направление Кыблы ✓"
            } else {
                "Кыбла · $bearing°"
            }

        statusText.text =
            "Направление телефона: ${azimuth.roundToInt()}°"
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
    }
}
