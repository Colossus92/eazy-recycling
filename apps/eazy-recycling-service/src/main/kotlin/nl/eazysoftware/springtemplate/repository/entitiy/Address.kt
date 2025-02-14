package nl.eazysoftware.springtemplate.repository.entitiy

data class Address(
    val streetName: String?,
    val buildingName: String?,
    val buildingNumber: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?
)