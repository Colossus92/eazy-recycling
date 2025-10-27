package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class TransportService(
  private val transportRepository: TransportRepository,
  private val companyBranchRepository: ProjectLocations,
  private val pdfGenerationClient: PdfGenerationClient, //TODO generate pdf on transport creation
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }

    /**
     * Validates that branch IDs belong to their respective company IDs
     *
     * @param pickupBranchId The ID of the pickup branch
     * @param pickupCompanyId The ID of the pickup company
     * @param deliveryBranchId The ID of the delivery branch
     * @param deliveryCompanyId The ID of the delivery company
     * @throws EntityNotFoundException If a branch is not found
     * @throws IllegalArgumentException If a branch doesn't belong to the specified company
     */
    private fun validateBranchCompanyRelationships(
        pickupBranchId: UUID?,
        pickupCompanyId: UUID?,
        deliveryBranchId: UUID?,
        deliveryCompanyId: UUID?
    ) {
        validateBranchCompanyRelationShip(pickupBranchId, pickupCompanyId)
        validateBranchCompanyRelationShip(deliveryBranchId, deliveryCompanyId)
    }

    private fun validateBranchCompanyRelationShip(companyBranchId: UUID?, companyId: UUID?) {
        if (companyBranchId != null && companyId != null) {
            val companyBranch = companyBranchRepository.findById(companyBranchId)
              ?: throw EntityNotFoundException("Vestiging met id $companyBranchId niet gevonden")

            if (companyBranch.companyId.uuid != companyId) {
                throw IllegalArgumentException("Vestiging met id $companyBranchId is niet van bedrijf met id $companyId")
            }
        }
    }

    fun deleteTransport(id: UUID) {
        transportRepository.deleteById(id)
    }

    fun getTransportById(id: UUID): TransportDto {
        return transportRepository.findByIdOrNull(id)
            ?: throw EntityNotFoundException("Transport with id $id not found")
    }


    fun markTransportAsFinished(id: UUID, transportHours: Double): TransportDto {
        return transportRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Transport met id $id niet gevonden") }
            .let {
                transportRepository.save(it.copy(transportHours = transportHours))
            }
    }
}
