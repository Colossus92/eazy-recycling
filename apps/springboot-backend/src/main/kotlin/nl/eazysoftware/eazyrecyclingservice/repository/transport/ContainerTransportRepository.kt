package nl.eazysoftware.eazyrecyclingservice.repository.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerTransport
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ContainerTransportRepository(
  private val jpaRepository: TransportRepository,
  private val mapper: ContainerTransportMapper
) : ContainerTransports {

  override fun save(containerTransport: ContainerTransport): ContainerTransport {
    val savedDto = mapper.toDto(containerTransport)
      .let { jpaRepository.save(it) }
    return mapper.toDomain(savedDto)
  }

  override fun findById(transportId: TransportId): ContainerTransport? {
    val dto = jpaRepository.findByIdOrNull(transportId.uuid) ?: return null
    // Only return if it's a container transport (no goods)
    return if (dto.goodsItem == null) {
      mapper.toDomain(dto)
    } else {
      null
    }
  }

  override fun findAll(): List<ContainerTransport> {
    return jpaRepository.findAll()
      .filter { it.goodsItem == null } // Only container transports
      .map { mapper.toDomain(it) }
  }

  override fun delete(transportId: TransportId) {
    jpaRepository.deleteById(transportId.uuid)
  }
}
