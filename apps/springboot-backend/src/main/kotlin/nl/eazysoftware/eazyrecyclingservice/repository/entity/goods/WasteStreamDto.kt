package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationDto

@Entity
@Table(name = "waste_streams")
data class WasteStreamDto(
  @Id
  @Column(name = "number")
  val number: String,

  @Column(name = "name", nullable = false)
  val name: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "eural_code", nullable = false)
  val euralCode: Eural,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processing_method_code", nullable = false)
  val processingMethodCode: ProcessingMethodDto,

  @Column(name = "waste_collection_type", nullable = false)
  val wasteCollectionType: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pickup_location_id", referencedColumnName = "id")
  val pickupLocation: PickupLocationDto,

  /**
   * Five digits indicating the processing party, which is the party to wich the goods are delivered.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processor_party_id", referencedColumnName = "processor_id", nullable = false)
  val processorParty: CompanyDto,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "consignor_party_id", referencedColumnName = "id", nullable = false)
  val consignorParty: CompanyDto,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pickup_party_id", referencedColumnName = "id", nullable = false)
  val pickupParty: CompanyDto,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dealer_party_id", referencedColumnName = "id")
  val dealerParty: CompanyDto?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "collector_party_id", referencedColumnName = "id")
  val collectorParty: CompanyDto?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "broker_party_id", referencedColumnName = "id")
  val brokerParty: CompanyDto?,

  )
