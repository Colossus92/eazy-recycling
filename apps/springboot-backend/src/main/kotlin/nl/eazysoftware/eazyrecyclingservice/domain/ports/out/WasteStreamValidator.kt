package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream

/**
 * Domain port for validating waste stream data against external validation service.
 * Following hexagonal architecture - this is an outbound port.
 */
interface WasteStreamValidator {
  /**
   * Validates a waste stream against the external validation service.
   *
   * @param wasteStream The waste stream to validate
   * @return Validation result containing success status and any validation errors
   */
  fun validate(wasteStream: WasteStream): WasteStreamValidationResult
}

/**
 * Result of waste stream validation
 */
data class WasteStreamValidationResult(
  val wasteStreamNumber: String,
  val isValid: Boolean,
  val errors: List<ValidationError>,
  val requestData: ValidationRequestData?
) {
  companion object {
    fun valid(requestData: ValidationRequestData) = WasteStreamValidationResult(
      wasteStreamNumber = requestData.wasteStreamNumber,
      isValid = true,
      errors = emptyList(),
      requestData = requestData
    )

    fun invalid(wasteStreamNumber: String, errors: List<ValidationError>, requestData: ValidationRequestData?) = WasteStreamValidationResult(
      wasteStreamNumber = wasteStreamNumber,
      isValid = false,
      errors = errors,
      requestData = requestData
    )
  }
}

data class ValidationError(
  val code: String,
  val description: String
)

/**
 * Echo of the request data sent for validation
 */
data class ValidationRequestData(
  val wasteStreamNumber: String,
  val routeCollection: String?,
  val collectorsScheme: String?,
  val consignor: ConsignorData?,
  val pickupLocation: PickupLocationData?,
  val deliveryLocation: String?,
  val consignorParty: CompanyData?,
  val collectorParty: CompanyData?,
  val dealerParty: CompanyData?,
  val brokerParty: CompanyData?,
  val wasteCode: String?,
  val wasteName: String?,
  val processingMethod: String?
)

data class ConsignorData(
  val companyRegistrationNumber: String?,
  val name: String?,
  val country: String?,
  val isPrivate: Boolean
)

data class PickupLocationData(
  val postalCode: String?,
  val buildingNumber: String?,
  val buildingNumberAddition: String?,
  val city: String?,
  val streetName: String?,
  val proximityDescription: String?,
  val country: String?
)

data class CompanyData(
  val companyRegistrationNumber: String?,
  val name: String?,
  val country: String?
)
