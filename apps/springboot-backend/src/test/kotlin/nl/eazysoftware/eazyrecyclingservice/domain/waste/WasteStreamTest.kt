package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID
import kotlin.test.assertFailsWith

class WasteStreamTest {

  @Test
  fun `wasteStream with proximity description can be initialized`() {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        originLocation = OriginLocation.ProximityDescription(
          "1234",
          "Stad",
          "Nabijheidsbeschrijving"
        ),
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
      )
    }
  }

  @Test
  fun `waste stream with Dutch address can be initialized`() {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        originLocation = OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "Stad",
        ),
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
      )
    }
  }

  @Test
  fun `waste stream with no origin location can be initialized for route collection`() {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.ROUTE,
        originLocation = OriginLocation.NoOriginLocation,
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        collectorParty = companyId(),
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
        originLocation = OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "Stad",
        ),
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        brokerParty = companyId(),
        collectorParty = companyId(),
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
        originLocation = OriginLocation.NoOriginLocation,
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
      )
    }

    assertThat(exception.message).isEqualTo("Locatie van herkomst is verplicht bij normale inzameling en zakelijke ontdoener")
  }

  @ParameterizedTest
  @EnumSource(value = WasteCollectionType::class, mode = EnumSource.Mode.EXCLUDE, names = ["DEFAULT"])
  fun `a waste stream should have no origin location when non-default collection`(collectionType: WasteCollectionType) {
    assertDoesNotThrow {
      WasteStream(
        wasteStreamNumber = wasteStreamNumber(),
        wasteType = wasteType(),
        collectionType = collectionType,
        originLocation = OriginLocation.NoOriginLocation,
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
        collectorParty = companyId(),
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
        originLocation = OriginLocation.NoOriginLocation,
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
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
        originLocation = OriginLocation.NoOriginLocation,
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Person,
        pickupParty = companyId(),
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
        originLocation = OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "123"
        ),
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
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
        originLocation = OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "123"
        ),
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Person,
        pickupParty = companyId(),
      )
    }

    assertThat(exception.message).isEqualTo("Locatie van herkomst is alleen toegestaan bij normale inzameling en zakelijke ontdoener")
  }

  @Test
  fun `the first five numbers of a waste stream number should be equal to the processorPartyId`() {
    val exception = assertFailsWith<IllegalArgumentException> {
      WasteStream(
        wasteStreamNumber = WasteStreamNumber(12346678912),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        originLocation = OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "Stad",
        ),
        destinationLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
      )
    }

    assertThat(exception.message).isEqualTo("De eerste 5 posities van het Afvalstroomnummer moeten gelijk zijn aan de LocatieOntvangst.")
  }

  @Nested
  inner class DutchAddress {
    @Test
    fun `a dutch address can't have a blank house number`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          ""
        )
      }
      assertThat(exception.message).isEqualTo("Het huisnummer is verplicht")
    }

    @Test
    fun `a dutch address must have Nederland as country`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "123",
          country = "Belgium"
        )
      }
      assertThat(exception.message).isEqualTo("Het land dient Nederland te zijn, maar was: Belgium")
    }

    @Test
    fun `a dutch address can have a house number addition`() {
      assertDoesNotThrow {
        OriginLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "123",
          houseNumberAddition = "A"
        )
      }
    }
  }

  @Nested
  inner class ProximityDescription {
    @Test
    fun `a proximity description can't have a dutch postal code`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.ProximityDescription(
          "1234 AB",
          "Stad",
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 1234 AB")
    }

    @Test
    fun `a proximity description can't have a postal code with 3 digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.ProximityDescription(
          "123",
          "Stad",
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 123")
    }

    @Test
    fun `a proximity description can't have a postal code with 5 digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.ProximityDescription(
          "12345",
          "Stad",
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 12345")
    }

    @Test
    fun `a proximity description can't have a postal code with a letter`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.ProximityDescription(
          "1234a",
          "Stad",
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("Voor een nabijheidsbeschrijving moet de postcode alleen vier cijfers bevatten, maar was: 1234a")
    }

    @Test
    fun `a proximity description can't have a blank city`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.ProximityDescription(
          "1234",
          "",
          "Nabijheidsbeschrijving"
        )
      }
      assertThat(exception.message).isEqualTo("De stad moet een waarde hebben")
    }

    @Test
    fun `a proximity description can't have a blank proximity description`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        OriginLocation.ProximityDescription(
          "1234",
          "Stad",
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
        ProcessorPartyId(1234)
      }
      assertThat(exception.message).isEqualTo("Het verwerkersnummer moet exact 5 tekens lang staan, maar is: 1234")
    }

    @Test
    fun `a processor party id can't have six digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        ProcessorPartyId(123456)
      }
      assertThat(exception.message).isEqualTo("Het verwerkersnummer moet exact 5 tekens lang staan, maar is: 123456")
    }
  }

  private fun wasteStreamNumber(): WasteStreamNumber = WasteStreamNumber(123456789012)

  private fun wasteType(): WasteType = WasteType(
    "koper",
    EuralCode("16 07 08"),
    ProcessingMethod("A.01"),
  )

  private fun companyId(): CompanyId = CompanyId(UUID.randomUUID())

  private fun destinationLocation(): DestinationLocation = DestinationLocation(
    ProcessorPartyId(12345),
    Address(
      streetName = "straatnaam",
      houseNumber = "1",
      houseNumberAddition = null,
      postalCode = DutchPostalCode("1234 AB"),
      city = "city",
      country = "country"
    )
  )
}
