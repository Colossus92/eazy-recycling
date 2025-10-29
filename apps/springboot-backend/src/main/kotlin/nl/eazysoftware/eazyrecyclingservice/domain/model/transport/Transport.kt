package nl.eazysoftware.eazyrecyclingservice.domain.model.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import kotlin.time.Duration

/**
 * Common interface for all transport types.
 */
interface Transport {
  val driver: UserId?
  val truck: LicensePlate?
  val transportHours: Duration?
}
