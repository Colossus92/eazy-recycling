package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class TransportService(
  private val transportRepository: TransportRepository,
  private val pdfGenerationClient: PdfGenerationClient, //TODO generate pdf on transport creation
) {

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }

    fun deleteTransport(id: UUID) {
        transportRepository.deleteById(id)
    }

    fun getTransportById(id: UUID): TransportDto {
        return transportRepository.findByIdOrNull(id)
            ?: throw EntityNotFoundException("Transport with id $id not found")
    }

    fun markTransportAsFinished(id: UUID, transportHours: Double): TransportDto {
        return transportRepository.findByIdOrNull(id)
          ?.let {
            transportRepository.save(it.copy(transportHours = transportHours))
          }
          ?: throw EntityNotFoundException("Transport met id $id niet gevonden")
    }
}
