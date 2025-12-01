package nl.eazysoftware.eazyrecyclingservice.test.util

import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

/**
 * A wrapper around MockMvc that adds authentication with admin role to all requests.
 * This simplifies testing of secured endpoints in integration tests.
 */
class SecuredMockMvc(private val mockMvc: MockMvc) {

    companion object {
        private val TEST_USER_METADATA = mapOf(
            "first_name" to "Test",
            "last_name" to "User"
        )
    }

    /**
     * Performs a GET request with admin role authentication.
     *
     * @param url The URL to perform the GET request on
     * @return The result of the request
     */
    fun get(url: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .with(jwt()
                    .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
                    .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
    }

    /**
     * Performs a POST request with admin role authentication.
     *
     * @param url The URL to perform the POST request on
     * @param content The content to send in the request body
     * @return The result of the request
     */
    fun post(url: String, content: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .with(jwt()
                    .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
                    .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
    }

    /**
     * Performs a POST request with custom subject and roles.
     *
     * @param url The URL to perform the POST request on
     * @param content The content to send in the request body
     * @param subject The subject claim to include in the JWT
     * @param roles The roles to include in the JWT
     * @return The result of the request
     */
    fun postWithSubject(url: String, content: String, subject: String, roles: List<String> = listOf(Roles.ADMIN)): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .with(jwt()
                    .jwt { it.claim("sub", subject) }
                    .authorities(roles.map { SimpleGrantedAuthority(it) })
                )
        )
    }

    /**
     * Performs a PUT request with admin role authentication.
     *
     * @param url The URL to perform the PUT request on
     * @param content The content to send in the request body
     * @return The result of the request
     */
    fun put(url: String, content: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .with(jwt()
                    .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
                    .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
    }

    /**
     * Performs a DELETE request with admin role authentication.
     *
     * @param url The URL to perform the DELETE request on
     * @return The result of the request
     */
    fun delete(url: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.delete(url)
                .with(jwt()
                    .jwt { it.claim("user_metadata", TEST_USER_METADATA) }
                    .authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
    }
}
