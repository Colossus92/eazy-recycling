package nl.eazysoftware.eazyrecyclingservice.controller.transport

data class AddressRequest(
    val streetName: String,
    val buildingNumber: String,
    val postalCode: String,
    val city: String,
    val country: String = "Nederland"
)