package nl.eazysoftware.eazyrecyclingservice.controller.transport

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import java.time.LocalDateTime
import java.util.*

data class CreateWasteTransportRequest(
  val consigneePartyId: String,
  val pickupPartyId: String,
  override val consignorPartyId: UUID,
  override val pickupDateTime: LocalDateTime,
  override val deliveryDateTime: LocalDateTime?,
  override val containerOperation: ContainerOperation,
  override val transportType: TransportType,
  override val driverId: UUID?,
  override val carrierPartyId: UUID,
  override val pickupCompanyId: UUID?,
  override val pickupCompanyBranchId: UUID? = null,
  override val pickupStreet: String,
  override val pickupBuildingNumber: String,
  @field:Pattern(regexp = "^\\d{4}\\s[A-Z]{2}$", message = "Postcode moet bestaan uit 4 cijfers gevolgd door een spatie en 2 hoofdletters")
    override val pickupPostalCode: String,
  override val pickupCity: String,
  override val deliveryCompanyId: UUID?,
  override val deliveryCompanyBranchId: UUID? = null,
  override val deliveryStreet: String,
  override val deliveryBuildingNumber: String,
  @field:Pattern(regexp = "^\\d{4}\\s[A-Z]{2}$", message = "Postcode moet bestaan uit 4 cijfers gevolgd door een spatie en 2 hoofdletters")
    override val deliveryPostalCode: String,
  override val deliveryCity: String,
  override val truckId: String?,
  override val containerId: UUID?,
  override val note: String,
  val consignorClassification: Int,
  val wasteStreamNumber: String?,
  @Min(0)
    val weight: Int,
  @NotBlank
    val unit: String,
  @Min(0)
    val quantity: Int,
  @NotBlank
    val goodsName: String,
  @NotBlank
    val euralCode: String,
  @NotBlank
    val processingMethodCode: String,
): CreateContainerTransportRequest( // Waste transport is also a container transport
    consignorPartyId = consignorPartyId,
    pickupDateTime = pickupDateTime,
    deliveryDateTime = deliveryDateTime,
    containerOperation = containerOperation,
    transportType = transportType,
    driverId = driverId,
    carrierPartyId = carrierPartyId,
    pickupCompanyId = pickupCompanyId,
    pickupCompanyBranchId = pickupCompanyBranchId,
    pickupStreet = pickupStreet,
    pickupBuildingNumber = pickupBuildingNumber,
    pickupPostalCode = pickupPostalCode,
    pickupCity = pickupCity,
    deliveryCompanyId = deliveryCompanyId,
    deliveryCompanyBranchId = deliveryCompanyBranchId,
    deliveryStreet = deliveryStreet,
    deliveryBuildingNumber = deliveryBuildingNumber,
    deliveryPostalCode = deliveryPostalCode,
    deliveryCity = deliveryCity,
    truckId = truckId,
    containerId = containerId,
    note = note,
)
