package nl.eazysoftware.eazyrecyclingservice.controller.amice

import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.DeclareFirstReceivals
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.AmiceSessions
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.MonthlyWasteDeclarationJobsJpaRepository
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.time.Clock

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AmiceControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var monthlyWasteDeclarationJobsRepository: MonthlyWasteDeclarationJobsJpaRepository

    @MockitoBean
    private lateinit var declareFirstReceivals: DeclareFirstReceivals

    @MockitoBean
    private lateinit var amiceSessions: AmiceSessions

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
    }

    @AfterEach
    fun cleanup() {
        monthlyWasteDeclarationJobsRepository.deleteAll()
    }

    @Test
    fun `should create pending jobs when declare endpoint is called`() {
        // Given - no existing jobs
        val jobsBeforeCall = monthlyWasteDeclarationJobsRepository.findAll()
        assertThat(jobsBeforeCall).isEmpty()

        val currentYearMonth = Clock.System.now().toYearMonth()

        // When - calling the declare endpoint
        securedMockMvc.post("/amice", "")
            .andExpect(status().isOk)

        // Then - verify two jobs are created with PENDING status
        val jobsAfterCall = monthlyWasteDeclarationJobsRepository.findAll()
        assertThat(jobsAfterCall).hasSize(2)

        // Verify FIRST_RECEIVALS job
        val firstReceivalsJob = jobsAfterCall.find {
            it.jobType == MonthlyWasteDeclarationJob.JobType.FIRST_RECEIVALS
        }
        assertThat(firstReceivalsJob).isNotNull
        assertThat(firstReceivalsJob?.status).isEqualTo(MonthlyWasteDeclarationJob.Status.PENDING)
        assertThat(firstReceivalsJob?.yearMonth).isEqualTo(currentYearMonth)
        assertThat(firstReceivalsJob?.fulfilledAt).isNull()

        // Verify MONTHLY_RECEIVALS job
        val monthlyReceivalsJob = jobsAfterCall.find {
            it.jobType == MonthlyWasteDeclarationJob.JobType.MONTHLY_RECEIVALS
        }
        assertThat(monthlyReceivalsJob).isNotNull
        assertThat(monthlyReceivalsJob?.status).isEqualTo(MonthlyWasteDeclarationJob.Status.PENDING)
        assertThat(monthlyReceivalsJob?.yearMonth).isEqualTo(currentYearMonth)
        assertThat(monthlyReceivalsJob?.fulfilledAt).isNull()
    }
}
