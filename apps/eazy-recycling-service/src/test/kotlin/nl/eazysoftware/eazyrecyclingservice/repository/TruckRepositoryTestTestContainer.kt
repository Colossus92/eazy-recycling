package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.TestContainerBaseTest
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.assertj.core.api.Assertions
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test

class TruckRepositoryTestTestContainer @Autowired constructor(
    private val truckRepository: TruckRepository
) : TestContainerBaseTest() {

    @Test
    fun cleanUp() {
        truckRepository.deleteAll()
    }

    @Test
    fun `should save and retrieve a Truck`() {
        val truck = Truck("N-678-VT", "Peugeot", "E-2008")

        val savedTruck = truckRepository.save(truck)
        val receivedTruck = truckRepository.findByLicensePlate(savedTruck.licensePlate)

        Assertions.assertThat(receivedTruck).isNotNull
        Assertions.assertThat(savedTruck).isEqualTo(receivedTruck)
    }
}