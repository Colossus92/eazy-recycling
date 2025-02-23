package nl.eazysoftware.eazyrecyclingservice

import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test

class TruckRepositoryTest @Autowired constructor(
    private val truckRepository: TruckRepository
) : BaseTest() {

    @Test
    fun cleanUp() {
        truckRepository.deleteAll()
    }

    @Test
    fun `should save and retrieve a Truck`() {
        val truck = Truck("N-678-VT", "Peugeot", "E-2008")

        val savedTruck = truckRepository.save(truck)
        val receivedTruck = truckRepository.findByLicensePlate(savedTruck.licensePlate)

        assertThat(receivedTruck).isNotNull
        assertThat(savedTruck).isEqualTo(receivedTruck)
    }
}