package nl.eazysoftware.eazyrecyclingservice.test.helpers

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import java.time.LocalDateTime
import java.util.*

object TransportDtoHelper {
    fun transport(
        truck: Truck?,
        pickupDateTime: LocalDateTime = LocalDateTime.of(2025, 5, 20, 10, 0),
        driver: ProfileDto?  = driver1,
        ): TransportDto =
        TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupLocation = pickupLocation,
            pickupDateTime = pickupDateTime,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = LocalDateTime.of(2025, 5, 20, 14, 0),
            transportType = TransportType.WASTE,
            truck = truck,
            driver = driver,
            note = "Test transport 1",
            sequenceNumber = 1
        )

    val driver1 = ProfileDto(
        id = UUID.randomUUID(),
        firstName = "John",
        lastName = "Doe",
    )

    val driver2 = ProfileDto(
        id = UUID.randomUUID(),
        firstName = "Jane",
        lastName = "Smith",
    )

    val company = CompanyDto(
        id = UUID.randomUUID(),
        name = "Test Company",
      address = AddressDto(
        streetName = "Main Street",
        buildingNumber = "10",
        postalCode = "1234 AB",
        city = "Amsterdam",
        country = "Netherlands"
      )
    )

    val pickupLocation = PickupLocationDto.PickupProjectLocationDto(
        id = UUID.randomUUID().toString(),
        company = company,
        streetName = "Main Street",
        buildingNumber = "10",
        buildingNumberAddition = null,
        postalCode = "1234 AB",
        city = "Amsterdam",
        country = "Netherlands"
    )

    val deliveryLocation = PickupLocationDto.PickupProjectLocationDto(
        id = UUID.randomUUID().toString(),
        company = company,
        streetName = "Second Street",
        buildingNumberAddition = null,
        buildingNumber = "20",
        postalCode = "5678 CD",
        city = "Rotterdam",
        country = "Netherlands"
    )
}
