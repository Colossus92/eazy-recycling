package nl.eazysoftware.eazyrecyclingservice.controller.amice

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.LmaDeclaration
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationDto
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.LmaDeclarationJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AmiceControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var lmaDeclarationJpaRepository: LmaDeclarationJpaRepository

  @BeforeEach
  fun setUp() {
    securedMockMvc = SecuredMockMvc(mockMvc)
  }

  @AfterEach
  fun tearDown() {
    lmaDeclarationJpaRepository.deleteAll()
  }

  @Test
  fun `should return true when there are pending approvals`() {
    // Given: Create an LMA declaration with WAITING_APPROVAL status
    val declaration = LmaDeclarationDto(
      id = "test-declaration-1",
      wasteStreamNumber = "TEST-001",
      period = "122024",
      transporters = listOf("Test Transporter"),
      totalWeight = 1000L,
      totalShipments = 5L,
      type = LmaDeclaration.Type.FIRST_RECEIVAL,
      createdAt = Instant.now(),
      status = LmaDeclarationDto.Status.WAITING_APPROVAL
    )
    lmaDeclarationJpaRepository.save(declaration)

    // When: Call the pending-approvals endpoint
    val result = securedMockMvc.get("/amice/pending-approvals")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.hasPendingApprovals").value(true))
      .andReturn()

    // Then: Verify the response
    val response = objectMapper.readValue(result.response.contentAsString, AmiceController.PendingApprovalsResponse::class.java)
    assertThat(response.hasPendingApprovals).isTrue()
  }

  @Test
  fun `should return false when there are no pending approvals`() {
    // Given: Create LMA declarations with non-WAITING_APPROVAL statuses
    val completedDeclaration = LmaDeclarationDto(
      id = "test-declaration-1",
      wasteStreamNumber = "TEST-001",
      period = "122024",
      transporters = listOf("Test Transporter"),
      totalWeight = 1000L,
      totalShipments = 5L,
      type = LmaDeclaration.Type.FIRST_RECEIVAL,
      createdAt = Instant.now(),
      status = LmaDeclarationDto.Status.COMPLETED
    )

    val pendingDeclaration = LmaDeclarationDto(
      id = "test-declaration-2",
      wasteStreamNumber = "TEST-002",
      period = "122024",
      transporters = listOf("Test Transporter 2"),
      totalWeight = 2000L,
      totalShipments = 3L,
      type = LmaDeclaration.Type.MONTHLY_RECEIVAL,
      createdAt = Instant.now(),
      status = LmaDeclarationDto.Status.PENDING
    )

    lmaDeclarationJpaRepository.saveAll(listOf(completedDeclaration, pendingDeclaration))

    // When: Call the pending-approvals endpoint
    val result = securedMockMvc.get("/amice/pending-approvals")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.hasPendingApprovals").value(false))
      .andReturn()

    // Then: Verify the response
    val response = objectMapper.readValue(result.response.contentAsString, AmiceController.PendingApprovalsResponse::class.java)
    assertThat(response.hasPendingApprovals).isFalse()
  }

  @Test
  fun `should return false when there are no declarations at all`() {
    // Given: No declarations in the database

    // When: Call the pending-approvals endpoint
    val result = securedMockMvc.get("/amice/pending-approvals")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.hasPendingApprovals").value(false))
      .andReturn()

    // Then: Verify the response
    val response = objectMapper.readValue(result.response.contentAsString, AmiceController.PendingApprovalsResponse::class.java)
    assertThat(response.hasPendingApprovals).isFalse()
  }

  @Test
  fun `should return true when multiple declarations with mixed statuses include waiting approval`() {
    // Given: Create multiple LMA declarations with different statuses, including WAITING_APPROVAL
    val declarations = listOf(
      LmaDeclarationDto(
        id = "test-declaration-1",
        wasteStreamNumber = "TEST-001",
        period = "122024",
        transporters = listOf("Test Transporter 1"),
        totalWeight = 1000L,
        totalShipments = 5L,
        type = LmaDeclaration.Type.FIRST_RECEIVAL,
        createdAt = Instant.now(),
        status = LmaDeclarationDto.Status.COMPLETED
      ),
      LmaDeclarationDto(
        id = "test-declaration-2",
        wasteStreamNumber = "TEST-002",
        period = "122024",
        transporters = listOf("Test Transporter 2"),
        totalWeight = 2000L,
        totalShipments = 3L,
        type = LmaDeclaration.Type.MONTHLY_RECEIVAL,
        createdAt = Instant.now(),
        status = LmaDeclarationDto.Status.WAITING_APPROVAL // This one should make the result true
      ),
      LmaDeclarationDto(
        id = "test-declaration-3",
        wasteStreamNumber = "TEST-003",
        period = "122024",
        transporters = listOf("Test Transporter 3"),
        totalWeight = 1500L,
        totalShipments = 2L,
        type = LmaDeclaration.Type.FIRST_RECEIVAL,
        createdAt = Instant.now(),
        status = LmaDeclarationDto.Status.PENDING
      )
    )

    lmaDeclarationJpaRepository.saveAll(declarations)

    // When: Call the pending-approvals endpoint
    val result = securedMockMvc.get("/amice/pending-approvals")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.hasPendingApprovals").value(true))
      .andReturn()

    // Then: Verify the response
    val response = objectMapper.readValue(result.response.contentAsString, AmiceController.PendingApprovalsResponse::class.java)
    assertThat(response.hasPendingApprovals).isTrue()
  }
}
