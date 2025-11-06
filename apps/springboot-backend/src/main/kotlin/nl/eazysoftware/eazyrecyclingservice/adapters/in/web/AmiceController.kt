package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.ValidateWasteStream
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.ValidateWasteStreamCommand
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ValidationRequestData
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamValidationResult
import org.hibernate.validator.constraints.Length
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for Amice integration endpoints.
 * When amice.enabled=false, validation requests will throw UnsupportedOperationException.
 */
@RestController
@PreAuthorize(HAS_ANY_ROLE)
class AmiceController(
  private val validateWasteStream: ValidateWasteStream
) {

  @PostMapping("/waste-streams/{wasteStreamNumber}/validate")
  fun validateWasteStreamNumber(
    @PathVariable
    @Length(min = 12, max = 12, message = "Afvalstroomnummer moet exact 12 tekens lang zijn")
    @Pattern(regexp = "^[0-9]{12}$", message = "Afvalstroomnummer moet 12 cijfers bevatten")
    wasteStreamNumber: String
  ): WasteStreamValidationResponse {
    val result = validateWasteStream.handle(ValidateWasteStreamCommand(WasteStreamNumber(wasteStreamNumber)))
    return WasteStreamValidationResponse.from(result)
  }
}


data class WasteStreamValidationResponse(
  val isValid: Boolean,
  val errors: List<ValidationErrorResponse>,
  val requestData: ValidationRequestDataResponse?
) {
  companion object {
    fun from(result: WasteStreamValidationResult): WasteStreamValidationResponse {
      return WasteStreamValidationResponse(
        isValid = result.isValid,
        errors = result.errors.map { ValidationErrorResponse(it.code, it.description) },
        requestData = result.requestData?.let { ValidationRequestDataResponse.from(it) }
      )
    }
  }
}

data class ValidationErrorResponse(
  val code: String,
  val description: String
)

data class ValidationRequestDataResponse(
  val wasteStreamNumber: String,
  val routeCollection: String?,
  val collectorsScheme: String?,
  val consignor: ConsignorDataResponse?,
  val pickupLocation: PickupLocationDataResponse?,
  val deliveryLocation: String?,
  val consignorParty: CompanyDataResponse?,
  val collectorParty: CompanyDataResponse?,
  val dealerParty: CompanyDataResponse?,
  val brokerParty: CompanyDataResponse?,
  val wasteCode: String?,
  val wasteName: String?,
  val processingMethod: String?
) {
  companion object {
    fun from(data: ValidationRequestData): ValidationRequestDataResponse {
      return ValidationRequestDataResponse(
        wasteStreamNumber = data.wasteStreamNumber,
        routeCollection = data.routeCollection,
        collectorsScheme = data.collectorsScheme,
        consignor = data.consignor?.let { ConsignorDataResponse.from(it) },
        pickupLocation = data.pickupLocation?.let { PickupLocationDataResponse.from(it) },
        deliveryLocation = data.deliveryLocation,
        consignorParty = data.consignorParty?.let { CompanyDataResponse.from(it) },
        collectorParty = data.collectorParty?.let { CompanyDataResponse.from(it) },
        dealerParty = data.dealerParty?.let { CompanyDataResponse.from(it) },
        brokerParty = data.brokerParty?.let { CompanyDataResponse.from(it) },
        wasteCode = data.wasteCode,
        wasteName = data.wasteName,
        processingMethod = data.processingMethod
      )
    }
  }
}
