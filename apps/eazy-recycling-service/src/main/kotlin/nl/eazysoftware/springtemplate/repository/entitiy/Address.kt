package nl.eazysoftware.springtemplate.repository.entitiy

data class Address(
    val streetName: String,
    val BuildingName: String,
    val buildingNumber: String,
    val cityName: String,
    val postalZone: String,
    val country: String
)