package nl.eazysoftware.eazyrecyclingservice.controller.admin

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.lmaimport.LmaImportErrorJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class LmaImportControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var wasteStreamRepository: WasteStreamJpaRepository

  @Autowired
  private lateinit var companyRepository: CompanyJpaRepository

  @Autowired
  private lateinit var lmaImportErrorRepository: LmaImportErrorJpaRepository

  private lateinit var processorCompany: CompanyDto
  private lateinit var ontdoenerCompany: CompanyDto

  companion object {
    private val TEST_USER_METADATA = mapOf(
      "first_name" to "Test",
      "last_name" to "User"
    )
  }

  @BeforeEach
  fun setup() {
    lmaImportErrorRepository.deleteAll()
    wasteStreamRepository.deleteAll()

    // Create processor company with processor ID 19808
    processorCompany = companyRepository.save(
      TestCompanyFactory.createTestCompany(
        processorId = "19808",
        chamberOfCommerceId = "78083180",
        vihbId = "198080VIXX",
        name = "WHD B.V."
      )
    )

    // Create ontdoener company with KvK 81092248
    ontdoenerCompany = companyRepository.save(
      TestCompanyFactory.createTestCompany(
        processorId = null,
        chamberOfCommerceId = "81092248",
        vihbId = "810933VIXX",
        name = "Test B.V."
      )
    )
  }

  @Test
  fun `should import waste streams from valid CSV`() {
    // Given - a valid LMA CSV with one waste stream
    val csvContent = """
      Afvalstroomnummer,Verwerkersnummer Locatie van Bestemming (LvB),LvB KvK nummer,LvB Bedrijfsnaam,LvB Adres,LvB Postcode/Plaats,Handelsregisternummer Ontdoener,Naam Ontdoener,Land Ontdoener,LocatieHerkomst Straatnaam,LocatieHerkomst Huisnummer,LocatieHerkomst HuisnummerToevoeging,LocatieHerkomst Postcode,LocatieHerkomst Plaats,LocatieHerkomst Nabijheidsbeschrijving,LocatieHerkomst Land,Euralcode,Euralcode Omschrijving,Gebruikelijke Naam Afvalstof,VerwerkingsMethode Code,VerwerkingsMethode Omschrijving,Routeinzameling,Inzamelaarsregeling,Particuliere Ontdoener
      198080000004,19808,78083180,WHD B.V.,verwerkerstraat 4,3702IB 'S-GRAVENZANDE,81092248,Test B.V.,Nederland,Teststraat,4,,3776KK,PIJNACKER,,Nederland,170405,ijzer en staal,ijzer en staal,A02,Overslag / opbulken,N,N,N
    """.trimIndent()

    val file = MockMultipartFile(
      "file",
      "lma-export.csv",
      "text/csv",
      csvContent.toByteArray()
    )

    // When
    val result = mockMvc.perform(
      multipart("/admin/lma/import")
        .file(file)
        .with(jwt()
          .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
          .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.totalRows").value(1))
      .andExpect(jsonPath("$.successfulImports").value(1))
      .andExpect(jsonPath("$.errorCount").value(0))
      .andReturn()

    // Then - verify waste stream was created
    val savedWasteStream = wasteStreamRepository.findById("198080000004")
    assertThat(savedWasteStream).isPresent
    assertThat(savedWasteStream.get().name).isEqualTo("ijzer en staal")
    assertThat(savedWasteStream.get().euralCode.code).isEqualTo("17 04 05")
    assertThat(savedWasteStream.get().processingMethodCode.code).isEqualTo("A.02")
  }

  @Test
  fun `should skip private consignor rows`() {
    // Given - a CSV with a private consignor (Particuliere Ontdoener = J)
    val csvContent = """
      Afvalstroomnummer,Verwerkersnummer Locatie van Bestemming (LvB),LvB KvK nummer,LvB Bedrijfsnaam,LvB Adres,LvB Postcode/Plaats,Handelsregisternummer Ontdoener,Naam Ontdoener,Land Ontdoener,LocatieHerkomst Straatnaam,LocatieHerkomst Huisnummer,LocatieHerkomst HuisnummerToevoeging,LocatieHerkomst Postcode,LocatieHerkomst Plaats,LocatieHerkomst Nabijheidsbeschrijving,LocatieHerkomst Land,Euralcode,Euralcode Omschrijving,Gebruikelijke Naam Afvalstof,VerwerkingsMethode Code,VerwerkingsMethode Omschrijving,Routeinzameling,Inzamelaarsregeling,Particuliere Ontdoener
      198080000221,19808,78083180,WHD B.V.,verwerkerstraat 4,3702IB 'S-GRAVENZANDE,,Particulier,,,,,,,,,170405,ijzer en staal,ijzer en staal,A02,Overslag / opbulken,N,N,J
    """.trimIndent()

    val file = MockMultipartFile(
      "file",
      "lma-export.csv",
      "text/csv",
      csvContent.toByteArray()
    )

    // When
    mockMvc.perform(
      multipart("/admin/lma/import")
        .file(file)
        .with(jwt()
          .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
          .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.totalRows").value(1))
      .andExpect(jsonPath("$.successfulImports").value(0))
      .andExpect(jsonPath("$.skippedRows").value(1))
      .andExpect(jsonPath("$.errorCount").value(0))

    // Then - verify no waste stream was created
    val savedWasteStream = wasteStreamRepository.findById("198080000221")
    assertThat(savedWasteStream).isEmpty
  }

  @Test
  fun `should skip duplicate waste stream numbers`() {
    // Given - a CSV with duplicate waste stream numbers (same number appears twice)
    val csvContent = """
      Afvalstroomnummer,Verwerkersnummer Locatie van Bestemming (LvB),LvB KvK nummer,LvB Bedrijfsnaam,LvB Adres,LvB Postcode/Plaats,Handelsregisternummer Ontdoener,Naam Ontdoener,Land Ontdoener,LocatieHerkomst Straatnaam,LocatieHerkomst Huisnummer,LocatieHerkomst HuisnummerToevoeging,LocatieHerkomst Postcode,LocatieHerkomst Plaats,LocatieHerkomst Nabijheidsbeschrijving,LocatieHerkomst Land,Euralcode,Euralcode Omschrijving,Gebruikelijke Naam Afvalstof,VerwerkingsMethode Code,VerwerkingsMethode Omschrijving,Routeinzameling,Inzamelaarsregeling,Particuliere Ontdoener
      198080000004,19808,78083180,WHD B.V.,verwerkerstraat 4,3702IB 'S-GRAVENZANDE,81092248,Test B.V.,Nederland,Teststraat,4,,3776KK,PIJNACKER,,Nederland,170405,ijzer en staal,ijzer en staal,A02,Overslag / opbulken,N,N,N
      198080000004,19808,78083180,WHD B.V.,verwerkerstraat 4,3702IB 'S-GRAVENZANDE,81092248,Test B.V.,Nederland,Teststraat,4,,3776KK,PIJNACKER,,Nederland,170405,ijzer en staal,ijzer en staal,A02,Overslag / opbulken,N,N,N
    """.trimIndent()

    val file = MockMultipartFile(
      "file",
      "lma-export.csv",
      "text/csv",
      csvContent.toByteArray()
    )

    // When
    mockMvc.perform(
      multipart("/admin/lma/import")
        .file(file)
        .with(jwt()
          .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
          .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.success").value(true))
      .andExpect(jsonPath("$.totalRows").value(1)) // Deduplicated to 1
      .andExpect(jsonPath("$.successfulImports").value(1))
      .andExpect(jsonPath("$.errorCount").value(0))

    // Then - verify only one waste stream was created
    assertThat(wasteStreamRepository.count()).isEqualTo(1)
  }

  @Test
  fun `should record error when company not found by KvK number`() {
    // Given - a CSV with unknown KvK number
    val csvContent = """
      Afvalstroomnummer,Verwerkersnummer Locatie van Bestemming (LvB),LvB KvK nummer,LvB Bedrijfsnaam,LvB Adres,LvB Postcode/Plaats,Handelsregisternummer Ontdoener,Naam Ontdoener,Land Ontdoener,LocatieHerkomst Straatnaam,LocatieHerkomst Huisnummer,LocatieHerkomst HuisnummerToevoeging,LocatieHerkomst Postcode,LocatieHerkomst Plaats,LocatieHerkomst Nabijheidsbeschrijving,LocatieHerkomst Land,Euralcode,Euralcode Omschrijving,Gebruikelijke Naam Afvalstof,VerwerkingsMethode Code,VerwerkingsMethode Omschrijving,Routeinzameling,Inzamelaarsregeling,Particuliere Ontdoener
      198080000099,19808,78083180,WHD B.V.,verwerkerstraat 4,3702IB 'S-GRAVENZANDE,99999999,Unknown Company,Nederland,Teststraat,4,,3776KK,PIJNACKER,,Nederland,170405,ijzer en staal,ijzer en staal,A02,Overslag / opbulken,N,N,N
    """.trimIndent()

    val file = MockMultipartFile(
      "file",
      "lma-export.csv",
      "text/csv",
      csvContent.toByteArray()
    )

    // When
    mockMvc.perform(
      multipart("/admin/lma/import")
        .file(file)
        .with(jwt()
          .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
          .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.totalRows").value(1))
      .andExpect(jsonPath("$.successfulImports").value(0))
      .andExpect(jsonPath("$.errorCount").value(1))

    // Then - verify error was recorded
    val errors = lmaImportErrorRepository.findAll()
    assertThat(errors).hasSize(1)
    assertThat(errors[0].errorCode).isEqualTo("COMPANY_NOT_FOUND")
    assertThat(errors[0].wasteStreamNumber).isEqualTo("198080000099")
  }

  @Test
  fun `should return empty file error for empty upload`() {
    // Given - an empty file
    val file = MockMultipartFile(
      "file",
      "empty.csv",
      "text/csv",
      ByteArray(0)
    )

    // When & Then
    mockMvc.perform(
      multipart("/admin/lma/import")
        .file(file)
        .with(jwt()
          .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
          .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
    )
      .andExpect(status().isBadRequest)
      .andExpect(jsonPath("$.success").value(false))
      .andExpect(jsonPath("$.message").value("Bestand is leeg"))
  }

  @Test
  fun `should get unresolved import errors`() {
    // Given - import a CSV that will generate errors
    val csvContent = """
      Afvalstroomnummer,Verwerkersnummer Locatie van Bestemming (LvB),LvB KvK nummer,LvB Bedrijfsnaam,LvB Adres,LvB Postcode/Plaats,Handelsregisternummer Ontdoener,Naam Ontdoener,Land Ontdoener,LocatieHerkomst Straatnaam,LocatieHerkomst Huisnummer,LocatieHerkomst HuisnummerToevoeging,LocatieHerkomst Postcode,LocatieHerkomst Plaats,LocatieHerkomst Nabijheidsbeschrijving,LocatieHerkomst Land,Euralcode,Euralcode Omschrijving,Gebruikelijke Naam Afvalstof,VerwerkingsMethode Code,VerwerkingsMethode Omschrijving,Routeinzameling,Inzamelaarsregeling,Particuliere Ontdoener
      198080000099,19808,78083180,WHD B.V.,verwerkerstraat 4,3702IB 'S-GRAVENZANDE,99999999,Unknown Company,Nederland,Teststraat,4,,3776KK,PIJNACKER,,Nederland,170405,ijzer en staal,ijzer en staal,A02,Overslag / opbulken,N,N,N
    """.trimIndent()

    val file = MockMultipartFile(
      "file",
      "lma-export.csv",
      "text/csv",
      csvContent.toByteArray()
    )

    mockMvc.perform(
      multipart("/admin/lma/import")
        .file(file)
        .with(jwt()
          .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
          .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
    ).andExpect(status().isOk)

    // When & Then - get errors
    mockMvc.perform(
      get("/admin/lma/import/errors")
        .with(jwt()
          .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
          .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.errors").isArray)
      .andExpect(jsonPath("$.errors.length()").value(1))
      .andExpect(jsonPath("$.errors[0].errorCode").value("COMPANY_NOT_FOUND"))
      .andExpect(jsonPath("$.errors[0].wasteStreamNumber").value("198080000099"))
  }
}
