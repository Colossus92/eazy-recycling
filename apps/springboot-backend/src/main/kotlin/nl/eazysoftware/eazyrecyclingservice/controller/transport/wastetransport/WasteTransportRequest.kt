package nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.TimingConstraintRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import java.util.*
import kotlin.reflect.KClass

@ValidWasteTransportTiming
data class WasteTransportRequest(
  @field:Valid
  val pickup: TimingConstraintRequest? = null,
  @field:Valid
  val delivery: TimingConstraintRequest? = null,
  val containerOperation: ContainerOperation,
  val transportType: TransportType,
  val driverId: UUID?,
  val carrierPartyId: UUID,
  val truckId: String?,
  val containerId: String?,
  val note: String,
  val goods: List<GoodsRequest>,
)

/**
 * Custom constraint annotation to validate that at least one timing constraint is provided.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [WasteTransportTimingValidator::class])
annotation class ValidWasteTransportTiming(
  val message: String = "Ophaal- of aflevertijd is verplicht",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)

/**
 * Validator to ensure at least one of pickup or delivery timing is provided.
 */
class WasteTransportTimingValidator : ConstraintValidator<ValidWasteTransportTiming, WasteTransportRequest> {
  override fun isValid(value: WasteTransportRequest?, context: ConstraintValidatorContext): Boolean {
    if (value == null) return true
    
    if (value.pickup == null && value.delivery == null) {
      context.disableDefaultConstraintViolation()
      context.buildConstraintViolationWithTemplate("Ophaal- of aflevertijd is verplicht")
        .addConstraintViolation()
      return false
    }
    return true
  }
}

data class GoodsRequest(
  val wasteStreamNumber: String,
  @field:Min(0)
  val weight: Double?,
  @field:NotBlank
  val unit: String,
  @field:Min(0)
  val quantity: Int,
)
