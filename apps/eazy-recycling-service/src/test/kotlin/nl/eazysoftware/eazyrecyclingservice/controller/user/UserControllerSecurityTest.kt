package nl.eazysoftware.eazyrecyclingservice.controller.user

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.jan.supabase.auth.user.UserInfo
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var userRepository: UserRepository

    private val testUserId = "test-user-id"

    @BeforeEach
    fun setup() {
        `when`(userRepository.getById(testUserId)).thenReturn(
            UserInfo(
                id = testUserId,
                email = "test-user-email",
                aud = "authenticated",
            )
        )
    }

    companion object {
        @JvmStatic
        fun roleAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // GET all users - any role can access
                Arguments.of("/users", "GET", Roles.ADMIN, null, 200),
                Arguments.of("/users", "GET", Roles.PLANNER, null, 200),
                Arguments.of("/users", "GET", Roles.CHAUFFEUR, null, 200),
                Arguments.of("/users", "GET", "unauthorized_role", null, 403),

                // GET user by id - any role can access
                Arguments.of("/users/test-user-id", "GET", Roles.ADMIN, null, 200),
                Arguments.of("/users/test-user-id", "GET", Roles.PLANNER, null, 200),
                Arguments.of("/users/test-user-id", "GET", Roles.CHAUFFEUR, null, 200),
                Arguments.of("/users/test-user-id", "GET", "unauthorized_role", null, 403),

                // PUT (update) user - only admin can access
                Arguments.of("/users/test-user-id", "PUT", Roles.ADMIN, null, 200),
                Arguments.of("/users/test-user-id", "PUT", Roles.PLANNER, null, 403),
                Arguments.of("/users/test-user-id", "PUT", Roles.CHAUFFEUR, null, 403),
                Arguments.of("/users/test-user-id", "PUT", "unauthorized_role", null, 403),

                // POST (create) user - only admin can access
                Arguments.of("/users", "POST", Roles.ADMIN, null, 200),
                Arguments.of("/users", "POST", Roles.PLANNER, null, 403),
                Arguments.of("/users", "POST", Roles.CHAUFFEUR, null, 403),
                Arguments.of("/users", "POST", "unauthorized_role", null, 403),

                // DELETE user - only admin can access
                Arguments.of("/users/test-user-id", "DELETE", Roles.ADMIN, null, 200),
                Arguments.of("/users/test-user-id", "DELETE", Roles.PLANNER, null, 403),
                Arguments.of("/users/test-user-id", "DELETE", Roles.CHAUFFEUR, null, 403),
                Arguments.of("/users/test-user-id", "DELETE", "unauthorized_role", null, 403)
            )
        }

        @JvmStatic
        fun updateUserScenarios(): Stream<Arguments> {
            return Stream.of(
                // Every role can update their own profile (sub claim matches id)
                Arguments.of(Roles.ADMIN, "test-user-id", "test-user-id", 200),
                Arguments.of(Roles.PLANNER, "test-user-id", "test-user-id", 200),
                Arguments.of(Roles.CHAUFFEUR, "test-user-id", "test-user-id", 200),

                // No role can update other profiles (sub claim doesn't match id)
                Arguments.of(Roles.PLANNER, "test-user-id", "other-user-id", 403),
                Arguments.of(Roles.PLANNER, "test-user-id", "other-user-id", 403),
                Arguments.of(Roles.CHAUFFEUR, "test-user-id", "other-user-id", 403),

                // Unauthorized role cannot update any profile
                Arguments.of("unauthorized_role", "test-user-id", "test-user-id", 403),
                Arguments.of("unauthorized_role", "test-user-id", "other-user-id", 403)
            )
        }
    }

    @ParameterizedTest(name = "{1} {0} with role {2} should return {4}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for user endpoints`(
        endpoint: String,
        method: String,
        role: String,
        subClaim: String?,
        expectedStatus: Int
    ) {
        val request = when (method) {
            "GET" -> get(endpoint)
            "PUT" -> put(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@example.com","password":"password","firstName":"Test","lastName":"User","roles":["planner"]}""")
            "POST" -> post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"test@example.com","password":"password","firstName":"Test","lastName":"User","roles":["planner"]}""")
            "DELETE" -> delete(endpoint)
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }

        val jwtPostProcessor = if (subClaim != null) {
            jwt().authorities(SimpleGrantedAuthority(role)).jwt { jwt -> jwt.claim("sub", subClaim) }
        } else {
            jwt().authorities(SimpleGrantedAuthority(role))
        }

        mockMvc.perform(
            request.with(jwtPostProcessor)
        ).andExpect(status().`is`(expectedStatus))
    }

    @ParameterizedTest(name = "User with role {0} updating user {1} with sub claim {2} should return {3}")
    @MethodSource("updateUserScenarios")
    fun `should verify update user security logic`(
        role: String,
        userId: String,
        subClaim: String,
        expectedStatus: Int
    ) {
        val updateRequest = UpdateUserRequest(
            email = "updated@example.com",
            firstName = "Updated",
            lastName = "User",
            roles = arrayOf("user")
        )

        mockMvc.perform(
            put("/users/$userId/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(jwt()
                    .authorities(SimpleGrantedAuthority(role))
                    .jwt { jwt -> jwt.claim("sub", subClaim) }
                )
        ).andExpect(status().`is`(expectedStatus))
    }
}
