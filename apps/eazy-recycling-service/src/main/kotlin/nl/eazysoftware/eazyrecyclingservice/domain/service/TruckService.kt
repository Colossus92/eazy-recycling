package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class TruckService(
    val truckRepository: TruckRepository,
) {

    fun createTruck(truck: Truck) {
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
            .findByLicensePlate(licensePlate)
            ?: throw EntityNotFoundException("Truck with license plate $licensePlate not found")
    }

    fun deleteTruck(licensePlate: String) {
        truckRepository.deleteById(licensePlate)
    }

    fun updateTruck(licensePlate: String, truck: Truck): Truck {
        if (!licensePlate.equals(truck.licensePlate, ignoreCase = true)) {
            throw IllegalStateException("Cannot change truck license plate")
        }

       truckRepository.findByLicensePlate(licensePlate)
            ?: throw EntityNotFoundException("Truck with license plate $licensePlate not found")

        return truckRepository.save(truck)
    }
}