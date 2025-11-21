package nl.eazysoftware.eazyrecyclingservice.domain.weightticket

import nl.eazysoftware.eazyrecyclingservice.config.clock.toCetInstant
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestLocationFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*
import kotlin.test.assertFailsWith
import kotlin.time.Clock

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
    direction = WeightTicketDirection.INBOUND,
    pickupLocation = TestLocationFactory.createDutchAddress(),
    deliveryLocation = TestLocationFactory.createCompanyAddress(),
    lines = EMPTY_LINES,
    carrierParty = companyId(),
    truckLicensePlate = LicensePlate("AB-123-CD"),
    reclamation = "Reclamation",
    note = Note("note"),
    createdAt = Clock.System.now(),
    tarraWeight = null,
    secondWeighing = null,
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
      direction = WeightTicketDirection.INBOUND,
      pickupLocation = TestLocationFactory.createDutchAddress(),
      deliveryLocation = TestLocationFactory.createCompanyAddress(),
      truckLicensePlate = LicensePlate("AB-123-CD"),
      reclamation = "Reclamation",
      note = Note("note"),
      tarraWeight = Weight(BigDecimal("50.00"), Weight.WeightUnit.KILOGRAM),
      secondWeighing = Weight(BigDecimal("10.00"), Weight.WeightUnit.KILOGRAM),
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
      weightedAt = LocalDate.of(2025, 11, 21).toCetInstant()
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
      weightedAt = LocalDate.of(2025, 11, 21).toCetInstant()
    }

    val exception = assertFailsWith<IllegalStateException> {
      weightTicket.complete()
    }

    assertThat(exception).hasMessageContaining("Weegbon kan alleen worden voltooid als er minimaal één sorteerregel is")
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

  @Test
  fun `can copy weight ticket with all fields`() {
    val originalTicket = WeightTicket(
      id = WeightTicketId(1),
      consignorParty = Consignor.Company(companyId()),
      status = WeightTicketStatus.COMPLETED,
      lines = SAMPLE_LINES,
      carrierParty = companyId(),
      direction = WeightTicketDirection.INBOUND,
      pickupLocation = TestLocationFactory.createDutchAddress(),
      deliveryLocation = TestLocationFactory.createCompanyAddress(),
      truckLicensePlate = LicensePlate("AB-123-CD"),
      reclamation = "Reclamation",
      note = Note("note"),
      tarraWeight = Weight(BigDecimal("50.00"), Weight.WeightUnit.KILOGRAM),
      secondWeighing = Weight(BigDecimal("10.00"), Weight.WeightUnit.KILOGRAM),
      createdAt = Clock.System.now(),
    )

    val newId = WeightTicketId(999)
    val copiedTicket = originalTicket.copy(newId)

    assertThat(copiedTicket.id).isEqualTo(newId)
    assertThat(copiedTicket.status).isEqualTo(WeightTicketStatus.DRAFT)
    assertThat(copiedTicket.consignorParty).isEqualTo(originalTicket.consignorParty)
    assertThat(copiedTicket.lines.getLines().first().waste).isEqualTo(originalTicket.lines.getLines().first().waste)
    assertThat(copiedTicket.lines.getLines().first().weight).isEqualTo(originalTicket.lines.getLines().first().weight)
    assertThat(copiedTicket.lines.getLines().size).isEqualTo(originalTicket.lines.getLines().size)
    assertThat(copiedTicket.carrierParty).isEqualTo(originalTicket.carrierParty)
    assertThat(copiedTicket.direction).isEqualTo(originalTicket.direction)
    assertThat(copiedTicket.pickupLocation).isEqualTo(originalTicket.pickupLocation)
    assertThat(copiedTicket.deliveryLocation).isEqualTo(originalTicket.deliveryLocation)
    assertThat(copiedTicket.truckLicensePlate).isEqualTo(originalTicket.truckLicensePlate)
    assertThat(copiedTicket.reclamation).isEqualTo(originalTicket.reclamation)
    assertThat(copiedTicket.note).isEqualTo(originalTicket.note)
    assertThat(copiedTicket.tarraWeight).isEqualTo(originalTicket.tarraWeight)
    assertThat(copiedTicket.secondWeighing).isEqualTo(originalTicket.secondWeighing)
  }

  @Test
  fun `copied weight ticket has new creation timestamp`() {
    val originalTicket = weightTicket()
    val originalCreatedAt = originalTicket.createdAt

    val copiedTicket = originalTicket.copy(WeightTicketId(999))

    assertThat(copiedTicket.createdAt).isNotEqualTo(originalCreatedAt)
  }

  @Test
  fun `copied weight ticket has null updatedAt`() {
    val originalTicket = weightTicket().apply {
      updatedAt = Clock.System.now()
    }

    val copiedTicket = originalTicket.copy(WeightTicketId(999))

    assertThat(copiedTicket.updatedAt).isNull()
  }

  @Test
  fun `copied weight ticket has same weightedAt`() {
    val now = Clock.System.now()
    val originalTicket = weightTicket().apply {
      weightedAt = now
    }

    val copiedTicket = originalTicket.copy(WeightTicketId(999))

    assertThat(copiedTicket.weightedAt).isEqualTo(now)
  }

  @Test
  fun `copied weight ticket always has draft status regardless of original status`() {
    val completedTicket = weightTicket(WeightTicketStatus.COMPLETED)
    val invoicedTicket = weightTicket(WeightTicketStatus.INVOICED)
    val cancelledTicket = weightTicket(WeightTicketStatus.CANCELLED)

    val copiedFromCompleted = completedTicket.copy(WeightTicketId(1))
    val copiedFromInvoiced = invoicedTicket.copy(WeightTicketId(2))
    val copiedFromCancelled = cancelledTicket.copy(WeightTicketId(3))

    assertThat(copiedFromCompleted.status).isEqualTo(WeightTicketStatus.DRAFT)
    assertThat(copiedFromInvoiced.status).isEqualTo(WeightTicketStatus.DRAFT)
    assertThat(copiedFromCancelled.status).isEqualTo(WeightTicketStatus.DRAFT)
  }

  @Test
  fun `copied weight ticket with empty lines`() {
    val originalTicket = weightTicket().apply {
      lines = EMPTY_LINES
    }

    val copiedTicket = originalTicket.copy(WeightTicketId(999))

    assertThat(copiedTicket.lines.isEmpty()).isTrue()
    assertThat(copiedTicket.lines.getLines()).isEmpty()
  }

  @Test
  fun `copied weight ticket with multiple lines`() {
    val multipleLines = WeightTicketLines(
      listOf(
        WeightTicketLine(
          waste = WasteStreamNumber("111111111111"),
          weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)
        ),
        WeightTicketLine(
          waste = WasteStreamNumber("222222222222"),
          weight = Weight(BigDecimal("200.00"), Weight.WeightUnit.KILOGRAM)
        ),
        WeightTicketLine(
          waste = WasteStreamNumber("333333333333"),
          weight = Weight(BigDecimal("300.00"), Weight.WeightUnit.KILOGRAM)
        )
      )
    )
    val originalTicket = weightTicket().apply {
      lines = multipleLines
    }

    val copiedTicket = originalTicket.copy(WeightTicketId(999))

    assertThat(copiedTicket.lines.getLines()).hasSize(3)
    assertThat(copiedTicket.lines.getLines()[0].waste.number).isEqualTo("111111111111")
    assertThat(copiedTicket.lines.getLines()[1].waste.number).isEqualTo("222222222222")
    assertThat(copiedTicket.lines.getLines()[2].waste.number).isEqualTo("333333333333")
  }

  @Test
  fun `copied weight ticket with null optional fields`() {
    val originalTicket = WeightTicket(
      id = WeightTicketId(1),
      consignorParty = Consignor.Company(companyId()),
      status = WeightTicketStatus.DRAFT,
      lines = EMPTY_LINES,
      carrierParty = null,
      direction = WeightTicketDirection.INBOUND,
      pickupLocation = null,
      deliveryLocation = null,
      truckLicensePlate = null,
      reclamation = null,
      note = null,
      tarraWeight = null,
      secondWeighing = null,
      createdAt = Clock.System.now(),
    )

    val copiedTicket = originalTicket.copy(WeightTicketId(999))

    assertThat(copiedTicket.carrierParty).isNull()
    assertThat(copiedTicket.pickupLocation).isNull()
    assertThat(copiedTicket.deliveryLocation).isNull()
    assertThat(copiedTicket.truckLicensePlate).isNull()
    assertThat(copiedTicket.reclamation).isNull()
    assertThat(copiedTicket.note).isNull()
    assertThat(copiedTicket.tarraWeight).isNull()
    assertThat(copiedTicket.secondWeighing).isNull()
  }

  private fun companyId(): CompanyId = CompanyId(UUID.randomUUID())
}

class CancellationReasonTest {

  @Test
  fun `can create cancellation reason with valid non-empty string`() {
    assertDoesNotThrow {
      CancellationReason("Valid reason")
    }
  }

  @Test
  fun `can create cancellation reason with single character`() {
    assertDoesNotThrow {
      CancellationReason("A")
    }
  }

  @Test
  fun `can create cancellation reason with special characters`() {
    assertDoesNotThrow {
      CancellationReason("Reason with special chars: !@#$%^&*()")
    }
  }

  @Test
  fun `can create cancellation reason with long text`() {
    val longReason = "A".repeat(1000)
    assertDoesNotThrow {
      CancellationReason(longReason)
    }
  }

  @Test
  fun `cannot create cancellation reason with empty string`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      CancellationReason("")
    }

    assertThat(exception).hasMessageContaining("Een reden van annulering is verplicht.")
  }

  @Test
  fun `cannot create cancellation reason with only whitespace`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      CancellationReason("   ")
    }

    assertThat(exception).hasMessageContaining("Een reden van annulering is verplicht.")
  }

  @Test
  fun `cannot create cancellation reason with only tabs`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      CancellationReason("\t\t\t")
    }

    assertThat(exception).hasMessageContaining("Een reden van annulering is verplicht.")
  }

  @Test
  fun `cannot create cancellation reason with only newlines`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      CancellationReason("\n\n\n")
    }

    assertThat(exception).hasMessageContaining("Een reden van annulering is verplicht.")
  }

  @Test
  fun `can create cancellation reason with mixed whitespace and text`() {
    assertDoesNotThrow {
      CancellationReason("  Valid reason with spaces  ")
    }
  }

  @Test
  fun `cancellation reason stores the value correctly`() {
    val reason = "Test cancellation reason"
    val cancellationReason = CancellationReason(reason)

    assertThat(cancellationReason.value).isEqualTo(reason)
  }

  @Test
  fun `cancellation reason is data class with proper equality`() {
    val reason1 = CancellationReason("Same reason")
    val reason2 = CancellationReason("Same reason")

    assertThat(reason1).isEqualTo(reason2)
  }

  @Test
  fun `cancellation reason with different values are not equal`() {
    val reason1 = CancellationReason("Reason 1")
    val reason2 = CancellationReason("Reason 2")

    assertThat(reason1).isNotEqualTo(reason2)
  }
}
