package nl.eazysoftware.eazyrecyclingservice.config.clock

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

fun LocalDateTime.toCetKotlinInstant(): Instant {
  return this.atZone(ZoneId.of("Europe/Amsterdam")).toInstant().toKotlinInstant()
}

fun LocalDateTime.toCetInstant(): java.time.Instant {
  return this.atZone(ZoneId.of("Europe/Amsterdam")).toInstant()
}

fun LocalDate.toCetInstant(): Instant {
  return this.atStartOfDay(ZoneId.of("Europe/Amsterdam")).toInstant().toKotlinInstant()
}
