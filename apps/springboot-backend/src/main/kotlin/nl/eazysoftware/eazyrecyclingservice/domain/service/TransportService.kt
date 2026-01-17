package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.repository.ProfileRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class TransportService(
  private val transportRepository: TransportRepository,
  private val profileRepository: ProfileRepository,
) {

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }

    fun deleteTransport(id: UUID) {
        transportRepository.deleteById(id)
    }

    fun getTransportById(id: UUID): TransportDto {
        return transportRepository.findByIdOrNull(id)
            ?: throw EntityNotFoundException("Transport met id $id niet gevonden")
    }

    fun markTransportAsFinished(id: UUID, transportHours: Double, driverNote: String): TransportDto {
        return transportRepository.findByIdOrNull(id)
          ?.let {
            transportRepository.save(
              it.copy(
                transportHours = transportHours,
                driverNote = driverNote,
              )
            )
          }
          ?: throw EntityNotFoundException("Transport met id $id niet gevonden")
    }

    fun updateTransportDriver(id: UUID, driverId: UUID?): TransportDto {
        val transport = transportRepository.findByIdOrNull(id)
            ?: throw EntityNotFoundException("Transport met id $id niet gevonden")
        
        val driver = driverId?.let { 
            profileRepository.findByIdOrNull(it)
                ?: throw EntityNotFoundException("Chauffeur met id $it niet gevonden")
        }
        
        return transportRepository.save(transport.copy(driver = driver))
    }
}
