package nl.eazysoftware.eazyrecyclingservice.domain.model.invoice

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId

data class CustomerSnapshot(
    val companyId: CompanyId,
    val customerNumber: String?,
    val name: String,
    val address: AddressSnapshot,
    val vatNumber: String?,
)

data class AddressSnapshot(
    val streetName: String,
    val buildingNumber: String?,
    val buildingNumberAddition: String?,
    val postalCode: String,
    val city: String,
    val country: String?,
)
