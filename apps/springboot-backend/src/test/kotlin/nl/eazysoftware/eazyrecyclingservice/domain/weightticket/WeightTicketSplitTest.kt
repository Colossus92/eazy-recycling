package nl.eazysoftware.eazyrecyclingservice.domain.weightticket

import kotlinx.datetime.Clock
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Weight
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertFailsWith

class WeightTicketSplitTest {

    @Test
    fun `should split weight ticket with 50-50 percentage`() {
        // Given
        val originalTicket = createWeightTicketWithLines(
            lines = listOf(
                WeightTicketLine(
                    waste = WasteStreamNumber("123456789012"),
                    weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)
                )
            )
        )

        // When
        val newTicket = originalTicket.split(
            newId = WeightTicketId(2),
            originalPercentage = 50,
            newPercentage = 50
        )

        // Then - Original ticket should have 50% of weight
        assertThat(originalTicket.lines.getLines()).hasSize(1)
        assertThat(originalTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("50.00"))
        assertThat(originalTicket.updatedAt).isNotNull()

        // Then - New ticket should have 50% of weight
        assertThat(newTicket.id).isEqualTo(WeightTicketId(2))
        assertThat(newTicket.lines.getLines()).hasSize(1)
        assertThat(newTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("50.00"))
        assertThat(newTicket.status).isEqualTo(WeightTicketStatus.DRAFT)
    }

    @Test
    fun `should split weight ticket with 70-30 percentage`() {
        // Given
        val originalTicket = createWeightTicketWithLines(
            lines = listOf(
                WeightTicketLine(
                    waste = WasteStreamNumber("123456789012"),
                    weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)
                )
            )
        )

        // When
        val newTicket = originalTicket.split(
            newId = WeightTicketId(2),
            originalPercentage = 70,
            newPercentage = 30
        )

        // Then
        assertThat(originalTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("70.00"))
        assertThat(newTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("30.00"))
    }

    @Test
    fun `should split weight ticket with multiple lines`() {
        // Given
        val originalTicket = createWeightTicketWithLines(
            lines = listOf(
                WeightTicketLine(
                    waste = WasteStreamNumber("123456789012"),
                    weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)
                ),
                WeightTicketLine(
                    waste = WasteStreamNumber("987654321098"),
                    weight = Weight(BigDecimal("200.00"), Weight.WeightUnit.KILOGRAM)
                ),
                WeightTicketLine(
                    waste = WasteStreamNumber("555555555555"),
                    weight = Weight(BigDecimal("50.50"), Weight.WeightUnit.KILOGRAM)
                )
            )
        )

        // When
        val newTicket = originalTicket.split(
            newId = WeightTicketId(2),
            originalPercentage = 60,
            newPercentage = 40
        )

        // Then - Original ticket should have 60% of all weights
        assertThat(originalTicket.lines.getLines()).hasSize(3)
        assertThat(originalTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("60.00"))
        assertThat(originalTicket.lines.getLines()[1].weight.value).isEqualByComparingTo(BigDecimal("120.00"))
        assertThat(originalTicket.lines.getLines()[2].weight.value).isEqualByComparingTo(BigDecimal("30.30"))

        // Then - New ticket should have 40% of all weights
        assertThat(newTicket.lines.getLines()).hasSize(3)
        assertThat(newTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("40.00"))
        assertThat(newTicket.lines.getLines()[1].weight.value).isEqualByComparingTo(BigDecimal("80.00"))
        assertThat(newTicket.lines.getLines()[2].weight.value).isEqualByComparingTo(BigDecimal("20.20"))
    }

    @Test
    fun `should preserve waste stream numbers when splitting`() {
        // Given
        val wasteStream1 = WasteStreamNumber("123456789012")
        val wasteStream2 = WasteStreamNumber("987654321098")
        val originalTicket = createWeightTicketWithLines(
            lines = listOf(
                WeightTicketLine(waste = wasteStream1, weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)),
                WeightTicketLine(waste = wasteStream2, weight = Weight(BigDecimal("200.00"), Weight.WeightUnit.KILOGRAM))
            )
        )

        // When
        val newTicket = originalTicket.split(
            newId = WeightTicketId(2),
            originalPercentage = 50,
            newPercentage = 50
        )

        // Then - Both tickets should have same waste stream numbers
        assertThat(originalTicket.lines.getLines()[0].waste).isEqualTo(wasteStream1)
        assertThat(originalTicket.lines.getLines()[1].waste).isEqualTo(wasteStream2)
        assertThat(newTicket.lines.getLines()[0].waste).isEqualTo(wasteStream1)
        assertThat(newTicket.lines.getLines()[1].waste).isEqualTo(wasteStream2)
    }

    @Test
    fun `should copy all metadata to new ticket`() {
        // Given
        val consignor = Consignor.Company(CompanyId(UUID.randomUUID()))
        val carrier = CompanyId(UUID.randomUUID())
        val licensePlate = LicensePlate("AB-123-CD")
        val reclamation = "Test reclamation"
        val note = Note("Test note")

        val originalTicket = WeightTicket(
            id = WeightTicketId(1),
            consignorParty = consignor,
            status = WeightTicketStatus.DRAFT,
            lines = WeightTicketLines(
                listOf(
                    WeightTicketLine(
                        waste = WasteStreamNumber("123456789012"),
                        weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)
                    )
                )
            ),
            carrierParty = carrier,
            truckLicensePlate = licensePlate,
            reclamation = reclamation,
            note = note,
            createdAt = Clock.System.now()
        )

        // When
        val newTicket = originalTicket.split(
            newId = WeightTicketId(2),
            originalPercentage = 50,
            newPercentage = 50
        )

        // Then - New ticket should have same metadata
        assertThat(newTicket.consignorParty).isEqualTo(consignor)
        assertThat(newTicket.carrierParty).isEqualTo(carrier)
        assertThat(newTicket.truckLicensePlate).isEqualTo(licensePlate)
        assertThat(newTicket.reclamation).isEqualTo(reclamation)
        assertThat(newTicket.note?.description).isEqualTo(note.description)
        assertThat(newTicket.status).isEqualTo(WeightTicketStatus.DRAFT)
    }

    @Test
    fun `should fail when percentages do not add up to 100`() {
        // Given
        val originalTicket = createWeightTicketWithLines()

        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            originalTicket.split(
                newId = WeightTicketId(2),
                originalPercentage = 60,
                newPercentage = 50
            )
        }

        assertThat(exception).hasMessageContaining("Percentages moeten optellen tot 100")
    }

    @Test
    fun `should fail when original percentage is zero`() {
        // Given
        val originalTicket = createWeightTicketWithLines()

        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            originalTicket.split(
                newId = WeightTicketId(2),
                originalPercentage = 0,
                newPercentage = 100
            )
        }

        assertThat(exception).hasMessageContaining("Percentages moeten positief zijn")
    }

    @Test
    fun `should fail when new percentage is zero`() {
        // Given
        val originalTicket = createWeightTicketWithLines()

        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            originalTicket.split(
                newId = WeightTicketId(2),
                originalPercentage = 100,
                newPercentage = 0
            )
        }

        assertThat(exception).hasMessageContaining("Percentages moeten positief zijn")
    }

    @Test
    fun `should fail when original percentage is negative`() {
        // Given
        val originalTicket = createWeightTicketWithLines()

        // When/Then
        val exception = assertFailsWith<IllegalArgumentException> {
            originalTicket.split(
                newId = WeightTicketId(2),
                originalPercentage = -10,
                newPercentage = 110
            )
        }

        assertThat(exception).hasMessageContaining("Percentages moeten positief zijn")
    }

    @ParameterizedTest
    @EnumSource(value = WeightTicketStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["DRAFT"])
    fun `should fail when weight ticket status is not DRAFT`(status: WeightTicketStatus) {
        // Given
        val originalTicket = createWeightTicketWithLines(status = status)

        // When/Then
        val exception = assertFailsWith<IllegalStateException> {
            originalTicket.split(
                newId = WeightTicketId(2),
                originalPercentage = 50,
                newPercentage = 50
            )
        }

        assertThat(exception).hasMessageContaining("Weegbon kan alleen worden gesplitst als de status openstaand is")
    }

    @Test
    fun `should fail when weight ticket has no lines`() {
        // Given
        val originalTicket = WeightTicket(
            id = WeightTicketId(1),
            consignorParty = Consignor.Company(CompanyId(UUID.randomUUID())),
            status = WeightTicketStatus.DRAFT,
            lines = WeightTicketLines(emptyList()),
            carrierParty = null,
            truckLicensePlate = null,
            reclamation = null,
            note = null,
            createdAt = Clock.System.now()
        )

        // When/Then
        val exception = assertFailsWith<IllegalStateException> {
            originalTicket.split(
                newId = WeightTicketId(2),
                originalPercentage = 50,
                newPercentage = 50
            )
        }

        assertThat(exception).hasMessageContaining("Kan geen lege weegbon splitsen")
    }

    @Test
    fun `should handle decimal weights correctly with rounding`() {
        // Given
        val originalTicket = createWeightTicketWithLines(
            lines = listOf(
                WeightTicketLine(
                    waste = WasteStreamNumber("123456789012"),
                    weight = Weight(BigDecimal("100.33"), Weight.WeightUnit.KILOGRAM)
                )
            )
        )

        // When
        val newTicket = originalTicket.split(
            newId = WeightTicketId(2),
            originalPercentage = 33,
            newPercentage = 67
        )

        // Then - Should round to 2 decimal places
        assertThat(originalTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("33.11"))
        assertThat(newTicket.lines.getLines()[0].weight.value).isEqualByComparingTo(BigDecimal("67.22"))
    }

    @Test
    fun `should set new ticket created timestamp`() {
        // Given
        val originalTicket = createWeightTicketWithLines()
        val originalCreatedAt = originalTicket.createdAt

        // When
        val newTicket = originalTicket.split(
            newId = WeightTicketId(2),
            originalPercentage = 50,
            newPercentage = 50
        )

        // Then
        assertThat(newTicket.createdAt).isNotNull()
        assertThat(newTicket.createdAt).isNotEqualTo(originalCreatedAt)
    }

    private fun createWeightTicketWithLines(
        status: WeightTicketStatus = WeightTicketStatus.DRAFT,
        lines: List<WeightTicketLine> = listOf(
            WeightTicketLine(
                waste = WasteStreamNumber("123456789012"),
                weight = Weight(BigDecimal("100.00"), Weight.WeightUnit.KILOGRAM)
            )
        )
    ): WeightTicket {
        return WeightTicket(
            id = WeightTicketId(1),
            consignorParty = Consignor.Company(CompanyId(UUID.randomUUID())),
            status = status,
            lines = WeightTicketLines(lines),
            carrierParty = CompanyId(UUID.randomUUID()),
            truckLicensePlate = LicensePlate("AB-123-CD"),
            reclamation = "Test reclamation",
            note = Note("Test note"),
            createdAt = Clock.System.now()
        )
    }
}
