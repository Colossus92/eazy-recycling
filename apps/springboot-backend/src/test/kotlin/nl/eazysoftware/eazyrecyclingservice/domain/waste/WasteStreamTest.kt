package nl.eazysoftware.eazyrecyclingservice.domain.waste

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestLocationFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.util.*
import kotlin.test.assertFailsWith

class WasteStreamTest {

  @Test
  fun `wasteStream with proximity description can be initialized`() {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        pickupLocation = TestLocationFactory.createProximityDescription(),
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
        pickupParty = companyId(),
      )
    }
  }

  @Test
  fun `waste stream with Dutch address can be initialized`() {
    assertDoesNotThrow {
      wasteStream()
    }
  }

  @Test
  fun `waste stream with no origin location can be initialized for route collection`() {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.ROUTE,
        pickupLocation = Location.NoLocation,
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        collectorParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }
  }

  @Test
  fun `a waste stream cannot have both a collector and broker`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        pickupLocation = Location.DutchAddress(
          address = Address(
            streetName = "Stadstraat",
            postalCode = DutchPostalCode("1234 AB"),
            buildingNumber = "2",
            city = City("Test City"),
          )
        ),
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        brokerParty = companyId(),
        collectorParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }

    assertThat(exception.message).isEqualTo("Een afvalstroomnummer kan niet zowel een handelaar als een bemiddelaar hebben")
  }

  @Test
  fun `a waste stream should have a origin location when default collection and company consignor`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        pickupLocation = Location.NoLocation,
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }

    assertThat(exception.message).isEqualTo("Locatie van herkomst is verplicht bij normale inzameling en zakelijke ontdoener")
  }

  @Test
  fun `a waste stream should be expired if last activity is more than five years ago`() {
    val status = wasteStream(
      status = WasteStreamStatus.ACTIVE,
      lastActivityAt = Instant.DISTANT_PAST,
    ).getEffectiveStatus()

    assertThat(status).isEqualTo(EffectiveStatus.EXPIRED)
  }

  @Test
  fun `a waste stream is set to inactive when deleted`() {
    val wasteStream = wasteStream()

    wasteStream.delete()

    assertThat(wasteStream.status).isEqualTo(WasteStreamStatus.INACTIVE)
  }

  @Test
  fun `a waste stream should not be deletable when already set to inactive`() {
    val wasteStream = wasteStream(
      status = WasteStreamStatus.INACTIVE,
    )

    val exception = assertFailsWith<IllegalStateException> {
      wasteStream.delete()
    }

    assertThat(exception.message).isEqualTo("Afvalstroom is al inactief en kan niet opnieuw worden verwijderd")
  }

  @Test
  fun `a waste stream is set to active when activated`() {
    val wasteStream = wasteStream(
      status = WasteStreamStatus.DRAFT,
    )

    wasteStream.activate()

    assertThat(wasteStream.status).isEqualTo(WasteStreamStatus.ACTIVE)
  }

  @Test
  fun `a waste stream should not be activatable when not in draft status`() {
    val wasteStream = wasteStream(
      status = WasteStreamStatus.ACTIVE,
    )

    val exception = assertFailsWith<IllegalStateException> {
      wasteStream.activate()
    }

    assertThat(exception.message).isEqualTo("Afvalstroom kan alleen worden geactiveerd vanuit DRAFT status. Huidige status: ACTIVE")
  }

  @Test
  fun `a waste stream can be updated when in draft status`() {
    val wasteStream = wasteStream(
      status = WasteStreamStatus.DRAFT,
    )
    val newWasteType = WasteType(
      "aluminium",
      EuralCode("16 07 09"),
      ProcessingMethod("A.02"),
    )

    wasteStream.update(
      wasteType = newWasteType,
    )

    assertThat(wasteStream.wasteType).isEqualTo(newWasteType)
  }

  @Test
  fun `a waste stream should not be updatable when not in draft status`() {
    val wasteStream = wasteStream(
      status = WasteStreamStatus.ACTIVE,
    )

    val exception = assertFailsWith<IllegalStateException> {
      wasteStream.update(
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
      )
    }

    assertThat(exception.message).isEqualTo("Afvalstroom kan alleen worden gewijzigd als de status DRAFT is. Huidige status: ACTIVE")
  }

  @ParameterizedTest
  @EnumSource(value = WasteCollectionType::class, mode = EnumSource.Mode.EXCLUDE, names = ["DEFAULT"])
  fun `a waste stream should have no origin location when non-default collection`(collectionType: WasteCollectionType) {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = collectionType,
        pickupLocation = Location.NoLocation,
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        collectorParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }
  }

  @ParameterizedTest
  @EnumSource(value = WasteCollectionType::class, mode = EnumSource.Mode.EXCLUDE, names = ["DEFAULT"])
  fun `a waste stream should have a collectorParty when non-default collection`(collectionType: WasteCollectionType) {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = collectionType,
        pickupLocation = Location.NoLocation,
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }

    assertThat(exception.message).isEqualTo("Als er RouteInzameling of InzamelaarsRegeling wordt toegepast dan moet de inzamelaar zijn gevuld")
  }

  @Test
  fun `a waste stream should have no origin location when person consignor`() {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        pickupLocation = Location.NoLocation,
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Person,
        pickupParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }
  }

  @ParameterizedTest
  @EnumSource(value = WasteCollectionType::class, mode = EnumSource.Mode.EXCLUDE, names = ["DEFAULT"])
  fun `a waste stream can't have origin location with non-default collection`(collectionType: WasteCollectionType) {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = collectionType,
        pickupLocation = Location.DutchAddress(
          address = Address(
            streetName = "Stadstraat",
            postalCode = DutchPostalCode("1234 AB"),
            buildingNumber = "123",
            city = City("Test city"),
          ),
        ),
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }

    assertThat(exception.message).isEqualTo("Locatie van herkomst is alleen toegestaan bij normale inzameling en zakelijke ontdoener")
  }

  @Test
  fun `a waste stream can't have origin location with person consignor`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        pickupLocation = Location.DutchAddress(
          address = Address(
            streetName = "Stadstraat",
            postalCode = DutchPostalCode("1234 AB"),
            buildingNumber = "123",
            city = City("Test city"),
          ),
        ),
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Person,
        pickupParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }

    assertThat(exception.message).isEqualTo("Locatie van herkomst is alleen toegestaan bij normale inzameling en zakelijke ontdoener")
  }

  @Test
  fun `the first five numbers of a waste stream number should be equal to the processorPartyId`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = WasteStreamNumber("123466789012"),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        pickupLocation = Location.DutchAddress(
          address = Address(
            streetName = "Stadstraat",
            postalCode = DutchPostalCode("1234 AB"),
            buildingNumber = "123",
            city = City("Test city"),
          ),
        ),
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }

    assertThat(exception.message).isEqualTo("De eerste 5 posities van het Afvalstroomnummer moeten gelijk zijn aan de LocatieOntvangst.")
  }

  @ParameterizedTest
  @EnumSource(value = WasteCollectionType::class, mode = EnumSource.Mode.EXCLUDE, names = ["DEFAULT"])
  fun `when the consignor is a person, the collection type should be DEFAULT`(collectionType: WasteCollectionType) {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = collectionType,
        pickupLocation = Location.NoLocation,
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Person,
        pickupParty = companyId(),
        collectorParty = companyId(),
        consignorClassification = ConsignorClassification.PICKUP_PARTY,
      )
    }

    assertThat(exception.message).isEqualTo("Als de ontdoener een particulier is dan mag route inzameling en inzamelaarsregeling niet worden toegepast")
  }

  @Nested
  inner class DutchAddress {
    @Test
    fun `a dutch address can't have a blank house number`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.DutchAddress(
          address = Address(
            streetName = "Stadstraat",
            postalCode = DutchPostalCode("1234 AB"),
            buildingNumber = "",
            city = City("Test city"),
          ),
        )
      }
      assertThat(exception.message).isEqualTo("Huisnummer moet een waarde hebben.")
    }

    @Test
    fun `a dutch address must have Nederland as country`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.DutchAddress(
          address = Address(
            streetName = "Stadstraat",
            postalCode = DutchPostalCode("1234 AB"),
            buildingNumber = "123",
            country = "Belgium",
            city = City("Test city")
          ),
        )
      }
      assertThat(exception.message).isEqualTo("Het land dient Nederland te zijn, maar was: Belgium")
    }

    @Test
    fun `a dutch address can have a house number addition`() {
      assertDoesNotThrow {
        Location.DutchAddress(
          address = Address(
            streetName = "Stadstraat",
            postalCode = DutchPostalCode("1234 AB"),
            buildingNumber = "123",
            buildingNumberAddition = "A",
            city = City("Test city")
          ),
        )
      }
    }
  }

  @Nested
  inner class ProximityDescription {
    @Test
    fun `a proximity description can't have a dutch postal code`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.ProximityDescription(
          "1234 AB",
          City("Stad"),
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 1234 AB")
    }

    @Test
    fun `a proximity description can't have a postal code with 3 digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.ProximityDescription(
          "123",
          City("Stad"),
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 123")
    }

    @Test
    fun `a proximity description can't have a postal code with 5 digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.ProximityDescription(
          "12345",
          City("Stad"),
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 12345")
    }

    @Test
    fun `a proximity description can't have a postal code with a letter`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.ProximityDescription(
          "1234a",
          City("Stad"),
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 1234a")
    }

    @Test
    fun `a proximity description can't have a blank city`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.ProximityDescription(
          "1234",
          City(""),
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("De stad is verplicht")
    }

    @Test
    fun `a proximity description can't have a blank proximity description`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        Location.ProximityDescription(
          "1234",
          City("Stad"),
          ""
        )
      }
      assertThat(exception.message).isEqualTo("De nabijheidsbeschrijving is verplicht")
    }
  }

  @Nested
  inner class ProccessorPartyId {
    @Test
    fun `a processor party id can't have four digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        ProcessorPartyId("1234")
      }
      assertThat(exception.message).isEqualTo("Het verwerkersnummer moet exact 5 tekens lang zijn, maar is: 1234")
    }

    @Test
    fun `a processor party id can't have six digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        ProcessorPartyId("123456")
      }
      assertThat(exception.message).isEqualTo("Het verwerkersnummer moet exact 5 tekens lang zijn, maar is: 123456")
    }
  }

  @ParameterizedTest
  @CsvSource("12345678901", "1234567890123")
  fun `a waste stream number should be exactly 12 characters long`(value: String) {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStreamNumber(value)
    }
    assertThat(exception.message).isEqualTo("Een afvalstroomnummer dient 12 tekens lang te zijn")
  }

  private fun wasteStream(
    status: WasteStreamStatus = WasteStreamStatus.ACTIVE,
    lastActivityAt: Instant = Clock.System.now(),
  ): WasteStream = WasteStream(
    wasteStreamNumber = wasteStreamNumber(),
    wasteType = wasteType(),
    collectionType = WasteCollectionType.DEFAULT,
    pickupLocation = TestLocationFactory.createDutchAddress(),
    deliveryLocation = destinationLocation(),
    consignorParty = Consignor.Company(companyId()),
    pickupParty = companyId(),
    status = status,
    lastActivityAt = lastActivityAt,
    consignorClassification = ConsignorClassification.PICKUP_PARTY,
  )

  private fun wasteStreamNumber(): WasteStreamNumber = WasteStreamNumber("123456789012")

  private fun wasteType(): WasteType = WasteType(
    "koper",
    EuralCode("16 07 08"),
    ProcessingMethod("A.01"),
  )

  private fun companyId(): CompanyId = CompanyId(UUID.randomUUID())

  private fun destinationLocation(): WasteDeliveryLocation = WasteDeliveryLocation(
    ProcessorPartyId("12345"),
  )
}
