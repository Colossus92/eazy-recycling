package nl.eazysoftware.eazyrecyclingservice.controller.amice

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessieResponse
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.FirstReceivalDeclarator
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarator
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ReceivalDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.SessionResults
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Controller for testing Amice integration endpoints.
 * This is primarily for manual testing and debugging of Amice SOAP service calls.
 */
@RestController
@RequestMapping("/amice")
class AmiceController(
  private val firstReceivalDeclarator: FirstReceivalDeclarator,
  private val monthlyWasteDeclarator: MonthlyWasteDeclarator,
  private val sessionResults: SessionResults,
) {

  @PostMapping
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  fun monthlyReport() {
    monthlyWasteDeclarator.declare()
  }

  @GetMapping("/{sessionId}")
  fun requestSessionResult(@PathVariable sessionId: UUID): OpvragenResultaatVerwerkingMeldingSessieResponse {
    return sessionResults.retrieve(sessionId)
  }

  /**
   * Test endpoint for declaring first receivals to Amice.
   * This allows manual testing of the FirstReceivalDeclarator integration.
   */
  @PostMapping("/declare-first-receivals")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  fun declareFirstReceivals(
    @Valid @RequestBody request: DeclareFirstReceivalsRequest
  ): DeclareFirstReceivalsResponse {

    // Convert request to domain objects
    val receivalDeclarations = request.declarations.map { declaration ->

      // Build WasteStream
      val wasteStream = WasteStream(
        wasteStreamNumber = WasteStreamNumber(declaration.wasteStreamNumber),
        wasteType = WasteType(
          name = declaration.wasteType.name,
          euralCode = EuralCode(declaration.wasteType.euralCode),
          processingMethod = ProcessingMethod(declaration.wasteType.processingMethod)
        ),
        collectionType = WasteCollectionType.valueOf(declaration.collectionType),
        pickupLocation = mapPickupLocation(declaration.pickupLocation),
        deliveryLocation = WasteDeliveryLocation(
          processorPartyId = ProcessorPartyId(declaration.deliveryLocation.processorPartyId)
        ),
        consignorParty = when (declaration.consignorParty.type) {
          "COMPANY" -> Consignor.Company(CompanyId(UUID.fromString(declaration.consignorParty.companyId!!)))
          "PERSON" -> Consignor.Person
          else -> throw IllegalArgumentException("Invalid consignor type: ${declaration.consignorParty.type}")
        },
        consignorClassification = ConsignorClassification.fromCode(declaration.consignorClassification),
        pickupParty = CompanyId(UUID.fromString(declaration.pickupParty)),
        dealerParty = declaration.dealerParty?.let { CompanyId(UUID.fromString(it)) },
        collectorParty = declaration.collectorParty?.let { CompanyId(UUID.fromString(it)) },
        brokerParty = declaration.brokerParty?.let { CompanyId(UUID.fromString(it)) }
      )

      ReceivalDeclaration(
        wasteStream = wasteStream,
        transporters = declaration.transporterIds,
        totalWeight = declaration.totalWeight,
        totalShipments = declaration.totalShipments,
        yearMonth = YearMonth(declaration.year, declaration.month)
      )
    }

    // Call the declarator
    firstReceivalDeclarator.declareFirstReceivals(receivalDeclarations)

    return DeclareFirstReceivalsResponse(
      success = true,
      message = "Successfully declared ${receivalDeclarations.size} first receival(s) to Amice",
      declaredWasteStreamNumbers = receivalDeclarations.map { it.wasteStream.wasteStreamNumber.number }
    )
  }

  private fun mapPickupLocation(location: PickupLocationRequest?): Location {
    if (location == null) {
      return Location.NoLocation
    }

    return when (location.type) {
      "DUTCH_ADDRESS" -> {
        val address = nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address(
          streetName = nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName(location.streetName!!),
          buildingNumber = location.buildingNumber!!,
          buildingNumberAddition = location.buildingNumberAddition,
          postalCode = nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode(location.postalCode!!),
          city = nl.eazysoftware.eazyrecyclingservice.domain.model.address.City(location.city!!),
          country = "Nederland"
        )
        Location.DutchAddress(address)
      }
      "PROXIMITY_DESCRIPTION" -> Location.ProximityDescription(
        postalCodeDigits = location.postalCodeDigits!!,
        city = nl.eazysoftware.eazyrecyclingservice.domain.model.address.City(location.city!!),
        description = location.description!!,
        country = "Nederland"
      )
      "NO_LOCATION" -> Location.NoLocation
      else -> throw IllegalArgumentException("Invalid pickup location type: ${location.type}")
    }
  }

  // Request/Response DTOs
  data class DeclareFirstReceivalsRequest(
    @field:NotEmpty(message = "At least one declaration is required")
    val declarations: List<ReceivalDeclarationRequest>
  )

  data class ReceivalDeclarationRequest(
    @field:NotBlank(message = "Waste stream number is required")
    @field:Pattern(regexp = "^\\d{12}$", message = "Waste stream number must be exactly 12 digits")
    val wasteStreamNumber: String,

    @field:Valid
    val wasteType: WasteTypeRequest,

    @field:NotBlank(message = "Collection type is required")
    val collectionType: String, // DEFAULT, ROUTE, COLLECTORS_SCHEME

    val pickupLocation: PickupLocationRequest?,

    @field:Valid
    val deliveryLocation: DeliveryLocationRequest,

    @field:Valid
    val consignorParty: ConsignorPartyRequest,

    val consignorClassification: Int,

    @field:NotBlank(message = "Pickup party is required")
    val pickupParty: String, // UUID

    val dealerParty: String?, // UUID
    val collectorParty: String?, // UUID
    val brokerParty: String?, // UUID

    @field:NotEmpty(message = "At least one transporter is required")
    val transporterIds: List<String>, //VIHB number

    val totalWeight: Int,
    val totalShipments: Short,
    val year: Int,
    val month: Int
  )

  data class WasteTypeRequest(
    @field:NotBlank(message = "Waste type name is required")
    val name: String,

    @field:NotBlank(message = "Eural code is required")
    @field:Pattern(regexp = "^\\d{6}\\*?$", message = "Eural code must be 6 digits, optionally followed by *")
    val euralCode: String,

    @field:NotBlank(message = "Processing method is required")
    @field:Pattern(regexp = "^[A-Z]\\.?\\d{2}$", message = "Processing method must be in format A01 or A.01")
    val processingMethod: String
  )

  data class PickupLocationRequest(
    @field:NotBlank(message = "Location type is required")
    val type: String, // DUTCH_ADDRESS, PROXIMITY_DESCRIPTION, NO_LOCATION

    val streetName: String?,
    val buildingNumber: String?,
    val buildingNumberAddition: String?,
    val postalCode: String?,
    val city: String?,
    val postalCodeDigits: String?,
    val description: String?
  )

  data class DeliveryLocationRequest(
    @field:NotBlank(message = "Processor party ID is required")
    @field:Pattern(regexp = "^\\d{5}$", message = "Processor party ID must be exactly 5 digits")
    val processorPartyId: String
  )

  data class ConsignorPartyRequest(
    @field:NotBlank(message = "Consignor type is required")
    val type: String, // COMPANY or PERSON

    val companyId: String? // UUID, required if type is COMPANY
  )

  data class DeclareFirstReceivalsResponse(
    val success: Boolean,
    val message: String,
    val declaredWasteStreamNumbers: List<String>
  )
}
