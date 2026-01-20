package nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.Valid
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.PickupLocationRequest
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.TimingConstraintRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import java.util.*
import kotlin.reflect.KClass

@ValidContainerTransportTiming
class ContainerTransportRequest(
  val consignorPartyId: UUID,
  @field:Valid
  val pickup: TimingConstraintRequest? = null,
  @field:Valid
  val delivery: TimingConstraintRequest? = null,
  val transportType: TransportType,
  val containerOperation: ContainerOperation,
  val driverId: UUID?,
  val carrierPartyId: UUID,
  val pickupLocation: PickupLocationRequest,
  val deliveryLocation: PickupLocationRequest,
  val truckId: String?,
  val containerId: String?,
  val note: String,
)

/**
 * Custom constraint annotation to validate that at least one timing constraint is provided.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ContainerTransportTimingValidator::class])
annotation class ValidContainerTransportTiming(
  val message: String = "Ophaal- of aflevertijd is verplicht",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = []
)

/**
 * Validator to ensure at least one of pickup or delivery timing is provided.
 */
class ContainerTransportTimingValidator : ConstraintValidator<ValidContainerTransportTiming, ContainerTransportRequest> {
  override fun isValid(value: ContainerTransportRequest?, context: ConstraintValidatorContext): Boolean {
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
