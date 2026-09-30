package com.islam9lam.namazwidget

object CitySelection {
    fun countryOptions(
        customCity: City?,
        verifiedCountries: List<String>
    ): List<String> =
        buildList {
            customCity?.country
                ?.takeIf { it.isNotBlank() }
                ?.let(::add)
            addAll(verifiedCountries)
        }.distinct()

    fun citiesForCountry(
        country: String,
        customCity: City?,
        verifiedCities: List<City>
    ): List<City> =
        buildList {
            customCity
                ?.takeIf { it.country == country }
                ?.let(::add)
            addAll(
                verifiedCities.filter {
                    it.country == country
                }
            )
        }.distinctBy { it.name }
}
