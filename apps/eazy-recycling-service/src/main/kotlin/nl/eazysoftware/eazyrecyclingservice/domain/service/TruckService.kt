package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class TruckService(
    val truckRepository: TruckRepository,
) {

    fun createTruck(truck: Truck) {
        val truck = truck.copy(
            licensePlate = truck.licensePlate.uppercase(),
        )
        if (truckRepository.existsById(truck.licensePlate)) {
            throw DuplicateKeyException("Vrachtwagen met kenteken ${truck.licensePlate} bestaat al")
        }

        truckRepository.save(truck)
    }

    fun getAllTrucks(): List<Truck> {
        return truckRepository.findAll()
    }

    fun getTruckByLicensePlate(licensePlate: String): Truck {
        return truckRepository
            .findByIdOrNull(licensePlate)
            ?: throw EntityNotFoundException("Vrachtwagen met kenteken $licensePlate niet gevonden")
    }

    fun deleteTruck(licensePlate: String) {
        truckRepository.deleteById(licensePlate)
    }

    fun updateTruck(licensePlate: String, truck: Truck): Truck {
        if (!licensePlate.equals(truck.licensePlate, ignoreCase = true)) {
            throw IllegalArgumentException("Vrachtwagen komt niet overeen met kenteken $licensePlate")
        }

       truckRepository.findByIdOrNull(licensePlate)
            ?: throw EntityNotFoundException("Vrachtwagen met kenteken $licensePlate niet gevonden")

        return truckRepository.save(truck)
    }
}