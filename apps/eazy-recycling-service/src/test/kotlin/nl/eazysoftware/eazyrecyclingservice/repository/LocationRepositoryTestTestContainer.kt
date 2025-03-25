package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.TestContainerBaseTest
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test

class LocationRepositoryTestTestContainer(
    @Autowired
    private val locationRepository: LocationRepository,
): TestContainerBaseTest() {

    @AfterTest
    fun cleanUp() {
        locationRepository.deleteAll()
    }

    @Test
    fun `Location can be stored and retrieved by postalcode and building number`() {
        val location = LocationDto(
            id = UUID.randomUUID().toString(),
            description = "Eazy Software",
            locationTypeCode = "1",
            address = AddressDto(
                streetName = "Abe Bonnemastraat 58",
                buildingNumber = "58",
                city = "Bergschenhoek",
                postalCode = "2662EJ",
                country = "Nederland",
            )
        )

        val savedLocation = locationRepository.save(location)

        val retrievedLocation = locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(location.address.postalCode!!, location.address.buildingNumber!!)

        Assertions.assertThat(retrievedLocation).isEqualTo(savedLocation)
    }
}