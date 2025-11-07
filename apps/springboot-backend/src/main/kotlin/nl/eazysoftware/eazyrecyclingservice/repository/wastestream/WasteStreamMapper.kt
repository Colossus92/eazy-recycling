package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import org.hibernate.Hibernate
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class WasteStreamMapper(
  @PersistenceContext private var entityManager: EntityManager,
  private var pickupLocationMapper: PickupLocationMapper,
  private var companies: Companies,
  private val companyMapper: CompanyMapper,
) {

  fun toDomain(dto: WasteStreamDto): WasteStream {
    val processorPartyId = dto.processorParty.processorId
      ?.let { ProcessorPartyId(it) }
      ?: throw IllegalArgumentException("Verwerkersnummer is verplicht voor een geldig afvalstroomnummer, maar ${dto.processorParty.name} heeft geen vewerkersnummer")
    val actualPickupLocation = Hibernate.unproxy(dto.pickupLocation, PickupLocationDto::class.java)

    return WasteStream(
      wasteStreamNumber = WasteStreamNumber(dto.number),
      wasteType = WasteType(
        name = dto.name,
        euralCode = EuralCode(dto.euralCode.code),
        processingMethod = ProcessingMethod(dto.processingMethodCode.code)
      ),
      collectionType = WasteCollectionType.valueOf(dto.wasteCollectionType),
      pickupLocation = pickupLocationMapper.toDomain(actualPickupLocation),
      deliveryLocation = WasteDeliveryLocation(
        processorPartyId = processorPartyId
      ),
      consignorParty = Consignor.Company(
        CompanyId(dto.consignorParty.id)
      ),
      consignorClassification = ConsignorClassification.fromCode(dto.consignorClassification),
      pickupParty = CompanyId(dto.pickupParty.id),
      dealerParty = dto.dealerParty?.let { CompanyId(it.id) },
      collectorParty = dto.collectorParty?.let { CompanyId(it.id) },
      brokerParty = dto.brokerParty?.let { CompanyId(it.id) },
      lastActivityAt = dto.lastActivityAt.toKotlinInstant(),
      status = WasteStreamStatus.valueOf(dto.status)
    )
  }

  fun toDto(domain: WasteStream): WasteStreamDto {
    return WasteStreamDto(
      number = domain.wasteStreamNumber.number,
      name = domain.wasteType.name,
      euralCode = entityManager.getReference(Eural::class.java, domain.wasteType.euralCode.code),
      processingMethodCode = entityManager.getReference(
        ProcessingMethodDto::class.java,
        domain.wasteType.processingMethod.code
      ),
      wasteCollectionType = domain.collectionType.name,
      pickupLocation = pickupLocationMapper.toDto(domain.pickupLocation),
      processorParty = companies.findByProcessorId(domain.deliveryLocation.processorPartyId.number)
        ?.let { companyMapper.toDto(it) }
        ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: ${domain.deliveryLocation.processorPartyId.number}"),
      consignorParty = when (val consignor = domain.consignorParty) {
        is Consignor.Company -> entityManager.getReference(CompanyDto::class.java, consignor.id.uuid)
        is Consignor.Person -> throw IllegalArgumentException("Person consignor is not yet supported in persistence layer")
      },
      consignorClassification = domain.consignorClassification.code,
      pickupParty = entityManager.getReference(CompanyDto::class.java, domain.pickupParty.uuid),
      dealerParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      collectorParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      brokerParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      lastActivityAt = domain.lastActivityAt.toJavaInstant(),
      status = domain.status.name
    )
  }


}
