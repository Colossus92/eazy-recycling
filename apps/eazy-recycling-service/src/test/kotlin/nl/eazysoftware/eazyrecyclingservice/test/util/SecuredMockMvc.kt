package nl.eazysoftware.eazyrecyclingservice.test.util

import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

/**
 * A wrapper around MockMvc that adds authentication with admin role to all requests.
 * This simplifies testing of secured endpoints in integration tests.
 */
class SecuredMockMvc(private val mockMvc: MockMvc) {

    /**
     * Performs a GET request with admin role authentication.
     *
     * @param url The URL to perform the GET request on
     * @return The result of the request
     */
    fun get(url: String): ResultActions {
        return mockMvc.perform(
            MockMvcRequestBuilders.get(url)
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
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
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
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
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
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
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
    }
}
