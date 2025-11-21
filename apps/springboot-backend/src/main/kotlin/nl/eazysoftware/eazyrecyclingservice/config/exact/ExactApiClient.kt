package nl.eazysoftware.eazyrecyclingservice.config.exact

import nl.eazysoftware.eazyrecyclingservice.domain.service.ExactOAuthService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.io.IOException

/**
 * REST client for making authenticated requests to Exact Online API.
 * 
 * This client automatically:
 * - Adds the OAuth2 access token to all requests
 * - Refreshes the token on 401 Unauthorized responses
 * - Retries the request after token refresh
 */
@Component
class ExactApiClient(
    private val exactOAuthService: ExactOAuthService
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Get a RestTemplate configured with Exact Online authentication
     */
    fun getRestTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(ExactAuthInterceptor(exactOAuthService))
        return restTemplate
    }
    
    /**
     * Interceptor that adds OAuth2 bearer token and handles token refresh on 401
     */
    private class ExactAuthInterceptor(
        private val exactOAuthService: ExactOAuthService
    ) : ClientHttpRequestInterceptor {
        
        private val logger = LoggerFactory.getLogger(javaClass)
        
        @Throws(IOException::class)
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            
            // Add bearer token to request
            val accessToken = exactOAuthService.getValidAccessToken()
            request.headers.setBearerAuth(accessToken)
            
            // Execute request
            var response = execution.execute(request, body)
            
            // If we get 401, try to refresh token and retry once
            if (response.statusCode == HttpStatus.UNAUTHORIZED) {
                logger.warn("Received 401 from Exact Online API, refreshing token and retrying...")
                
                try {
                    // Refresh the token
                    exactOAuthService.refreshAccessToken()
                    
                    // Get the new token and update the request header
                    val newAccessToken = exactOAuthService.getValidAccessToken()
                    request.headers.setBearerAuth(newAccessToken)
                    
                    // Retry the request
                    response = execution.execute(request, body)
                    logger.info("Successfully retried request after token refresh")
                    
                } catch (e: Exception) {
                    logger.error("Failed to refresh token and retry request", e)
                    throw HttpClientErrorException(
                        HttpStatus.UNAUTHORIZED,
                        "Failed to authenticate with Exact Online after token refresh"
                    )
                }
            }
            
            return response
        }
    }
}
