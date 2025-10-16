package nl.eazysoftware.eazyrecyclingservice.domain.weightticket

import kotlinx.datetime.Clock
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Consignor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*
import kotlin.test.assertFailsWith

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
      weightTicket.delete()
    }

    assertThat(weightTicket.status).isEqualTo(WeightTicketStatus.CANCELLED)
  }

  @Test
  fun `cannot delete weight ticket that is already cancelled`() {
    val weightTicket = weightTicket().apply {
      status = WeightTicketStatus.CANCELLED
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.delete()
    }

    assertThat(exception).hasMessageContaining("Weegbon is al geannuleerd en kan niet opnieuw worden geannuleerd")
  }

  @Test
  fun `cannot delete weight ticket that is invoiced`() {
    val weightTicket = weightTicket().apply {
      status = WeightTicketStatus.INVOICED
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.delete()
    }

    assertThat(exception).hasMessageContaining("Weegbon is al gefactureerd en kan niet worden geannuleerd")
  }

  @Test
  fun `cannot delete weight ticket that is completed`() {
    val weightTicket = weightTicket().apply {
      status = WeightTicketStatus.COMPLETED
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.delete()
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
      weightTicket.delete()
    }
  }

  private fun companyId(): CompanyId = CompanyId(UUID.randomUUID())
}
