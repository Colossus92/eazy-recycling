package nl.eazysoftware.eazyrecyclingservice.repository.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.WasteTransport
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository


@Repository
class WasteTransportRepository(
  private val jpaRepository: TransportRepository,
  private val wasteTransportMapper: WasteTransportMapper,
) : WasteTransports {
  override fun save(wasteTransport: WasteTransport) =
    wasteTransportMapper.toDto(wasteTransport)
      .let { jpaRepository.save(it) }
      .let { wasteTransportMapper.toDomain(it) }

  override fun findById(transportId: TransportId): WasteTransport? {
    val dto: TransportDto? = jpaRepository.findByIdOrNull(transportId.uuid)

    return if (dto?.goods?.isNotEmpty() == true) {
      wasteTransportMapper.toDomain(dto)
    } else {
      null
    }
  }

  override fun findAll() =
    jpaRepository.findAll()
      .filter { it.goods != null }
      .map { wasteTransportMapper.toDomain(it) }

  override fun delete(transportId: TransportId) {
    jpaRepository.deleteById(transportId.uuid)
  }

  override fun findByWeightTicketNumber(weightTicketNumber: Long): List<WasteTransport> {
    return jpaRepository.findByWeightTicketNumber(weightTicketNumber)
      .filter { it.goods != null }
      .map { wasteTransportMapper.toDomain(it) }
  }
}
