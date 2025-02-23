package nl.eazysoftware.eazyrecyclingservice

import nl.eazysoftware.eazyrecyclingservice.repository.LocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.Test

class LocationRepositoryTest(
    @Autowired
    private val locationRepository: LocationRepository,
): BaseTest() {

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

        assertThat(retrievedLocation).isEqualTo(savedLocation)
    }
}