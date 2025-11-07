package nl.eazysoftware.eazyrecyclingservice.domain.model.waste

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import nl.eazysoftware.eazyrecyclingservice.config.clock.TimeConfiguration
import kotlin.time.Instant

enum class WasteStreamStatus {
  DRAFT,
  ACTIVE,
  INACTIVE,
}

enum class EffectiveStatus {
  DRAFT,
  ACTIVE,
  INACTIVE,
  EXPIRED,
}

object EffectiveStatusPolicy {
  fun compute(base: WasteStreamStatus, lastActivityAt: Instant, now: Instant): EffectiveStatus =
    if (now >= lastActivityAt.plus(5, DateTimeUnit.YEAR, TimeConfiguration.DOMAIN_TIMEZONE)) EffectiveStatus.EXPIRED
    else when (base) {
      WasteStreamStatus.DRAFT -> EffectiveStatus.DRAFT
      WasteStreamStatus.ACTIVE -> EffectiveStatus.ACTIVE
      WasteStreamStatus.INACTIVE -> EffectiveStatus.INACTIVE
    }

}
