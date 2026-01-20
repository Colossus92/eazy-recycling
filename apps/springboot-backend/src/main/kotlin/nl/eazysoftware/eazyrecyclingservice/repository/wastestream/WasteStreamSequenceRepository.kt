package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreamSequences
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class WasteStreamSequenceRepository(
  private val jdbcTemplate: JdbcTemplate
) : WasteStreamSequences {

  override fun nextValue(processorId: ProcessorPartyId): Long {
    return jdbcTemplate.queryForObject(
      "SELECT public.next_waste_stream_sequence(?)",
      Long::class.java,
      processorId.number
    ) ?: throw IllegalStateException("Failed to get next sequence value for processor ${processorId.number}")
  }
}
