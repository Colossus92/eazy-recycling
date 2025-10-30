package nl.eazysoftware.eazyrecyclingservice.domain.weightticket

import kotlinx.datetime.Clock
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.CancellationReason
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketLine
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketLines
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import java.math.BigDecimal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*
import kotlin.test.assertFailsWith

private val REASON = CancellationReason("reason")
private val EMPTY_LINES = WeightTicketLines(emptyList())
private val SAMPLE_LINES = WeightTicketLines(
  listOf(
    WeightTicketLine(
      waste = WasteStreamNumber("123456789012"),
      weight = Weight(BigDecimal("100.50"), Weight.WeightUnit.KILOGRAM)
    )
  )
)

class WeightTicketTest {

  @Test
  fun `can create weight ticket with all fields`() {
    assertDoesNotThrow {
      weightTicket()
    }
  }

  private fun weightTicket(status: WeightTicketStatus = WeightTicketStatus.DRAFT): WeightTicket = WeightTicket(
    id = WeightTicketId(1),
    consignorParty = Consignor.Company(companyId()),
    status = status,
    lines = EMPTY_LINES,
    carrierParty = companyId(),
    truckLicensePlate = LicensePlate("AB-123-CD"),
    reclamation = "Reclamation",
    note = Note("note"),
    createdAt = Clock.System.now(),
  )

  @Test
  fun `weight ticket should be updated when in draft status`() {
    val weightTicket = weightTicket()

    weightTicket.update(
      reclamation = "New reclamation",
    )

    assertThat(weightTicket.reclamation).isEqualTo("New reclamation")
  }

  @Test
  fun `weight ticket can be updated with lines`() {
    val weightTicket = weightTicket()

    weightTicket.update(
      lines = SAMPLE_LINES,
      reclamation = "New reclamation",
    )

    assertThat(weightTicket.lines).isEqualTo(SAMPLE_LINES)
    assertThat(weightTicket.lines.isEmpty()).isFalse()
    assertThat(weightTicket.reclamation).isEqualTo("New reclamation")
  }

  @Test
  fun `weight ticket can be created with lines`() {
    val weightTicket = WeightTicket(
      id = WeightTicketId(1),
      consignorParty = Consignor.Company(companyId()),
      status = WeightTicketStatus.DRAFT,
      lines = SAMPLE_LINES,
      carrierParty = companyId(),
      truckLicensePlate = LicensePlate("AB-123-CD"),
      reclamation = "Reclamation",
      note = Note("note"),
      createdAt = Clock.System.now(),
    )

    assertThat(weightTicket.lines).isEqualTo(SAMPLE_LINES)
    assertThat(weightTicket.lines.getLines()).hasSize(1)
    assertThat(weightTicket.lines.getLines().first().waste.number).isEqualTo("123456789012")
    assertThat(weightTicket.lines.getLines().first().weight.value).isEqualByComparingTo(BigDecimal("100.50"))
  }

  @ParameterizedTest
  @EnumSource(value = WeightTicketStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["DRAFT"])
  fun `a weight ticket that does not have status draft cannot be updated`(status: WeightTicketStatus) {
    val weightTicket = weightTicket(status)

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.update(
        reclamation = "New reclamation",
      )
    }

    assertThat(exception).hasMessageContaining("Weegbon kan alleen worden gewijzigd als de status concept is.")
  }

  @Test
  fun `can delete weight ticket with draft status`() {
    val weightTicket = weightTicket()

    assertDoesNotThrow {
      weightTicket.cancel(REASON)
    }

    assertThat(weightTicket.status).isEqualTo(WeightTicketStatus.CANCELLED)
    assertThat(weightTicket.cancellationReason).isEqualTo(REASON)
    assertThat(weightTicket.updatedAt).isNotNull
  }

  @Test
  fun `cannot delete weight ticket that is already cancelled`() {
    val weightTicket = weightTicket().apply {
      status = WeightTicketStatus.CANCELLED
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.cancel(REASON)
    }

    assertThat(exception).hasMessageContaining("Weegbon is al geannuleerd en kan niet opnieuw worden geannuleerd")
  }

  @Test
  fun `cannot delete weight ticket that is invoiced`() {
    val weightTicket = weightTicket().apply {
      status = WeightTicketStatus.INVOICED
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.cancel(REASON)
    }

    assertThat(exception).hasMessageContaining("Weegbon is al gefactureerd en kan niet worden geannuleerd")
  }

  @Test
  fun `cannot delete weight ticket that is completed`() {
    val weightTicket = weightTicket().apply {
      status = WeightTicketStatus.COMPLETED
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.cancel(REASON)
    }

    assertThat(exception).hasMessageContaining("Weegbon is al verwerkt en kan niet worden geannuleerd")
  }

  @ParameterizedTest
  @EnumSource(value = WeightTicketStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["DRAFT"])
  fun `cannot delete weight ticket with non-draft status`(status: WeightTicketStatus) {
    val weightTicket = weightTicket().apply {
      this.status = status
    }

    assertFailsWith<IllegalStateException> {
      weightTicket.cancel(REASON)
    }
  }

  @Test
  fun `can complete weight ticket with draft status and lines`() {
    val weightTicket = weightTicket().apply {
      lines = SAMPLE_LINES
    }

    assertDoesNotThrow {
      weightTicket.complete()
    }

    assertThat(weightTicket.status).isEqualTo(WeightTicketStatus.COMPLETED)
    assertThat(weightTicket.updatedAt).isNotNull
  }

  @Test
  fun `cannot complete weight ticket without lines`() {
    val weightTicket = weightTicket().apply {
      lines = EMPTY_LINES
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.complete()
    }

    assertThat(exception).hasMessageContaining("Weegbon kan alleen worden voltooid als er minimaal één sorteerweging is")
  }

  @ParameterizedTest
  @EnumSource(value = WeightTicketStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["DRAFT"])
  fun `cannot complete weight ticket with non-draft status`(status: WeightTicketStatus) {
    val weightTicket = weightTicket(status).apply {
      lines = SAMPLE_LINES
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.complete()
    }

    assertThat(exception).hasMessageContaining("Weegbon kan alleen worden voltooid als de status openstaand is")
  }

  private fun companyId(): CompanyId = CompanyId(UUID.randomUUID())
}
