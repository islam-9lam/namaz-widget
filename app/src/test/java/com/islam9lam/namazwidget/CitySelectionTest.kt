package com.islam9lam.namazwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CitySelectionTest {
    private val verifiedCountries = listOf("Россия", "Казахстан")
    private val verifiedCities = listOf(
        City("Москва", "Россия", 55.7558, 37.6173, "Europe/Moscow"),
        City("Алматы", "Казахстан", 43.2389, 76.8897, "Asia/Almaty")
    )

    @Test
    fun arbitrarySearchCityIsKeptAsFirstCountryAndCity() {
        val tashkent = City(
            "Ташкент",
            "Узбекистан",
            41.2995,
            69.2401,
            "Asia/Tashkent"
        )

        assertEquals(
            listOf("Узбекистан", "Россия", "Казахстан"),
            CitySelection.countryOptions(tashkent, verifiedCountries)
        )
        assertEquals(
            listOf(tashkent),
            CitySelection.citiesForCountry("Узбекистан", tashkent, verifiedCities)
        )
    }

    @Test
    fun customCityInVerifiedCountryAppearsAlongsideCatalogCities() {
        val sochi = City("Сочи", "Россия", 43.5855, 39.7231, "Europe/Moscow")

        assertEquals(
            listOf("Россия", "Казахстан"),
            CitySelection.countryOptions(sochi, verifiedCountries)
        )
        assertEquals(
            listOf("Сочи", "Москва"),
            CitySelection.citiesForCountry("Россия", sochi, verifiedCities).map { it.name }
        )
    }

    @Test
    fun catalogStillWorksWithoutCustomCity() {
        assertEquals(
            listOf("Россия", "Казахстан"),
            CitySelection.countryOptions(null, verifiedCountries)
        )
        assertEquals(
            listOf("Алматы"),
            CitySelection.citiesForCountry("Казахстан", null, verifiedCities).map { it.name }
        )
    }

    @Test
    fun everyCountryInVerifiedCatalogContainsAtLeastOneCity() {
        Cities.countries().forEach { country ->
            assertTrue(Cities.forCountry(country).isNotEmpty())
        }
    }
}
