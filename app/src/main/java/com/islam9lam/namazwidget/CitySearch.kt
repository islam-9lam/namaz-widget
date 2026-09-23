package com.islam9lam.namazwidget

import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

data class CitySearchResult(
    val name: String,
    val country: String,
    val admin1: String?,
    val lat: Double,
    val lon: Double,
    val timeZone: String
) {
    fun displayName(): String =
        listOfNotNull(
            name,
            admin1?.takeIf { it.isNotBlank() },
            country.takeIf { it.isNotBlank() }
        ).distinct().joinToString(", ")

    fun toCity(): City =
        City(
            name = name,
            country = country,
            lat = lat,
            lon = lon,
            timeZone = timeZone,
            ummaSlug = null
        )
}

object CitySearch {

    fun search(query: String): List<CitySearchResult> {

        val clean = query.trim()

        if (clean.length < 2) {
            return emptyList()
        }

        val encoded =
            URLEncoder.encode(
                clean,
                Charsets.UTF_8.name()
            )

        val url =
            "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=$encoded" +
                "&count=10" +
                "&language=ru" +
                "&format=json"

        val body =
            Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(15000)
                .execute()
                .body()

        val root =
            JSONObject(body)

        val results =
            root.optJSONArray("results")
                ?: return emptyList()

        val out =
            mutableListOf<CitySearchResult>()

        for (i in 0 until results.length()) {

            val item =
                results.optJSONObject(i)
                    ?: continue

            val name =
                item.optString("name")
                    .trim()

            val country =
                item.optString("country")
                    .trim()

            val timeZone =
                item.optString("timezone")
                    .trim()

            if (
                name.isBlank() ||
                timeZone.isBlank() ||
                !item.has("latitude") ||
                !item.has("longitude")
            ) {
                continue
            }

            out +=
                CitySearchResult(
                    name = name,
                    country = country,
                    admin1 =
                        item.optString("admin1")
                            .trim()
                            .takeIf {
                                it.isNotBlank()
                            },
                    lat =
                        item.getDouble(
                            "latitude"
                        ),
                    lon =
                        item.getDouble(
                            "longitude"
                        ),
                    timeZone =
                        timeZone
                )
        }

        return out
    }
}
