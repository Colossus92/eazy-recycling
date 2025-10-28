package nl.eazysoftware.eazyrecyclingservice.config.clock

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.time.LocalDateTime
import java.time.ZoneId

fun LocalDateTime.toCetKotlinInstant(): Instant {
  return this.atZone(ZoneId.of("Europe/Amsterdam")).toInstant().toKotlinInstant()
}
