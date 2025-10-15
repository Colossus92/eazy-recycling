package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class ProcessingMethodControllerIntegrationTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
  }

  @Test
  fun `GET processing-methods should return all processing methods with 200 OK`() {
    // When & Then
    securedMockMvc.get("/processing-methods")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.length()").value(31))
      .andExpect(jsonPath("$[0].code").value("A.01"))
      .andExpect(jsonPath("$[0].description").value("Bewaren"))
  }

  @Test
  fun `GET processing-methods should return 401 Unauthorized when no authentication is provided`() {
    // When & Then - using raw mockMvc without authentication
    mockMvc.perform(
      MockMvcRequestBuilders.get("/processing-methods")
        .contentType(MediaType.APPLICATION_JSON)
    )
      .andExpect(status().isUnauthorized)
  }
}
