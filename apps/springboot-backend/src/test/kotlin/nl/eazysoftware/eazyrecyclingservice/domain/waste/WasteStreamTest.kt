package nl.eazysoftware.eazyrecyclingservice.domain.waste

import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
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
        pickupLocation = PickupLocation.ProximityDescription(
          "1234",
          "Stad",
          "Nabijheidsbeschrijving"
        ),
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "Stad",
        ),
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.NoPickupLocation,
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "Stad",
        ),
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.NoPickupLocation,
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.NoPickupLocation,
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.NoPickupLocation,
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.NoPickupLocation,
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "123"
        ),
        deliveryLocation = destinationLocation(),
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
        pickupLocation = PickupLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "123"
        ),
        deliveryLocation = destinationLocation(),
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
        wasteStreamNumber = WasteStreamNumber("12346678912"),
        wasteType = wasteType(),
        collectionType = WasteCollectionType.DEFAULT,
        pickupLocation = PickupLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "Stad",
        ),
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Company(companyId()),
        pickupParty = companyId(),
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
        pickupLocation = PickupLocation.NoPickupLocation,
        deliveryLocation = destinationLocation(),
        consignorParty = Consignor.Person,
        pickupParty = companyId(),
        collectorParty = companyId(),
      )
    }

    assertThat(exception.message).isEqualTo("Als de ontdoener een particulier is dan mag route inzameling en inzamelaarsregeling niet worden toegepast")
  }

  @Nested
  inner class DutchAddress {
    @Test
    fun `a dutch address can't have a blank house number`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        PickupLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          ""
        )
      }
      assertThat(exception.message).isEqualTo("Het huisnummer is verplicht")
    }

    @Test
    fun `a dutch address must have Nederland as country`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        PickupLocation.DutchAddress(
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
        PickupLocation.DutchAddress(
          DutchPostalCode("1234 AB"),
          "123",
          buildingNumberAddition = "A"
        )
      }
    }
  }

  @Nested
  inner class ProximityDescription {
    @Test
    fun `a proximity description can't have a dutch postal code`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        PickupLocation.ProximityDescription(
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
        PickupLocation.ProximityDescription(
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
        PickupLocation.ProximityDescription(
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
        PickupLocation.ProximityDescription(
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
        PickupLocation.ProximityDescription(
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
        PickupLocation.ProximityDescription(
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
        ProcessorPartyId("1234")
      }
      assertThat(exception.message).isEqualTo("Het verwerkersnummer moet exact 5 tekens lang staan, maar is: 1234")
    }

    @Test
    fun `a processor party id can't have six digits`() {
      val exception = assertFailsWith<IllegalArgumentException> {
        ProcessorPartyId("123456")
      }
      assertThat(exception.message).isEqualTo("Het verwerkersnummer moet exact 5 tekens lang staan, maar is: 123456")
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

  private fun wasteStreamNumber(): WasteStreamNumber = WasteStreamNumber("123456789012")

  private fun wasteType(): WasteType = WasteType(
    "koper",
    EuralCode("16 07 08"),
    ProcessingMethod("A.01"),
  )

  private fun companyId(): CompanyId = CompanyId(UUID.randomUUID())

  private fun destinationLocation(): DeliveryLocation = DeliveryLocation(
    ProcessorPartyId("12345"),
  )
}
