package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamListView
import nl.eazysoftware.eazyrecyclingservice.application.usecase.CreateWasteStream
import nl.eazysoftware.eazyrecyclingservice.application.usecase.WasteStreamCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.DeleteWasteStream
import nl.eazysoftware.eazyrecyclingservice.application.usecase.DeleteWasteStreamCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.UpdateWasteStream
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteStreamService
import nl.eazysoftware.eazyrecyclingservice.domain.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.waste.PickupLocation.*
import org.hibernate.validator.constraints.Length
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/waste-streams")
@PreAuthorize(HAS_ANY_ROLE)
class WasteStreamController(
  private val wasteStreamService: WasteStreamService,
  private val createWasteStream: CreateWasteStream,
  private val updateWasteStream: UpdateWasteStream,
  private val deleteWasteStream: DeleteWasteStream,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@Valid @RequestBody request: WasteStreamRequest): CreateWasteStreamResponse {
    val result = createWasteStream.handle(request.toCommand())
    return CreateWasteStreamResponse(wasteStreamNumber = result.wasteStreamNumber.number)
  }

  @GetMapping
  fun getWasteStreams(): List<WasteStreamListView> {
    return wasteStreamService.getWasteStreams()
  }

  @GetMapping("/{wasteStreamNumber}")
  fun getWasteStreamByNumber(
    @PathVariable
    @Length(min = 12, max = 12, message = "Afvalstroomnummer moet exact 12 tekens lang zijn")
    @Pattern(regexp = "^[0-9]{12}$", message = "Afvalstroomnummer moet 12 cijfers bevatten")
    wasteStreamNumber: String
  ): WasteStreamDetailView {
    return wasteStreamService.getWasteStreamByNumber(WasteStreamNumber(wasteStreamNumber))
      ?: throw EntityNotFoundException("Afvalstroom met nummer $wasteStreamNumber niet gevonden")
  }

  @PutMapping("/{wasteStreamNumber}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun update(
    @PathVariable
    @Length(min = 12, max = 12, message = "Afvalstroomnummer moet exact 12 tekens lang zijn")
    @Pattern(regexp = "^[0-9]{12}$", message = "Afvalstroomnummer moet 12 cijfers bevatten")
    wasteStreamNumber: String,
    @Valid @RequestBody request: WasteStreamRequest
  ) {
    require(wasteStreamNumber == request.wasteStreamNumber) {
      "Afvalstroomnummer in path moet overeenkomen met afvalstroomnummer in body"
    }
    updateWasteStream.handle(request.toCommand())
  }

  @DeleteMapping("/{wasteStreamNumber}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun delete(
    @PathVariable
    @Length(min = 12, max = 12, message = "Afvalstroomnummer moet exact 12 tekens lang zijn")
    @Pattern(regexp = "^[0-9]{12}$", message = "Afvalstroomnummer moet 12 cijfers bevatten")
    wasteStreamNumber: String
  ) {
    deleteWasteStream.handle(DeleteWasteStreamCommand(WasteStreamNumber(wasteStreamNumber)))
  }
}

data class WasteStreamRequest(
  @field:Length(min = 12, max = 12, message = "Afvalstroomnummer moet exact 12 tekens lang zijn")
  @field:Pattern(regexp = "^[0-9]{12}$", message = "Afvalstroomnummer 12 cijfers bevatten")
  val wasteStreamNumber: String,

  @field:NotBlank(message = "Naam is verplicht")
  val name: String,

  @field:NotBlank(message = "Eural code is verplicht")
  val euralCode: String,

  @field:NotBlank(message = "Verwerkingsmethode code is verplicht")
  val processingMethodCode: String,

  val collectionType: String = "DEFAULT",

  @field:Valid
  val pickupLocation: PickupLocationRequest,

  @field:Length(min = 5, max = 5, message = "Processor party ID moet exact 5 cijfers lang zijn")
  @field:Pattern(regexp = "^[0-9]{5}$", message = "Processor party ID mag alleen cijfers bevatten")
  val processorPartyId: String,

  val consignorParty: ConsignorRequest,

  val pickupParty: UUID,

  val dealerParty: UUID? = null,

  val collectorParty: UUID? = null,

  val brokerParty: UUID? = null
) {
  fun toCommand(): WasteStreamCommand {
    return WasteStreamCommand(
      wasteStreamNumber = WasteStreamNumber(wasteStreamNumber),
      wasteType = WasteType(
        name = name,
        euralCode = EuralCode(euralCode),
        processingMethod = ProcessingMethod(processingMethodCode)
      ),
      collectionType = WasteCollectionType.valueOf(collectionType.uppercase()),
      pickupLocation = pickupLocation.toDomain(),
      deliveryLocation = DeliveryLocation(
        processorPartyId = ProcessorPartyId(processorPartyId)
      ),
      consignorParty = consignorParty.toDomain(),
      pickupParty = CompanyId(pickupParty),
      dealerParty = dealerParty?.let { CompanyId(it) },
      collectorParty = collectorParty?.let { CompanyId(it) },
      brokerParty = brokerParty?.let { CompanyId(it) }
    )
  }
}

data class CreateWasteStreamResponse(
  val wasteStreamNumber: String
)

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(
  JsonSubTypes.Type(value = ConsignorRequest.CompanyConsignor::class, name = "company"),
  JsonSubTypes.Type(value = ConsignorRequest.PersonConsignor::class, name = "person")
)
sealed class ConsignorRequest {
  abstract fun toDomain(): Consignor

  data class CompanyConsignor(val companyId: UUID) : ConsignorRequest() {
    override fun toDomain() = Consignor.Company(CompanyId(companyId))
  }

  class PersonConsignor() : ConsignorRequest() {
    override fun toDomain() = Consignor.Person
  }
}

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type"
)
@JsonSubTypes(
  JsonSubTypes.Type(value = PickupLocationRequest.DutchAddressRequest::class, name = "dutch_address"),
  JsonSubTypes.Type(value = PickupLocationRequest.ProximityDescriptionRequest::class, name = "proximity"),
  JsonSubTypes.Type(value = PickupLocationRequest.NoPickupLocationRequest::class, name = "none")
)
sealed class PickupLocationRequest {
  abstract fun toDomain(): PickupLocation

  data class DutchAddressRequest(

    @field:NotBlank(message = "Straatnaam is verplicht")
    val streetName: String,

    @field:NotBlank(message = "Huisnummer is verplicht")
    val buildingNumber: String,

    val buildingNumberAddition: String? = null,

    @field:NotBlank(message = "Postcode is verplicht")
    @field:Pattern(regexp = "^[1-9][0-9]{3}\\s?[A-Za-z]{2}$", message = "Postcode moet het formaat 1234AB of 1234 AB hebben")
    val postalCode: String,

    @field:NotBlank(message = "Land is verplicht")
    val country: String
  ) : PickupLocationRequest() {
    override fun toDomain() = DutchAddress(
      streetName = streetName,
      postalCode = DutchPostalCode(postalCode),
      buildingNumber = buildingNumber,
      buildingNumberAddition = buildingNumberAddition,
      country = country
    )
  }

  data class ProximityDescriptionRequest(
    @field:NotBlank(message = "Omschrijving is verplicht")
    val description: String,

    @field:NotBlank(message = "Postcode cijfers zijn verplicht")
    @field:Pattern(regexp = "^[0-9]{4}$", message = "Postcode cijfers moeten 4 cijfers zijn")
    val postalCodeDigits: String,

    @field:NotBlank(message = "Stad is verplicht")
    val city: String,

    @field:NotBlank(message = "Land is verplicht")
    val country: String
  ) : PickupLocationRequest() {
    override fun toDomain() = ProximityDescription(
      description = description,
      postalCodeDigits = postalCodeDigits,
      city = city,
      country = country
    )
  }

  class NoPickupLocationRequest : PickupLocationRequest() {
    override fun toDomain() = NoPickupLocation
  }
}
